const express = require('express');
const router = express.Router();
const db = require('../db');

/**
 * POST /api/magazzino/trasferisci
 * Sposta un certo numero di pezzi dal magazzino al catalogo su uno scaffale.
 * body: { idProdotto: string, quantita: number, idScaffale: number }
 */
router.post('/trasferisci', async (req, res) => {
    const client = await db.pool.connect();
    try {
        const { idProdotto, quantita, idScaffale } = req.body || {};
        const qty = Number(quantita);
        const shelfId = Number(idScaffale);

        if (!idProdotto || Number.isNaN(shelfId) || shelfId <= 0 || Number.isNaN(qty) || qty <= 0) {
            return res
                .status(400)
                .json({ error: 'idProdotto, idScaffale e quantita (>0) sono obbligatori' });
        }

        await client.query('BEGIN');

        const shelfCheck = await client.query(
            `SELECT id_scaffale FROM scaffali WHERE id_scaffale = $1`,
            [shelfId]
        );
        if (shelfCheck.rowCount === 0) {
            await client.query('ROLLBACK');
            return res.status(404).json({ error: 'Scaffale non trovato' });
        }

        let warehouseRes = await client.query(
            `SELECT id_magazzino, quantita_disponibile
             FROM magazzino
             WHERE id_prodotto = $1
             FOR UPDATE`,
            [idProdotto]
        );

        let available = warehouseRes.rowCount > 0 ? Number(warehouseRes.rows[0].quantita_disponibile) : 0;

        const arrivedRes = await client.query(
            `SELECT COALESCE(SUM(quantita_ordinata), 0) AS qty
             FROM riordini_magazzino
             WHERE id_prodotto = $1 AND arrivato = true`,
            [idProdotto]
        );
        const arrivedQty = Number(arrivedRes.rows[0].qty);

        // Se non c'e' riga magazzino ma esistono arrivi, crea la riga
        if (warehouseRes.rowCount === 0 && arrivedQty > 0) {
            const inserted = await client.query(
                `INSERT INTO magazzino (id_prodotto, quantita_disponibile, id_ultimo_riordino_arrivato)
                 VALUES ($1, $2, NULL)
                 RETURNING id_magazzino, quantita_disponibile`,
                [idProdotto, arrivedQty]
            );
            warehouseRes = { rows: inserted.rows, rowCount: 1 };
            available = Number(inserted.rows[0].quantita_disponibile);
        }

        if (warehouseRes.rowCount === 0 && arrivedQty <= 0) {
            await client.query('ROLLBACK');
            return res.status(404).json({ error: 'Prodotto non presente in magazzino' });
        }

        // Se esiste riga magazzino ma quantita' 0 e abbiamo arrivi, sincronizza con gli arrivi
        if (available <= 0 && arrivedQty > 0) {
            await client.query(
                `UPDATE magazzino
                 SET quantita_disponibile = $2
                 WHERE id_magazzino = $1`,
                [warehouseRes.rows[0].id_magazzino, arrivedQty]
            );
            available = arrivedQty;
        }

        if (available < qty) {
            await client.query('ROLLBACK');
            return res.status(409).json({
                error: 'Quantita magazzino insufficiente',
                disponibile: available
            });
        }

        await client.query(
            `UPDATE magazzino
             SET quantita_disponibile = quantita_disponibile - $2
             WHERE id_prodotto = $1`,
            [idProdotto, qty]
        );

        const catalogRes = await client.query(
            `SELECT id_catalogo, quantita_disponibile, prezzo, vecchio_prezzo
             FROM catalogo
             WHERE id_prodotto = $1 AND id_scaffale = $2
             FOR UPDATE`,
            [idProdotto, shelfId]
        );

        let catalogRow;
        if (catalogRes.rowCount > 0) {
            const updated = await client.query(
                `UPDATE catalogo
                 SET quantita_disponibile = quantita_disponibile + $2
                 WHERE id_catalogo = $1
                 RETURNING *`,
                [catalogRes.rows[0].id_catalogo, qty]
            );
            catalogRow = updated.rows[0];
        } else {
            const fallback = await client.query(
                `SELECT prezzo, vecchio_prezzo FROM catalogo WHERE id_prodotto = $1 LIMIT 1`,
                [idProdotto]
            );
            const price = Number(fallback.rows[0]?.prezzo ?? 0);
            const oldPrice = fallback.rows[0]?.vecchio_prezzo ?? null;

            const inserted = await client.query(
                `INSERT INTO catalogo (id_prodotto, quantita_disponibile, prezzo, vecchio_prezzo, id_scaffale)
                 VALUES ($1, $2, $3, $4, $5)
                 RETURNING *`,
                [idProdotto, qty, price, oldPrice, shelfId]
            );
            catalogRow = inserted.rows[0];
        }

        await client.query('COMMIT');

            return res.json({
                message: 'Trasferimento completato',
                idProdotto,
                idScaffale: shelfId,
                quantitaTrasferita: qty,
                magazzinoResiduo: available - qty,
                catalogo: {
                idCatalogo: catalogRow.id_catalogo,
                quantitaDisponibile: Number(catalogRow.quantita_disponibile),
                prezzo: Number(catalogRow.prezzo),
                vecchioPrezzo: catalogRow.vecchio_prezzo
            }
        });
    } catch (error) {
        await client.query('ROLLBACK');
        console.error('Errore trasferimento scorte:', error);
        res.status(500).json({ error: 'Errore durante il trasferimento dal magazzino' });
    } finally {
        client.release();
    }
});

/**
 * POST /api/magazzino/riconcilia-arrivi
 * Sincronizza la tabella magazzino con i riordini arrivati (arrivato = true).
 * Utile per riallineare dati importati da dump dove magazzino non � stato aggiornato.
 */
router.post('/riconcilia-arrivi', async (_req, res) => {
    const client = await db.pool.connect();
    try {
        await client.query('BEGIN');

        const result = await client.query(
            `
            WITH arrivi AS (
                SELECT id_prodotto, SUM(quantita_ordinata) AS qty,
                       MAX(id_riordino) FILTER (WHERE arrivato = true) AS last_arrivo
                FROM riordini_magazzino
                WHERE arrivato = true
                GROUP BY id_prodotto
            ),
            updated AS (
                UPDATE magazzino m
                SET quantita_disponibile = a.qty,
                    id_ultimo_riordino_arrivato = a.last_arrivo
                FROM arrivi a
                WHERE m.id_prodotto = a.id_prodotto
                  AND m.id_ultimo_riordino_arrivato IS NULL
                RETURNING m.id_prodotto
            ),
            inserted AS (
                INSERT INTO magazzino (id_prodotto, quantita_disponibile, id_ultimo_riordino_arrivato)
                SELECT a.id_prodotto, a.qty, a.last_arrivo
                FROM arrivi a
                WHERE NOT EXISTS (
                    SELECT 1 FROM magazzino m WHERE m.id_prodotto = a.id_prodotto
                )
                RETURNING id_prodotto
            )
            SELECT id_prodotto FROM updated
            UNION ALL
            SELECT id_prodotto FROM inserted
            `
        );

        await client.query('COMMIT');
        res.json({
            message: 'Magazzino sincronizzato con riordini arrivati',
            updatedOrInserted: result.rowCount
        });
    } catch (error) {
        await client.query('ROLLBACK');
        console.error('Errore riconciliazione magazzino:', error);
        res.status(500).json({ error: 'Errore durante la riconciliazione magazzino' });
    } finally {
        client.release();
    }
});

module.exports = router;
