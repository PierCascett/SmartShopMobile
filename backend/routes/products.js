const express = require('express');
const router = express.Router();
const db = require('../db');

/**
 * GET /api/prodotti
 * Ritorna tutti i prodotti
 */
router.get('/', async (req, res) => {
    try {
        const result = await db.query(
            `SELECT
                id,
                nome as name,
                marca as brand,
                categoria_id as "categoryId",
                prezzo as price,
                vecchio_prezzo as "oldPrice",
                disponibilita as availability,
                tag as tags,
                descrizione as description
            FROM prodotti_catalogo
            ORDER BY nome`
        );
        res.json(result.rows);
    } catch (error) {
        console.error('Errore nel recupero dei prodotti:', error);
        res.status(500).json({ error: 'Errore nel recupero dei prodotti' });
    }
});

/**
 * GET /api/prodotti/categoria/:categoryId
 * Ritorna i prodotti di una specifica categoria
 */
router.get('/categoria/:categoryId', async (req, res) => {
    try {
        const { categoryId } = req.params;
        const result = await db.query(
            `SELECT
                id,
                nome as name,
                marca as brand,
                categoria_id as "categoryId",
                prezzo as price,
                vecchio_prezzo as "oldPrice",
                disponibilita as availability,
                tag as tags,
                descrizione as description
            FROM prodotti_catalogo
            WHERE categoria_id = $1
            ORDER BY nome`,
            [categoryId]
        );
        res.json(result.rows);
    } catch (error) {
        console.error('Errore nel recupero dei prodotti per categoria:', error);
        res.status(500).json({ error: 'Errore nel recupero dei prodotti' });
    }
});

/**
 * GET /api/prodotti/search?q=query
 * Cerca prodotti per nome o marca
 */
router.get('/search', async (req, res) => {
    try {
        const { q } = req.query;
        if (!q || q.trim().length === 0) {
            return res.json([]);
        }

        const searchTerm = `%${q.toLowerCase()}%`;
        const result = await db.query(
            `SELECT
                id,
                nome as name,
                marca as brand,
                categoria_id as "categoryId",
                prezzo as price,
                vecchio_prezzo as "oldPrice",
                disponibilita as availability,
                tag as tags,
                descrizione as description
            FROM prodotti_catalogo
            WHERE LOWER(nome) LIKE $1 OR LOWER(marca) LIKE $1
            ORDER BY nome
            LIMIT 50`,
            [searchTerm]
        );
        res.json(result.rows);
    } catch (error) {
        console.error('Errore nella ricerca dei prodotti:', error);
        res.status(500).json({ error: 'Errore nella ricerca' });
    }
});

module.exports = router;

