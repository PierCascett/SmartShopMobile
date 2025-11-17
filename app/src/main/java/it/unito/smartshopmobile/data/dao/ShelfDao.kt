package it.unito.smartshopmobile.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import it.unito.smartshopmobile.data.entity.Shelf
import kotlinx.coroutines.flow.Flow

@Dao
interface ShelfDao {
    @Query("SELECT * FROM scaffali ORDER BY id_scaffale ASC")
    fun getAll(): Flow<List<Shelf>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<Shelf>)

    @Query("DELETE FROM scaffali")
    suspend fun deleteAll()
}

