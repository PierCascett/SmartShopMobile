/**
 * AppDatabase.kt
 *
 * RUOLO MVVM: Database - Data Layer (Room Database)
 * - Punto di accesso principale al database Room dell'applicazione
 * - Definisce tutte le Entity e fornisce i DAO
 * - Gestisce versioning, migrazioni e configurazione DB
 *
 * RESPONSABILITÀ:
 * - @Database: definisce versione e lista di Entity
 * - Fornisce metodi per ottenere i DAO (productDao(), cartDao(), ecc.)
 * - Implementa Singleton pattern per una sola istanza
 * - Gestisce migrazioni tra versioni del database
 *
 * PATTERN: Singleton + Abstract Factory
 * - Una sola istanza del database per tutta l'app
 * - Factory methods per ottenere i DAO
 * - Usato SOLO dai Repository (mai da ViewModel/UI)
 *
 * ESEMPIO (futuro):
 * @Database(
 *     entities = [ProductEntity::class, CartEntity::class],
 *     version = 1,
 *     exportSchema = false
 * )
 * abstract class AppDatabase : RoomDatabase() {
 *     abstract fun productDao(): ProductDao
 *     abstract fun cartDao(): CartDao
 *
 *     companion object {
 *         @Volatile private var INSTANCE: AppDatabase? = null
 *         fun getInstance(context: Context): AppDatabase {
 *             return INSTANCE ?: synchronized(this) {
 *                 Room.databaseBuilder(
 *                     context, AppDatabase::class.java, "smartshop.db"
 *                 ).build().also { INSTANCE = it }
 *             }
 *         }
 *     }
 * }
 */
package it.unito.smartshopmobile.data.database

// AppDatabase - database Room principale dell'applicazione

