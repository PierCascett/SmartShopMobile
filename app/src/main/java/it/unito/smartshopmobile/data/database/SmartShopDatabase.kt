/**
 * SmartShopDatabase.kt
 * 
 * MVVM: Model Layer - Database Room (SQLite)
 * 
 * FUNZIONAMENTO:
 * - Database principale dell'app (Room)
 * - Singleton pattern: unica istanza per tutta l'app
 * - Definisce Entity (@Database) e fornisce DAO (factory methods)
 * - TypeConverters per tipi complessi (List, Date, ecc.)
 * 
 * PATTERN MVVM:
 * - Database abstraction: Room sopra SQLite
 * - Singleton: thread-safe con synchronized
 * - Factory methods: categoryDao(), productDao(), ecc.
 * - Migration strategy: versione 13 (fallbackToDestructiveMigration per dev)
 * 
 * NOTE:
 * - fallbackToDestructiveMigration: cancella DB se schema cambia (solo dev!)
 * - In produzione: definire Migration per preservare dati utente
 */
package it.unito.smartshopmobile.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import it.unito.smartshopmobile.data.dao.CategoryDao
import it.unito.smartshopmobile.data.dao.OrderDao
import it.unito.smartshopmobile.data.dao.ProductDao
import it.unito.smartshopmobile.data.dao.RestockDao
import it.unito.smartshopmobile.data.dao.ShelfDao
import it.unito.smartshopmobile.data.dao.SupplierDao
import it.unito.smartshopmobile.data.entity.Category
import it.unito.smartshopmobile.data.entity.Order
import it.unito.smartshopmobile.data.entity.OrderLine
import it.unito.smartshopmobile.data.entity.Product
import it.unito.smartshopmobile.data.entity.Restock
import it.unito.smartshopmobile.data.entity.Shelf
import it.unito.smartshopmobile.data.entity.Supplier

/**
 * Database Room principale dell'applicazione SmartShop.
 *
 * Gestisce la persistenza locale di tutte le entità (prodotti, ordini, categorie, ecc.)
 * con pattern Singleton thread-safe. Fornisce accesso ai DAO per operazioni CRUD.
 *
 * Caratteristiche:
 * - Versione 13: include indirizzo spedizione negli ordini
 * - Singleton pattern: unica istanza per l'intera applicazione
 * - TypeConverters per List<String> (tags prodotti)
 * - fallbackToDestructiveMigration: ricrea DB se schema cambia (solo dev!)
 *
 * Note produzione:
 * - Implementare Migration per preservare dati utente
 * - Rimuovere fallbackToDestructiveMigration
 * - Attivare exportSchema per tracciare versioni
 *
 * @property categoryDao DAO per operazioni su categorie
 * @property productDao DAO per operazioni su prodotti
 * @property orderDao DAO per operazioni su ordini e righe
 * @property restockDao DAO per operazioni su riordini
 * @property shelfDao DAO per operazioni su scaffali
 * @property supplierDao DAO per operazioni su fornitori
 */
@Database(
    entities = [Category::class, Product::class, Order::class, OrderLine::class, Restock::class, Shelf::class, Supplier::class],
    version = 13, // include indirizzo spedizione negli ordini
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SmartShopDatabase : RoomDatabase() {

    /**
     * DAO per accesso alle categorie prodotti.
     */
    abstract fun categoryDao(): CategoryDao
    /**
     * Helper per gestire product dao.
     */
    abstract fun productDao(): ProductDao
    /**
     * Helper per gestire order dao.
     */
    abstract fun orderDao(): OrderDao
    /**
     * Helper per gestire restock dao.
     */
    abstract fun restockDao(): RestockDao
    /**
     * Helper per gestire shelf dao.
     */
    abstract fun shelfDao(): ShelfDao
    /**
     * Helper per gestire supplier dao.
     */
    abstract fun supplierDao(): SupplierDao

    companion object {
        @Volatile
        private var INSTANCE: SmartShopDatabase? = null
        /**
         * Helper per gestire get database.
         */

        fun getDatabase(context: Context): SmartShopDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SmartShopDatabase::class.java,
                    "smartshop_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
