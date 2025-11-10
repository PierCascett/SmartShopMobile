package it.unito.smartshopmobile.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import it.unito.smartshopmobile.data.dao.CategoryDao
import it.unito.smartshopmobile.data.dao.ProductDao
import it.unito.smartshopmobile.data.entity.Category
import it.unito.smartshopmobile.data.entity.Product

@Database(
    entities = [Category::class, Product::class],
    version = 1,
    exportSchema = false
)
abstract class SmartShopDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao

    companion object {
        @Volatile
        private var INSTANCE: SmartShopDatabase? = null

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

