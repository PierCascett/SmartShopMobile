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
                o.metodo_consegna,
                o.id_locker,
                o.codice_ritiro,
                o.indirizzo_spedizione,
                u.nome,
                u.cognome,
                u.email,
                l.codice AS locker_codice,
                l.occupato AS locker_occupato,
                cd.id_rider,
                cd.data_assegnazione,
                cd.data_consegna
             FROM ordini o
             JOIN utenti u ON u.id_utente = o.id_utente
             LEFT JOIN locker l ON l.id_locker = o.id_locker
             LEFT JOIN consegna_domicilio cd ON cd.id_ordine = o.id_ordine
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

        const orders = ordersResult.rows.map((order) => {
            const locker =
                order.id_locker != null
                    ? {
                          id: order.id_locker,
                          codice: order.locker_codice,
                          occupato: order.locker_occupato
                      }
                    : null;
            const consegnaDomicilio =
                order.id_rider != null ||
                order.data_assegnazione != null ||
                order.data_consegna != null
                    ? {
                          idRider: order.id_rider,
                          dataAssegnazione: order.data_assegnazione,
                          dataConsegna: order.data_consegna
                      }
                    : null;
            return {
                ...order,
                locker,
                consegnaDomicilio,
                righe: linesByOrder[order.id_ordine] || []
            };
        });

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
        const { idUtente, items, metodoConsegna } = req.body;
        const indirizzoSpedizione = req.body?.indirizzoSpedizione;
        if (!idUtente || !Array.isArray(items) || items.length === 0) {
            return res.status(400).json({ error: 'idUtente e items sono obbligatori' });
        }

        const normalizedDelivery =
            (metodoConsegna || 'LOCKER').toString().trim().toUpperCase();
        if (!['LOCKER', 'DOMICILIO'].includes(normalizedDelivery)) {
            return res.status(400).json({ error: 'metodoConsegna deve essere LOCKER o DOMICILIO' });
        }
        if (normalizedDelivery === 'DOMICILIO' && (!indirizzoSpedizione || indirizzoSpedizione.trim().length === 0)) {
            return res.status(400).json({ error: 'indirizzoSpedizione richiesto per DOMICILIO' });
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

            // Recupera tutte le righe di catalogo per il prodotto e verifica stock aggregato
            const stockRes = await client.query(
                `SELECT id_catalogo, quantita_disponibile, prezzo 
                 FROM catalogo 
                 WHERE id_prodotto = $1
                 FOR UPDATE`,
                [idProdotto]
            );

            if (stockRes.rowCount === 0) {
                await client.query('ROLLBACK');
                return res.status(404).json({ error: `Prodotto ${idProdotto} non trovato` });
            }

            const totalAvailable = stockRes.rows.reduce((sum, row) => sum + Number(row.quantita_disponibile), 0);
            if (totalAvailable < quantita) {
                await client.query('ROLLBACK');
                return res.status(409).json({ error: `Stock insufficiente per ${idProdotto}` });
            }

            const price = Number(stockRes.rows[0].prezzo);
            total += price * quantita;
            orderLines.push({ idProdotto, quantita, prezzoUnitario: price, prezzoTotale: price * quantita });

            let remaining = quantita;
            for (const row of stockRes.rows) {
                if (remaining <= 0) break;
                const take = Math.min(Number(row.quantita_disponibile), remaining);
                if (take > 0) {
                    await client.query(
                        `UPDATE catalogo
                         SET quantita_disponibile = quantita_disponibile - $2
                         WHERE id_catalogo = $1`,
                        [row.id_catalogo, take]
                    );
                    remaining -= take;
                }
            }

        }

        const orderRes = await client.query(
            `INSERT INTO ordini (id_utente, data_ordine, stato, totale, metodo_consegna, indirizzo_spedizione)
             VALUES ($1, NOW(), 'CREATO', $2, $3, $4)
             RETURNING id_ordine, data_ordine, stato, totale, metodo_consegna, indirizzo_spedizione`,
            [idUtente, total, normalizedDelivery, indirizzoSpedizione || null]
        );

        const orderId = orderRes.rows[0].id_ordine;

        if (normalizedDelivery === 'DOMICILIO') {
            await client.query(
                `INSERT INTO consegna_domicilio (id_ordine, data_assegnazione)
                 VALUES ($1, NOW())
                 ON CONFLICT (id_ordine) DO NOTHING`,
                [orderId]
            );
        }
        for (const line of orderLines) {
            await client.query(
                `INSERT INTO righe_ordine (id_ordine, id_prodotto, quantita, prezzo_unitario, prezzo_totale)
                 VALUES ($1, $2, $3, $4, $5)`,
                [orderId, line.idProdotto, line.quantita, line.prezzoUnitario, line.prezzoTotale]
            );
        }

        await client.query('COMMIT');
        res.status(201).json({
            idOrdine: orderId,
            totale: total,
            metodoConsegna: normalizedDelivery,
            righe: orderLines
        });
    } catch (error) {
        await client.query('ROLLBACK');
        console.error('Errore creazione ordine:', error);
        res.status(500).json({ error: 'Errore durante la creazione ordine' });
    } finally {
        client.release();
    }
});

/**
 * PATCH /api/ordini/:id
 * Aggiorna lo stato di un ordine (es. IN_PREPARAZIONE, CONCLUSO) e gestisce locker/consegna domicilio.
 * body: { stato }
 */
router.patch('/:orderId', async (req, res) => {
    const client = await db.pool.connect();
    try {
        const orderId = Number(req.params.orderId);
        const { stato, idRider } = req.body || {};
        if (!orderId || !stato) {
            return res.status(400).json({ error: 'orderId e stato sono obbligatori' });
        }

        const newStatus = stato.toString().trim().toUpperCase();
        const allowedStatuses = ['CREATO', 'SPEDITO', 'CONSEGNATO', 'ANNULLATO', 'CONCLUSO'];
        if (!allowedStatuses.includes(newStatus)) {
            return res.status(400).json({ error: 'Stato non valido' });
        }

        await client.query('BEGIN');
        const currentRes = await client.query(
            `SELECT id_ordine, stato, metodo_consegna, id_locker, codice_ritiro
             FROM ordini
             WHERE id_ordine = $1
             FOR UPDATE`,
            [orderId]
        );
        if (currentRes.rowCount === 0) {
            await client.query('ROLLBACK');
            return res.status(404).json({ error: 'Ordine non trovato' });
        }
        const current = currentRes.rows[0];

        let lockerRow = null;
        let codiceRitiro = current.codice_ritiro;
        let lockerToAssign = null;

        // Gestione locker: lo assegniamo quando l'ordine e' pronto (SPEDITO) e lo liberiamo a conclusione/annullamento
        if (current.metodo_consegna === 'LOCKER') {
            if (newStatus === 'SPEDITO') {
                if (current.id_locker) {
                    const lockerRes = await client.query(
                        `SELECT * FROM locker WHERE id_locker = $1 FOR UPDATE`,
                        [current.id_locker]
                    );
                    lockerRow = lockerRes.rows[0] || null;
                }
                if (!lockerRow) {
                    const lockerRes = await client.query(
                        `SELECT * FROM locker WHERE occupato = false LIMIT 1 FOR UPDATE`
                    );
                    if (lockerRes.rowCount === 0) {
                        await client.query('ROLLBACK');
                        return res
                            .status(409)
                            .json({ error: 'Nessun locker disponibile', code: 'LOCKER_FULL' });
                    }
                    lockerRow = lockerRes.rows[0];
                }
                lockerToAssign = lockerRow;
                codiceRitiro =
                    codiceRitiro ||
                    Math.random().toString(36).slice(2, 8).toUpperCase();
                await client.query(
                    `UPDATE locker SET occupato = true WHERE id_locker = $1`,
                    [lockerRow.id_locker]
                );
            }

            if (['CONCLUSO', 'ANNULLATO'].includes(newStatus) && current.id_locker) {
                await client.query(
                    `UPDATE locker SET occupato = false WHERE id_locker = $1`,
                    [current.id_locker]
                );
            }
        }

        // Gestione consegna a domicilio
        if (current.metodo_consegna === 'DOMICILIO') {
            await client.query(
                `INSERT INTO consegna_domicilio (id_ordine, data_assegnazione)
                 VALUES ($1, NOW())
                 ON CONFLICT (id_ordine) DO NOTHING`,
                [orderId]
            );

            if (newStatus === 'SPEDITO') {
                await client.query(
                    `UPDATE consegna_domicilio
                     SET data_assegnazione = COALESCE(data_assegnazione, NOW()),
                         id_rider = COALESCE($2, id_rider)
                     WHERE id_ordine = $1`,
                    [orderId, idRider ?? null]
                );
            }

            if (['CONSEGNATO', 'CONCLUSO'].includes(newStatus)) {
                await client.query(
                    `UPDATE consegna_domicilio
                     SET data_consegna = COALESCE(data_consegna, NOW()),
                         id_rider = COALESCE($2, id_rider)
                     WHERE id_ordine = $1`,
                    [orderId, idRider ?? null]
                );
            }
        }

        const updateRes = await client.query(
            `UPDATE ordini
             SET stato = $2,
                 id_locker = COALESCE($3, id_locker),
                 codice_ritiro = COALESCE($4, codice_ritiro)
             WHERE id_ordine = $1
             RETURNING *`,
            [orderId, newStatus, lockerToAssign?.id_locker ?? null, codiceRitiro ?? null]
        );

        await client.query('COMMIT');

        return res.json({
            ...updateRes.rows[0],
            locker: lockerToAssign
                ? {
                      id: lockerToAssign.id_locker,
                      codice: lockerToAssign.codice,
                      posizione: lockerToAssign.posizione,
                      occupato: true
                  }
                : null
        });
    } catch (error) {
        await client.query('ROLLBACK');
        console.error('Errore aggiornamento ordine:', error);
        res.status(500).json({ error: 'Errore durante l\'aggiornamento dell\'ordine' });
    } finally {
        client.release();
    }
});

module.exports = router;
