const express = require('express');
const bcrypt = require('bcrypt');
const fs = require('fs');
const path = require('path');
const multer = require('multer');
const sharp = require('sharp');
const router = express.Router();
const db = require('../db');

const PROFILE_DIR = path.join(__dirname, '..', 'images', 'profiles');
fs.mkdirSync(PROFILE_DIR, { recursive: true });
const upload = multer({
    storage: multer.memoryStorage(),
    limits: { fileSize: 4 * 1024 * 1024 } // 4MB
});

function getAvatarUrl(userId) {
    const candidate = path.join(PROFILE_DIR, `${userId}.jpg`);
    if (!fs.existsSync(candidate)) return null;
    const stats = fs.statSync(candidate);
    const version = Math.floor(stats.mtimeMs);
    return `/images/profiles/${userId}.jpg?v=${version}`;
}

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
        const responseUser = { ...user, avatar_url: getAvatarUrl(user.id_utente) };
        res.json({ user: responseUser });
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

        const created = insert.rows[0];
        res.status(201).json({ user: { ...created, avatar_url: getAvatarUrl(created.id_utente) } });
    } catch (error) {
        console.error('Errore registrazione:', error);
        res.status(500).json({ error: 'Errore durante la registrazione' });
    }
});

/**
 * PATCH /api/auth/profile/:userId
 * Aggiorna i dati anagrafici dell'utente (nome, cognome, telefono, email).
 */
router.patch('/profile/:userId', async (req, res) => {
    try {
        const userId = Number(req.params.userId);
        const { nome, cognome, telefono, email } = req.body || {};

        if (!userId) {
            return res.status(400).json({ error: 'userId obbligatorio' });
        }

        if (email) {
            const existing = await db.query(
                `SELECT 1 FROM utenti WHERE LOWER(email) = LOWER($1) AND id_utente <> $2`,
                [email, userId]
            );
            if (existing.rowCount > 0) {
                return res.status(409).json({ error: 'Email già in uso' });
            }
        }

        const current = await db.query(
            `SELECT id_utente FROM utenti WHERE id_utente = $1`,
            [userId]
        );
        if (current.rowCount === 0) {
            return res.status(404).json({ error: 'Utente non trovato' });
        }

        const updated = await db.query(
            `UPDATE utenti
             SET nome = COALESCE($2, nome),
                 cognome = COALESCE($3, cognome),
                 telefono = COALESCE($4, telefono),
                 email = COALESCE($5, email)
             WHERE id_utente = $1
             RETURNING id_utente, nome, cognome, email, telefono, ruolo`,
            [userId, nome, cognome, telefono, email]
        );

        const responseUser = {
            ...updated.rows[0],
            avatar_url: getAvatarUrl(updated.rows[0].id_utente)
        };
        res.json({ user: responseUser });
    } catch (error) {
        console.error('Errore aggiornamento profilo:', error);
        res.status(500).json({ error: 'Errore durante l\'aggiornamento del profilo' });
    }
});

/**
 * POST /api/auth/profile/:userId/photo
 * Carica/aggiorna la foto profilo dell'utente
 */
router.post('/profile/:userId/photo', upload.single('photo'), async (req, res) => {
    const userId = Number(req.params.userId);
    if (!userId) {
        return res.status(400).json({ error: 'userId obbligatorio' });
    }
    if (!req.file) {
        return res.status(400).json({ error: 'Nessun file caricato' });
    }
    if (!req.file.mimetype?.startsWith('image/')) {
        return res.status(400).json({ error: 'Formato immagine non valido' });
    }

    try {
        const filename = `${userId}.jpg`;
        const filepath = path.join(PROFILE_DIR, filename);
        await sharp(req.file.buffer)
            .resize(600, 600, { fit: 'cover' })
            .jpeg({ quality: 85 })
            .toFile(filepath);

        res.json({ avatarUrl: getAvatarUrl(userId) });
    } catch (error) {
        console.error('Errore upload foto profilo:', error);
        res.status(500).json({ error: 'Errore nel caricamento della foto profilo' });
    }
});

module.exports = router;

