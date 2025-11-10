/**
 * ProductDao.kt
 *
 * RUOLO MVVM: Data Access Object (DAO) - Data Layer
 * - Definisce le operazioni sul database Room per la tabella products
 * - Fornisce query SQL type-safe tramite annotazioni Room
 * - Espone Flow per osservabilità reattiva dei cambiamenti nel DB
 *
 * RESPONSABILITÀ:
 * - @Query: selezionare prodotti dal database (getAll, getById, search)
 * - @Insert: inserire nuovi prodotti
 * - @Update: aggiornare prodotti esistenti
 * - @Delete: eliminare prodotti
 *
 * PATTERN: Data Access Object
 * - Annotato con @Dao (Room)
 * - Restituisce Flow<List<ProductEntity>> per reattività
 * - Usato SOLO dal Repository (mai da ViewModel/UI)
 *
 * ESEMPIO (futuro):
 * @Dao
 * interface ProductDao {
 *     @Query("SELECT * FROM products")
 *     fun observeAll(): Flow<List<ProductEntity>>
 *
 *     @Insert(onConflict = OnConflictStrategy.REPLACE)
 *     suspend fun insertAll(products: List<ProductEntity>)
 * }
 */
package it.unito.smartshopmobile.data.dao

// ProductDao - interfaccia Room per accesso prodotti

