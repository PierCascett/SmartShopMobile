const express = require('express');
const router = express.Router();
const db = require('../db');

/**
 * GET /api/categorie
 * Ritorna tutte le categorie con sovracategoria e conteggio prodotti
 */
router.get('/', async (_req, res) => {
    try {
        const result = await db.query(
            `SELECT 
                c.id_categoria::text AS id,
                c.nome,
                c.descrizione,
                c.id_sovracategoria::text AS "parentId",
                s.nome AS "parentName",
                COUNT(p.id_prodotto) AS prodotti_totali
             FROM categorie_prodotti c
             LEFT JOIN prodotti p ON p.id_categoria = c.id_categoria
             LEFT JOIN sovracategorie s ON s.id_sovracategoria = c.id_sovracategoria
             GROUP BY c.id_categoria, c.nome, c.descrizione, c.id_sovracategoria, s.nome
             ORDER BY s.nome, c.nome`
        );
        res.json(result.rows);
    } catch (error) {
        console.error('Errore nel recupero delle categorie:', error);
        res.status(500).json({ error: 'Errore nel recupero delle categorie' });
    }
});

/**
 * GET /api/categorie/macro
 * Restituisce le sovracategorie con conteggio categorie/prodotti
 */
router.get('/macro', async (_req, res) => {
    try {
        const result = await db.query(
            `SELECT 
                s.id_sovracategoria::text AS id,
                s.nome,
                NULL::text AS descrizione,
                NULL::text AS "parentId",
                NULL::text AS "parentName",
                COUNT(p.id_prodotto) AS prodotti_totali
             FROM sovracategorie s
             LEFT JOIN categorie_prodotti c ON c.id_sovracategoria = s.id_sovracategoria
             LEFT JOIN prodotti p ON p.id_categoria = c.id_categoria
             GROUP BY s.id_sovracategoria, s.nome
             ORDER BY s.nome`
        );
        res.json(result.rows);
    } catch (error) {
        console.error('Errore nel recupero delle macro-categorie:', error);
        res.status(500).json({ error: 'Errore nel recupero delle macro-categorie' });
    }
});

/**
 * GET /api/categorie/:parentId/sottocategorie
 * Ritorna le sottocategorie di una sovracategoria
 */
router.get('/:parentId/sottocategorie', async (req, res) => {
    try {
        const { parentId } = req.params;
        const result = await db.query(
            `SELECT 
                c.id_categoria::text AS id,
                c.nome,
                c.descrizione,
                c.id_sovracategoria::text AS "parentId",
                s.nome AS "parentName",
                COUNT(p.id_prodotto) AS prodotti_totali
             FROM categorie_prodotti c
             LEFT JOIN prodotti p ON p.id_categoria = c.id_categoria
             LEFT JOIN sovracategorie s ON s.id_sovracategoria = c.id_sovracategoria
             WHERE c.id_sovracategoria = $1
             GROUP BY c.id_categoria, c.nome, c.descrizione, c.id_sovracategoria, s.nome
             ORDER BY c.nome`,
            [parentId]
        );
        res.json(result.rows);
    } catch (error) {
        console.error('Errore nel recupero delle sottocategorie:', error);
        res.status(500).json({ error: 'Errore nel recupero delle sottocategorie' });
    }
});

module.exports = router;
