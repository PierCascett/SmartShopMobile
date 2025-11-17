const express = require('express');
const router = express.Router();
const db = require('../db');

/**
 * GET /api/riordini
 * Elenco riordini magazzino con info prodotto/fornitore/responsabile
 */
router.get('/', async (_req, res) => {
    try {
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

        if (!idProdotto || !idFornitore || !quantitaOrdinata) {
            return res.status(400).json({ error: 'idProdotto, idFornitore e quantitaOrdinata sono obbligatori' });
        }

        const insert = await db.query(
            `INSERT INTO riordini_magazzino
                (id_prodotto, id_fornitore, quantita_ordinata, data_ordine, data_arrivo_prevista, arrivato, id_responsabile)
             VALUES ($1, $2, $3, NOW(), $4, false, $5)
             RETURNING id_riordino`,
            [idProdotto, idFornitore, quantitaOrdinata, dataArrivoPrevista || null, idResponsabile || null]
        );

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
            [insert.rows[0].id_riordino]
        );

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
        await client.query('BEGIN');

        const resRiordino = await client.query(
            `SELECT id_prodotto, quantita_ordinata
             FROM riordini_magazzino
             WHERE id_riordino = $1
             FOR UPDATE`,
            [id]
        );

        if (resRiordino.rowCount === 0) {
            await client.query('ROLLBACK');
            return res.status(404).json({ error: 'Riordino non trovato' });
        }

        const { id_prodotto, quantita_ordinata } = resRiordino.rows[0];

        await client.query(
            `UPDATE riordini_magazzino
             SET arrivato = true, data_arrivo_effettiva = CURRENT_DATE
             WHERE id_riordino = $1`,
            [id]
        );

        const updateRes = await client.query(
            `UPDATE magazzino
             SET quantita_disponibile = quantita_disponibile + $2,
                 id_ultimo_riordino_arrivato = $3
             WHERE id_prodotto = $1`,
            [id_prodotto, quantita_ordinata, id]
        );

        if (updateRes.rowCount === 0) {
            await client.query(
                `INSERT INTO magazzino (id_prodotto, quantita_disponibile, id_ultimo_riordino_arrivato)
                 VALUES ($1, $2, $3)`,
                [id_prodotto, quantita_ordinata, id]
            );
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
