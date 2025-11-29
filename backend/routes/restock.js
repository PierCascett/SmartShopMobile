const express = require('express');
const router = express.Router();
const db = require('../db');

const AUTO_ARRIVAL_DELAY_MS = 30_000;

async function markRestockArrival(client, restockId, arrivedAt = null) {
    const resRiordino = await client.query(
        `SELECT id_prodotto, quantita_ordinata, arrivato
         FROM riordini_magazzino
         WHERE id_riordino = $1
         FOR UPDATE`,
        [restockId]
    );

    if (resRiordino.rowCount === 0) {
        return null;
    }
    const { id_prodotto, quantita_ordinata, arrivato } = resRiordino.rows[0];
    if (arrivato) {
        return null;
    }

    await client.query(
        `UPDATE riordini_magazzino
         SET arrivato = true, data_arrivo_effettiva = COALESCE($2, CURRENT_TIMESTAMP)
         WHERE id_riordino = $1`,
        [restockId, arrivedAt || null]
    );

    const updateRes = await client.query(
        `UPDATE magazzino
         SET quantita_disponibile = quantita_disponibile + $2,
             id_ultimo_riordino_arrivato = $1
         WHERE id_prodotto = $3`,
        [restockId, quantita_ordinata, id_prodotto]
    );

    if (updateRes.rowCount === 0) {
        await client.query(
            `INSERT INTO magazzino (id_prodotto, quantita_disponibile, id_ultimo_riordino_arrivato)
             VALUES ($1, $2, $3)`,
            [id_prodotto, quantita_ordinata, restockId]
        );
    }

    return { restockId, productId: id_prodotto, quantity: quantita_ordinata };
}

async function processPendingArrivals() {
    const client = await db.pool.connect();
    const processed = [];
    try {
        await client.query('BEGIN');
        const pending = await client.query(
            `SELECT id_riordino
             FROM riordini_magazzino
             WHERE arrivato = false
               AND data_arrivo_prevista IS NOT NULL
               AND data_arrivo_prevista <= NOW()
               AND data_ordine <= NOW() - ($1::text || ' milliseconds')::interval
             FOR UPDATE`,
            [AUTO_ARRIVAL_DELAY_MS]
        );

        for (const row of pending.rows) {
            const result = await markRestockArrival(client, row.id_riordino, new Date());
            if (result) {
                processed.push(result);
            }
        }

        await client.query('COMMIT');
    } catch (error) {
        await client.query('ROLLBACK');
        throw error;
    } finally {
        client.release();
    }
    return processed;
}

async function processRestockById(restockId) {
    const client = await db.pool.connect();
    try {
        await client.query('BEGIN');
        const result = await markRestockArrival(client, restockId, new Date());
        await client.query('COMMIT');
        return result;
    } catch (error) {
        await client.query('ROLLBACK');
        throw error;
    } finally {
        client.release();
    }
}

function scheduleAutoArrival(restockId) {
    setTimeout(() => {
        processRestockById(restockId).catch((err) =>
            console.error('Errore auto-arrivo riordino', restockId, err.message || err)
        );
    }, AUTO_ARRIVAL_DELAY_MS);
}

/**
 * GET /api/riordini
 * Elenco riordini magazzino con info prodotto/fornitore/responsabile
 */
router.get('/', async (_req, res) => {
    try {
        await processPendingArrivals();
        const result = await db.query(
            `SELECT 
                r.id_riordino,
                r.id_prodotto,
                p.nome AS prodotto_nome,
                r.id_fornitore,
                f.nome AS fornitore_nome,
                r.quantita_ordinata,
                r.data_ordine,
                r.data_arrivo_prevista,
                r.data_arrivo_effettiva,
                r.arrivato,
                r.id_responsabile,
                u.nome AS responsabile_nome,
                u.cognome AS responsabile_cognome
             FROM riordini_magazzino r
             JOIN prodotti p ON p.id_prodotto = r.id_prodotto
             JOIN fornitori f ON f.id_fornitore = r.id_fornitore
             LEFT JOIN utenti u ON u.id_utente = r.id_responsabile
             ORDER BY r.data_ordine DESC`
        );
        res.json(result.rows);
    } catch (error) {
        console.error('Errore recupero riordini:', error);
        res.status(500).json({ error: 'Errore nel recupero dei riordini' });
    }
});

/**
 * POST /api/riordini/processa-arrivi
 * Forza l'elaborazione dei riordini con data_arrivo_prevista <= NOW()
 */
router.post('/processa-arrivi', async (_req, res) => {
    try {
        const processed = await processPendingArrivals();
        res.json({ processed: processed.length });
    } catch (error) {
        console.error('Errore auto-arrivi riordini:', error);
        res.status(500).json({ error: 'Errore durante l\'elaborazione degli arrivi' });
    }
});

/**
 * POST /api/riordini
 * Crea un nuovo riordino
 */
router.post('/', async (req, res) => {
    try {
        const {
            idProdotto,
            idFornitore,
            quantitaOrdinata,
            dataArrivoPrevista,
            idResponsabile
        } = req.body;
        const etaIso = dataArrivoPrevista || new Date(Date.now() + AUTO_ARRIVAL_DELAY_MS).toISOString();

        if (!idProdotto || !idFornitore || !quantitaOrdinata) {
            return res.status(400).json({ error: 'idProdotto, idFornitore e quantitaOrdinata sono obbligatori' });
        }

        const insert = await db.query(
            `INSERT INTO riordini_magazzino
                (id_prodotto, id_fornitore, quantita_ordinata, data_ordine, data_arrivo_prevista, arrivato, id_responsabile)
             VALUES ($1, $2, $3, NOW(), $4, false, $5)
             RETURNING id_riordino`,
            [idProdotto, idFornitore, quantitaOrdinata, etaIso, idResponsabile || null]
        );
        const newRestockId = insert.rows[0].id_riordino;

        const detail = await db.query(
            `SELECT 
                r.id_riordino,
                r.id_prodotto,
                p.nome AS prodotto_nome,
                r.id_fornitore,
                f.nome AS fornitore_nome,
                r.quantita_ordinata,
                r.data_ordine,
                r.data_arrivo_prevista,
                r.data_arrivo_effettiva,
                r.arrivato,
                r.id_responsabile,
                u.nome AS responsabile_nome,
                u.cognome AS responsabile_cognome
             FROM riordini_magazzino r
             JOIN prodotti p ON p.id_prodotto = r.id_prodotto
             JOIN fornitori f ON f.id_fornitore = r.id_fornitore
             LEFT JOIN utenti u ON u.id_utente = r.id_responsabile
             WHERE r.id_riordino = $1`,
            [newRestockId]
        );

        scheduleAutoArrival(newRestockId);
        res.status(201).json(detail.rows[0]);
    } catch (error) {
        console.error('Errore creazione riordino:', error);
        res.status(500).json({ error: 'Errore durante la creazione del riordino' });
    }
});

/**
 * PATCH /api/riordini/:id/arrivo
 * Marca un riordino come arrivato e aggiorna magazzino
 */
router.patch('/:id/arrivo', async (req, res) => {
    const client = await db.pool.connect();
    try {
        const { id } = req.params;
        const { arrivedAt } = req.body || {};
        await client.query('BEGIN');

        const result = await markRestockArrival(client, id, arrivedAt || null);
        if (!result) {
            await client.query('ROLLBACK');
            return res.status(404).json({ error: 'Riordino non trovato o già arrivato' });
        }
        await client.query('COMMIT');
        res.json({ ok: true });
    } catch (error) {
        await client.query('ROLLBACK');
        console.error('Errore conferma arrivo riordino:', error);
        res.status(500).json({ error: 'Errore nel chiudere il riordino' });
    } finally {
        client.release();
    }
});

module.exports = router;
