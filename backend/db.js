const { Pool } = require('pg');
require('dotenv').config();

// Configurazione pool PostgreSQL
const pool = new Pool({
    host: process.env.DB_HOST || 'localhost',
    port: process.env.DB_PORT || 5432,
    database: process.env.DB_NAME || 'SmartShopMobile',
    user: process.env.DB_USER || 'postgres',
    password: process.env.DB_PASSWORD || '12345',
    max: 20,
    idleTimeoutMillis: 30000,
    connectionTimeoutMillis: 2000,
});

// Test della connessione
pool.on('connect', () => {
    console.log('✅ Connesso a PostgreSQL');
});

pool.on('error', (err) => {
    console.error('Errore nella connessione PostgreSQL:', err);
    process.exit(-1);
});

// Funzione helper per query
const query = async (text, params) => {
    const start = Date.now();
    try {
        const res = await pool.query(text, params);
        const duration = Date.now() - start;
        console.log('Query eseguita', { text, duration, rows: res.rowCount });
        return res;
    } catch (error) {
        console.error('Errore query:', error);
        throw error;
    }
};

module.exports = {
    query,
    pool
};

