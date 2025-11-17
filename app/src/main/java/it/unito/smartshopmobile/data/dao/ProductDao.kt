package it.unito.smartshopmobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import it.unito.smartshopmobile.data.entity.Product
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Query("SELECT * FROM prodotti_catalogo")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM prodotti_catalogo WHERE categoria_id = :categoryId")
    fun getProductsByCategory(categoryId: String): Flow<List<Product>>

    // id prodotto non è unico (stesso prodotto in più scaffali): restituiamo una riga qualsiasi
    @Query("SELECT * FROM prodotti_catalogo WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: String): Product?

    @Query("SELECT * FROM prodotti_catalogo WHERE nome LIKE '%' || :query || '%' OR marca LIKE '%' || :query || '%'")
    fun searchProducts(query: String): Flow<List<Product>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<Product>)

    @Query("DELETE FROM prodotti_catalogo")
    suspend fun deleteAll()
}
