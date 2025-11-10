const express = require('express');
const router = express.Router();
const db = require('../db');

/**
 * GET /api/categorie
 * Ritorna tutte le categorie
 */
router.get('/', async (req, res) => {
    try {
        const result = await db.query(
            'SELECT id, titolo, gruppo, ordine, parent_id FROM categorie_catalogo ORDER BY ordine, titolo'
        );
        res.json(result.rows);
    } catch (error) {
        console.error('Errore nel recupero delle categorie:', error);
        res.status(500).json({ error: 'Errore nel recupero delle categorie' });
    }
});

/**
 * GET /api/categorie/macro
 * Ritorna solo le macro-categorie (parent_id IS NULL)
 */
router.get('/macro', async (req, res) => {
    try {
        const result = await db.query(
            'SELECT id, titolo, gruppo, ordine FROM categorie_catalogo WHERE parent_id IS NULL ORDER BY ordine, titolo'
        );
        res.json(result.rows);
    } catch (error) {
        console.error('Errore nel recupero delle macro-categorie:', error);
        res.status(500).json({ error: 'Errore nel recupero delle macro-categorie' });
    }
});

/**
 * GET /api/categorie/:parentId/sottocategorie
 * Ritorna le sottocategorie di una categoria padre
 */
router.get('/:parentId/sottocategorie', async (req, res) => {
    try {
        const { parentId } = req.params;
        const result = await db.query(
            'SELECT id, titolo, gruppo, ordine, parent_id FROM categorie_catalogo WHERE parent_id = $1 ORDER BY ordine, titolo',
            [parentId]
        );
        res.json(result.rows);
    } catch (error) {
        console.error('Errore nel recupero delle sottocategorie:', error);
        res.status(500).json({ error: 'Errore nel recupero delle sottocategorie' });
    }
});

module.exports = router;

