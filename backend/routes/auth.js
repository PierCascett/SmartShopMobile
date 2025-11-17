const express = require('express');
const bcrypt = require('bcrypt');
const router = express.Router();
const db = require('../db');

/**
 * POST /api/auth/login
 * Autentica un utente e restituisce dati + ruolo dal DB
 */
router.post('/login', async (req, res) => {
    try {
        const { email, password } = req.body;
        if (!email || !password) {
            return res.status(400).json({ error: 'Email e password sono obbligatorie' });
        }

        const result = await db.query(
            `SELECT id_utente, nome, cognome, email, telefono, ruolo, password
             FROM utenti
             WHERE LOWER(email) = LOWER($1)
             LIMIT 1`,
            [email]
        );

        if (result.rowCount === 0) {
            return res.status(401).json({ error: 'Credenziali non valide' });
        }

        const user = result.rows[0];
        const isValid = await bcrypt.compare(password, user.password || '');
        if (!isValid) {
            return res.status(401).json({ error: 'Credenziali non valide' });
        }

        // Non inviare l'hash al client
        delete user.password;
        res.json({ user });
    } catch (error) {
        console.error('Errore login:', error);
        res.status(500).json({ error: 'Errore durante il login' });
    }
});

/**
 * POST /api/auth/register
 * Crea un nuovo utente (ruolo di default Cliente se non specificato)
 */
router.post('/register', async (req, res) => {
    try {
        const { nome, cognome, email, telefono, password, ruolo = 'Cliente' } = req.body;
        if (!nome || !cognome || !email || !password) {
            return res.status(400).json({ error: 'Nome, cognome, email e password sono obbligatori' });
        }

        const existing = await db.query(
            'SELECT 1 FROM utenti WHERE LOWER(email) = LOWER($1)',
            [email]
        );
        if (existing.rowCount > 0) {
            return res.status(409).json({ error: 'Email gi\u00e0 registrata' });
        }

        const hashed = await bcrypt.hash(password, 10);
        const insert = await db.query(
            `INSERT INTO utenti (nome, cognome, email, telefono, ruolo, password)
             VALUES ($1, $2, $3, $4, $5, $6)
             RETURNING id_utente, nome, cognome, email, telefono, ruolo`,
            [nome, cognome, email, telefono, ruolo, hashed]
        );

        res.status(201).json({ user: insert.rows[0] });
    } catch (error) {
        console.error('Errore registrazione:', error);
        res.status(500).json({ error: 'Errore durante la registrazione' });
    }
});

module.exports = router;

