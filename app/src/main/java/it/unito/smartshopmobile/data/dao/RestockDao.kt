package it.unito.smartshopmobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import it.unito.smartshopmobile.data.entity.Restock
import kotlinx.coroutines.flow.Flow

@Dao
interface RestockDao {
    @Query("SELECT * FROM riordini_cache ORDER BY data_ordine DESC")
    fun getRestocks(): Flow<List<Restock>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<Restock>)

    @Query("DELETE FROM riordini_cache")
    suspend fun deleteAll()
}

