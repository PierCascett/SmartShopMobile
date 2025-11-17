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
                p.id_prodotto AS id,
                p.nome AS name,
                p.marca AS brand,
                p.id_categoria::text AS "categoryId",
                c.nome AS "categoryName",
                c.descrizione AS "categoryDescription",
                cat.id_catalogo AS "catalogId",
                cat.quantita_disponibile AS "catalogQuantity",
                cat.prezzo AS price,
                cat.vecchio_prezzo AS "oldPrice",
                cat.id_scaffale AS "shelfId",
                COALESCE(SUM(mg.quantita_disponibile), 0) AS "warehouseQuantity",
                COALESCE(SUM(mg.quantita_disponibile), 0) + cat.quantita_disponibile AS "totalQuantity",
                CASE
                    WHEN cat.quantita_disponibile <= 0 THEN 'Non disponibile'
                    WHEN cat.quantita_disponibile <= 5 THEN 'Quasi esaurito'
                    ELSE 'Disponibile'
                END AS availability,
                COALESCE(
                    json_agg(DISTINCT t.nome) FILTER (WHERE t.nome IS NOT NULL),
                    '[]'
                ) AS tags,
                p.descrizione AS description
            FROM prodotti p
            JOIN catalogo cat ON cat.id_prodotto = p.id_prodotto
            LEFT JOIN magazzino mg ON mg.id_prodotto = p.id_prodotto
            LEFT JOIN prodotti_tag pt ON pt.id_prodotto = p.id_prodotto
            LEFT JOIN tag t ON t.id_tag = pt.id_tag
            LEFT JOIN categorie_prodotti c ON c.id_categoria = p.id_categoria
            GROUP BY p.id_prodotto, p.nome, p.marca, p.id_categoria, p.descrizione,
                     cat.id_catalogo, cat.quantita_disponibile, cat.prezzo, cat.vecchio_prezzo,
                     cat.id_scaffale, c.nome, c.descrizione
            ORDER BY p.nome`
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
                p.id_prodotto AS id,
                p.nome AS name,
                p.marca AS brand,
                p.id_categoria::text AS "categoryId",
                c.nome AS "categoryName",
                c.descrizione AS "categoryDescription",
                cat.id_catalogo AS "catalogId",
                cat.quantita_disponibile AS "catalogQuantity",
                cat.prezzo AS price,
                cat.vecchio_prezzo AS "oldPrice",
                cat.id_scaffale AS "shelfId",
                COALESCE(SUM(mg.quantita_disponibile), 0) AS "warehouseQuantity",
                COALESCE(SUM(mg.quantita_disponibile), 0) + cat.quantita_disponibile AS "totalQuantity",
                CASE
                    WHEN cat.quantita_disponibile <= 0 THEN 'Non disponibile'
                    WHEN cat.quantita_disponibile <= 5 THEN 'Quasi esaurito'
                    ELSE 'Disponibile'
                END AS availability,
                COALESCE(
                    json_agg(DISTINCT t.nome) FILTER (WHERE t.nome IS NOT NULL),
                    '[]'
                ) AS tags,
                p.descrizione AS description
            FROM prodotti p
            JOIN catalogo cat ON cat.id_prodotto = p.id_prodotto
            LEFT JOIN magazzino mg ON mg.id_prodotto = p.id_prodotto
            LEFT JOIN prodotti_tag pt ON pt.id_prodotto = p.id_prodotto
            LEFT JOIN tag t ON t.id_tag = pt.id_tag
            LEFT JOIN categorie_prodotti c ON c.id_categoria = p.id_categoria
            WHERE p.id_categoria = $1
            GROUP BY p.id_prodotto, p.nome, p.marca, p.id_categoria, p.descrizione,
                     cat.id_catalogo, cat.quantita_disponibile, cat.prezzo, cat.vecchio_prezzo,
                     cat.id_scaffale, c.nome, c.descrizione
            ORDER BY p.nome`,
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
                p.id_prodotto AS id,
                p.nome AS name,
                p.marca AS brand,
                p.id_categoria::text AS "categoryId",
                c.nome AS "categoryName",
                c.descrizione AS "categoryDescription",
                cat.id_catalogo AS "catalogId",
                cat.quantita_disponibile AS "catalogQuantity",
                cat.prezzo AS price,
                cat.vecchio_prezzo AS "oldPrice",
                cat.id_scaffale AS "shelfId",
                COALESCE(SUM(mg.quantita_disponibile), 0) AS "warehouseQuantity",
                COALESCE(SUM(mg.quantita_disponibile), 0) + cat.quantita_disponibile AS "totalQuantity",
                CASE
                    WHEN cat.quantita_disponibile <= 0 THEN 'Non disponibile'
                    WHEN cat.quantita_disponibile <= 5 THEN 'Quasi esaurito'
                    ELSE 'Disponibile'
                END AS availability,
                COALESCE(
                    json_agg(DISTINCT t.nome) FILTER (WHERE t.nome IS NOT NULL),
                    '[]'
                ) AS tags,
                p.descrizione AS description
            FROM prodotti p
            JOIN catalogo cat ON cat.id_prodotto = p.id_prodotto
            LEFT JOIN magazzino mg ON mg.id_prodotto = p.id_prodotto
            LEFT JOIN prodotti_tag pt ON pt.id_prodotto = p.id_prodotto
            LEFT JOIN tag t ON t.id_tag = pt.id_tag
            LEFT JOIN categorie_prodotti c ON c.id_categoria = p.id_categoria
            WHERE LOWER(p.nome) LIKE $1 OR LOWER(p.marca) LIKE $1
            GROUP BY p.id_prodotto, p.nome, p.marca, p.id_categoria, p.descrizione,
                     cat.id_catalogo, cat.quantita_disponibile, cat.prezzo, cat.vecchio_prezzo,
                     cat.id_scaffale, c.nome, c.descrizione
            ORDER BY p.nome
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
