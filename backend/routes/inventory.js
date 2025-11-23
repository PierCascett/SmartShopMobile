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

        const warehouseRes = await client.query(
            `SELECT id_magazzino, quantita_disponibile
             FROM magazzino
             WHERE id_prodotto = $1
             FOR UPDATE`,
            [idProdotto]
        );

        if (warehouseRes.rowCount === 0) {
            await client.query('ROLLBACK');
            return res.status(404).json({ error: 'Prodotto non presente in magazzino' });
        }

        const available = Number(warehouseRes.rows[0].quantita_disponibile);
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

module.exports = router;
