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
            `SELECT 
                c.id_categoria::text AS id,
                c.nome,
                c.descrizione,
                COUNT(p.id_prodotto) AS prodotti_totali
             FROM categorie_prodotti c
             LEFT JOIN prodotti p ON p.id_categoria = c.id_categoria
             GROUP BY c.id_categoria, c.nome, c.descrizione
             ORDER BY c.nome`
        );
        res.json(result.rows);
    } catch (error) {
        console.error('Errore nel recupero delle categorie:', error);
        res.status(500).json({ error: 'Errore nel recupero delle categorie' });
    }
});

/**
 * GET /api/categorie/macro
 * Nel nuovo schema non ci sono macro/sotto categorie: restituiamo tutte per compatibilit��
 */
router.get('/macro', async (req, res) => {
    try {
        const result = await db.query(
            `SELECT 
                c.id_categoria::text AS id,
                c.nome,
                c.descrizione
             FROM categorie_prodotti c
             ORDER BY c.nome`
        );
        res.json(result.rows);
    } catch (error) {
        console.error('Errore nel recupero delle macro-categorie:', error);
        res.status(500).json({ error: 'Errore nel recupero delle macro-categorie' });
    }
});

/**
 * GET /api/categorie/:parentId/sottocategorie
 * Compatibilit��: nel nuovo schema non ci sono sottocategorie, quindi torniamo []
 */
router.get('/:parentId/sottocategorie', async (req, res) => {
    try {
        res.json([]);
    } catch (error) {
        console.error('Errore nel recupero delle sottocategorie:', error);
        res.status(500).json({ error: 'Errore nel recupero delle sottocategorie' });
    }
});

module.exports = router;

