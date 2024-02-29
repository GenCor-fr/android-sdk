package tech.kissmyapps.android.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import tech.kissmyapps.android.database.PurchaseEntity.Companion.COLUMN_PURCHASE_TOKEN
import tech.kissmyapps.android.database.PurchaseEntity.Companion.TABLE_NAME

@Dao
internal abstract class PurchasesDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun insert(purchase: PurchaseEntity)

    @Query("DELETE FROM $TABLE_NAME WHERE $COLUMN_PURCHASE_TOKEN = :purchaseToken")
    abstract fun delete(purchaseToken: String)

    @Query("SELECT * FROM $TABLE_NAME")
    abstract fun getAll(): List<PurchaseEntity>
}