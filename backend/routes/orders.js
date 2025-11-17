const express = require('express');
const router = express.Router();
const db = require('../db');

/**
 * GET /api/ordini
 * Elenco ordini con dettaglio righe per dipendenti/responsabili
 */
router.get('/', async (_req, res) => {
    try {
        const ordersResult = await db.query(
            `SELECT 
                o.id_ordine,
                o.id_utente,
                o.data_ordine,
                o.stato,
                o.totale,
                u.nome,
                u.cognome,
                u.email
             FROM ordini o
             JOIN utenti u ON u.id_utente = o.id_utente
             ORDER BY o.data_ordine DESC`
        );

        const linesResult = await db.query(
            `SELECT 
                r.id_riga,
                r.id_ordine,
                r.id_prodotto,
                r.quantita,
                r.prezzo_unitario,
                r.prezzo_totale,
                p.nome,
                p.marca
             FROM righe_ordine r
             JOIN prodotti p ON p.id_prodotto = r.id_prodotto`
        );

        const linesByOrder = linesResult.rows.reduce((acc, row) => {
            acc[row.id_ordine] = acc[row.id_ordine] || [];
            acc[row.id_ordine].push(row);
            return acc;
        }, {});

        const orders = ordersResult.rows.map((order) => ({
            ...order,
            righe: linesByOrder[order.id_ordine] || []
        }));

        res.json(orders);
    } catch (error) {
        console.error('Errore nel recupero ordini:', error);
        res.status(500).json({ error: 'Errore nel recupero degli ordini' });
    }
});

/**
 * POST /api/ordini
 * Crea un nuovo ordine cliente e aggiorna catalogo/magazzino
 * body: { idUtente: number, items: [{ idProdotto, quantita }] }
 */
router.post('/', async (req, res) => {
    const client = await db.pool.connect();
    try {
        const { idUtente, items } = req.body;
        if (!idUtente || !Array.isArray(items) || items.length === 0) {
            return res.status(400).json({ error: 'idUtente e items sono obbligatori' });
        }

        await client.query('BEGIN');
        const orderLines = [];
        let total = 0;

        for (const item of items) {
            const { idProdotto, quantita } = item;
            if (!idProdotto || !quantita || quantita <= 0) {
                await client.query('ROLLBACK');
                return res.status(400).json({ error: 'Ogni item deve avere idProdotto e quantita > 0' });
            }

            const stockRes = await client.query(
                `SELECT quantita_disponibile, prezzo 
                 FROM catalogo 
                 WHERE id_prodotto = $1
                 FOR UPDATE`,
                [idProdotto]
            );

            if (stockRes.rowCount === 0) {
                await client.query('ROLLBACK');
                return res.status(404).json({ error: `Prodotto ${idProdotto} non trovato in catalogo` });
            }

            const { quantita_disponibile, prezzo } = stockRes.rows[0];
            if (quantita_disponibile < quantita) {
                await client.query('ROLLBACK');
                return res.status(400).json({ error: `Stock insufficiente per ${idProdotto}` });
            }

            const lineTotal = Number(prezzo) * quantita;
            total += lineTotal;
            orderLines.push({
                idProdotto,
                quantita,
                prezzoUnitario: prezzo,
                prezzoTotale: lineTotal
            });

            await client.query(
                `UPDATE catalogo
                 SET quantita_disponibile = quantita_disponibile - $2
                 WHERE id_prodotto = $1`,
                [idProdotto, quantita]
            );

            await client.query(
                `UPDATE magazzino
                 SET quantita_disponibile = GREATEST(quantita_disponibile - $2, 0)
                 WHERE id_prodotto = $1`,
                [idProdotto, quantita]
            );
        }

        const orderRes = await client.query(
            `INSERT INTO ordini (id_utente, data_ordine, stato, totale)
             VALUES ($1, NOW(), 'CREATO', $2)
             RETURNING id_ordine, data_ordine, stato, totale`,
            [idUtente, total]
        );

        const orderId = orderRes.rows[0].id_ordine;
        for (const line of orderLines) {
            await client.query(
                `INSERT INTO righe_ordine (id_ordine, id_prodotto, quantita, prezzo_unitario, prezzo_totale)
                 VALUES ($1, $2, $3, $4, $5)`,
                [orderId, line.idProdotto, line.quantita, line.prezzoUnitario, line.prezzoTotale]
            );
        }

        await client.query('COMMIT');
        res.status(201).json({ idOrdine: orderId, totale: total, righe: orderLines });
    } catch (error) {
        await client.query('ROLLBACK');
        console.error('Errore creazione ordine:', error);
        res.status(500).json({ error: 'Errore durante la creazione ordine' });
    } finally {
        client.release();
    }
});

module.exports = router;

