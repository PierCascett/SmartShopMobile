const express = require('express');
const cors = require('cors');
const morgan = require('morgan');
const path = require('path');
const os = require('os');
const { promisify } = require('util');
const { exec } = require('child_process');
const execPromise = promisify(exec);
require('dotenv').config();

const categoriesRoutes = require('./routes/categories');
const productsRoutes = require('./routes/products');
const authRoutes = require('./routes/auth');
const ordersRoutes = require('./routes/orders');
const restockRoutes = require('./routes/restock');
const suppliersRoutes = require('./routes/suppliers');
const db = require('./db');
const shelvesRoutes = require('./routes/shelves');
const inventoryRoutes = require('./routes/inventory');

const app = express();
const PORT = process.env.PORT || 3000;
const FIREWALL_RULE_NAME = 'SmartShop Backend Temp';
const LAN_IP = process.env.HOST || getLocalIp() || 'localhost';

// Middleware
app.use(cors());
app.use(express.json());
app.use(morgan('dev'));

// Serve static images
app.use('/images', express.static(path.join(__dirname, 'images')));

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
app.use('/api/auth', authRoutes);
app.use('/api/ordini', ordersRoutes);
app.use('/api/riordini', restockRoutes);
app.use('/api/fornitori', suppliersRoutes);
app.use('/api/scaffali', shelvesRoutes);
app.use('/api/magazzino', inventoryRoutes);

// 404 handler
app.use((req, res) => {
    res.status(404).json({ error: 'Endpoint non trovato' });
});

// Error handler
app.use((err, req, res, next) => {
    console.error('Errore server:', err);
    res.status(500).json({ error: 'Errore interno del server' });
});

function getLocalIp() {
    const nets = os.networkInterfaces();
    for (const name of Object.keys(nets)) {
        for (const net of nets[name] || []) {
            if (net.family === 'IPv4' && !net.internal) {
                return net.address;
            }
        }
    }
    return null;
}

// Prova ad aprire la porta nel firewall (se siamo in ambiente Windows e abbiamo privilegi)
async function tryAddFirewallRule() {
    try {
        // Aggiunge la regola se non esiste (netsh non ha "if not exists", quindi proviamo comunque)
        await execPromise(`netsh advfirewall firewall add rule name="${FIREWALL_RULE_NAME}" dir=in action=allow protocol=TCP localport=${PORT}`);
        console.log(`Regola firewall aggiunta: ${FIREWALL_RULE_NAME} porta ${PORT}`);
    } catch (err) {
        console.warn('Impossibile aggiungere la regola firewall (potrebbe non essere necessario o mancare privilegi):', err.message || err);
    }
}

// Avvio server (salviamo il riferimento per poter chiudere)
(async () => {
    await tryAddFirewallRule();

    const server = app.listen(PORT, '0.0.0.0', () => {
        console.log(`\n╔═════════════════════════════════════════╗\n║   SmartShop Backend Server              ║\n║   Port: ${PORT}                         ║\n║   Local: http://localhost:${PORT}       ║\n║   Network: http://${LAN_IP}:${PORT}  ║\n╚═════════════════════════════════════════╝\n    `);
    });

    // Prevent double shutdown
    let isShuttingDown = false;

    async function gracefulShutdown(reason) {
        if (isShuttingDown) {
            console.log('Shutdown già in corso, ignorando ulteriore segnale');
            return;
        }
        isShuttingDown = true;

        console.log(`Ricevuto ${reason} - avvio graceful shutdown...`);

        // 1) Chiudi il server HTTP (rifiuta nuove connessioni ma serve quelle attive)
        try {
            await new Promise((resolve, reject) => {
                if (!server || !server.close) return resolve();
                server.close(err => (err ? reject(err) : resolve()));
                setTimeout(resolve, 5000);
            });
            console.log('Server HTTP chiuso');
        } catch (err) {
            console.warn('Errore durante la chiusura del server HTTP:', err);
        }

        // 2) Chiudi il pool DB se disponibile
        try {
            if (db) {
                if (db.pool && typeof db.pool.end === 'function') {
                    await db.pool.end();
                    console.log('Pool DB chiuso (db.pool.end)');
                } else if (typeof db.end === 'function') {
                    await db.end();
                    console.log('Connessione DB chiusa (db.end)');
                } else if (typeof db.close === 'function') {
                    await db.close();
                    console.log('Connessione DB chiusa (db.close)');
                }
            }
        } catch (err) {
            console.warn('Errore durante la chiusura del DB:', err);
        }

        // 3) Rimuovi la regola firewall aggiunta (se possibile)
        try {
            await execPromise(`netsh advfirewall firewall delete rule name="${FIREWALL_RULE_NAME}"`);
            console.log('Regola firewall rimossa (se esistente)');
        } catch (err) {
            console.warn('Impossibile rimuovere la regola firewall (potrebbe richiedere privilegi):', err.message || err);
        }

        console.log('Shutdown completato, esco.');
        setTimeout(() => process.exit(0), 100);
    }

    // Gestione segnali ed errori
    process.on('SIGINT', () => gracefulShutdown('SIGINT')); // Ctrl+C
    process.on('SIGTERM', () => gracefulShutdown('SIGTERM'));
    process.on('SIGBREAK', () => gracefulShutdown('SIGBREAK')); // Ctrl+Break su Windows
    process.on('uncaughtException', (err) => {
        console.error('Uncaught Exception:', err);
        gracefulShutdown('uncaughtException');
    });
    process.on('unhandledRejection', (reason, promise) => {
        console.error('Unhandled Rejection at:', promise, 'reason:', reason);
        gracefulShutdown('unhandledRejection');
    });

    // Esport (utile se si esegue in test o altrove)
    module.exports = app;
})();
