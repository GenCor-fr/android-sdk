package tech.kissmyapps.android.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

@androidx.room.Database(
    version = 1,
    exportSchema = false,
    entities = [PurchaseEntity::class]
)
internal abstract class Database : RoomDatabase() {
    abstract fun purchasesDao(): PurchasesDao

    internal companion object {
        @Volatile
        private var INSTANCE: Database? = null

        fun getInstance(applicationContext: Context): Database {
            return INSTANCE ?: synchronized(this) {
                if (INSTANCE == null) {
                    INSTANCE = create(applicationContext)
                }

                INSTANCE!!
            }
        }

        private fun create(context: Context): Database {
            return Room.databaseBuilder(context, Database::class.java, name = "kma-database")
                .build()
        }
    }
}