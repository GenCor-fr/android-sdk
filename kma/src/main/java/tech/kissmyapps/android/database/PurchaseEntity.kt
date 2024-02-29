package tech.kissmyapps.android.database

import androidx.annotation.IntDef
import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = PurchaseEntity.TABLE_NAME,
    primaryKeys = [PurchaseEntity.COLUMN_PURCHASE_TOKEN]
)
internal data class PurchaseEntity(
    @ColumnInfo(name = COLUMN_PRODUCT_ID)
    val productId: String,

    @ProductType
    @ColumnInfo(name = COLUMN_PRODUCT_TYPE)
    val productType: Int,

    @ColumnInfo(name = COLUMN_PURCHASE_TOKEN)
    val purchaseToken: String,

    @ColumnInfo(name = COLUMN_PRICE)
    val price: Double,

    @ColumnInfo(name = COLUMN_CURRENCY)
    val currency: String
) {
    companion object {
        const val TABLE_NAME = "purchase"
        const val COLUMN_PRODUCT_ID = "product_id"
        const val COLUMN_PRODUCT_TYPE = "product_type"
        const val COLUMN_PURCHASE_TOKEN = "purchase_token"
        const val COLUMN_PRICE = "price"
        const val COLUMN_CURRENCY = "currency"
    }

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(value = [ProductType.SUBS, ProductType.INAPP])
    annotation class ProductType {
        companion object {
            const val SUBS = 1
            const val INAPP = 2
        }
    }
}