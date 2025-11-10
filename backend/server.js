const express = require('express');
const cors = require('cors');
const morgan = require('morgan');
require('dotenv').config();

const categoriesRoutes = require('./routes/categories');
const productsRoutes = require('./routes/products');
const db = require('./db');

const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(cors());
app.use(express.json());
app.use(morgan('dev'));

// Health check
app.get('/health', async (req, res) => {
    try {
        await db.query('SELECT 1');
        res.json({
            status: 'healthy',
            database: 'connected',
            timestamp: new Date().toISOString()
        });
    } catch (error) {
        res.status(500).json({
            status: 'unhealthy',
            database: 'disconnected',
            error: error.message
        });
    }
});

// API Routes
app.use('/api/categorie', categoriesRoutes);
app.use('/api/prodotti', productsRoutes);

// 404 handler
app.use((req, res) => {
    res.status(404).json({ error: 'Endpoint non trovato' });
});

// Error handler
app.use((err, req, res, next) => {
    console.error('Errore server:', err);
    res.status(500).json({ error: 'Errore interno del server' });
});

// Avvio server
app.listen(PORT, '0.0.0.0', () => {
    console.log(`
╔═════════════════════════════════════════╗
║   SmartShop Backend Server              ║
║   Port: ${PORT}                         ║
║   Local: http://localhost:${PORT}       ║
║   Network: http://192.168.1.51:${PORT}  ║
╚═════════════════════════════════════════╝
    `);
});

// Graceful shutdown
process.on('SIGTERM', () => {
    db.pool.end(() => process.exit(0));
});


