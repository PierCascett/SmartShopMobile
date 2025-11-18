package it.unito.smartshopmobile.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import it.unito.smartshopmobile.data.dao.CategoryDao
import it.unito.smartshopmobile.data.dao.OrderDao
import it.unito.smartshopmobile.data.dao.ProductDao
import it.unito.smartshopmobile.data.dao.RestockDao
import it.unito.smartshopmobile.data.dao.ShelfDao
import it.unito.smartshopmobile.data.entity.Category
import it.unito.smartshopmobile.data.entity.Order
import it.unito.smartshopmobile.data.entity.OrderLine
import it.unito.smartshopmobile.data.entity.Product
import it.unito.smartshopmobile.data.entity.Restock
import it.unito.smartshopmobile.data.entity.Shelf
import androidx.room.TypeConverters
import it.unito.smartshopmobile.data.database.Converters

@Database(
    entities = [Category::class, Product::class, Order::class, OrderLine::class, Restock::class, Shelf::class],
    version = 7,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SmartShopDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun orderDao(): OrderDao
    abstract fun restockDao(): RestockDao
    abstract fun shelfDao(): ShelfDao

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
