const express = require('express');
const router = express.Router();
const db = require('../db');

/**
 * GET /api/fornitori
 * Restituisce l'elenco dei fornitori per consentire al responsabile di selezionarli con nome leggibile.
 */
router.get('/', async (_req, res) => {
    try {
        const result = await db.query(
            `SELECT id_fornitore, nome, telefono, email, indirizzo
             FROM fornitori
             ORDER BY nome`
        );
        res.json(result.rows);
    } catch (error) {
        console.error('Errore nel recupero fornitori:', error);
        res.status(500).json({ error: 'Errore nel recupero dei fornitori' });
    }
});

module.exports = router;
