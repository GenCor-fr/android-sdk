package tech.kissmyapps.android.purchases

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import tech.kissmyapps.android.purchases.model.CustomerInfo
import timber.log.Timber

internal interface PurchasesDataStore {
    fun getCustomerInfo(): Flow<CustomerInfo?>

    suspend fun setCustomerInfo(customerInfo: CustomerInfo?)

    suspend fun isPurchasesSynced(): Boolean

    suspend fun setPurchasesSynced(isSynced: Boolean)
}

internal class PurchasesPreferencesDataStore(
    private val preferencesDataStore: DataStore<Preferences>
) : PurchasesDataStore {
    private val keyActiveSubscriptions = stringSetPreferencesKey("active_subscriptions")
    private val keyAllPurchasedProductIds = stringSetPreferencesKey("all_purchased_product_ids")
    private val keyIsPurchasesSynced = booleanPreferencesKey("is_purchases_synced")

    override fun getCustomerInfo(): Flow<CustomerInfo?> {
        return preferencesDataStore.data.map {
            val activeSubscriptions = it[keyActiveSubscriptions]
            val allPurchasedProductIds = it[keyAllPurchasedProductIds]

            if (activeSubscriptions == null && allPurchasedProductIds == null) {
                null
            } else {
                CustomerInfo(
                    activeSubscriptions.orEmpty(),
                    allPurchasedProductIds.orEmpty()
                )
            }
        }
    }

    override suspend fun setCustomerInfo(customerInfo: CustomerInfo?) {
        if (customerInfo == null) {
            return
        }

        try {
            preferencesDataStore.edit {
                it[keyActiveSubscriptions] = customerInfo.activeSubscriptions
                it[keyAllPurchasedProductIds] = customerInfo.allPurchasedProductIds
            }
        } catch (e: Throwable) {
            Timber.e(e)
        }
    }

    override suspend fun isPurchasesSynced(): Boolean {
        return try {
            return preferencesDataStore.data
                .map { it[keyIsPurchasesSynced] ?: false }
                .firstOrNull() ?: false
        } catch (e: Throwable) {
            Timber.e(e)
            false
        }
    }

    override suspend fun setPurchasesSynced(isSynced: Boolean) {
        try {
            preferencesDataStore.edit {
                it[keyIsPurchasesSynced] = isSynced
            }
        } catch (e: Throwable) {
            Timber.e(e)
        }
    }

    companion object {
        fun create(applicationContext: Context): PurchasesDataStore {
            val dataStore = PreferenceDataStoreFactory.create {
                applicationContext.preferencesDataStoreFile("purchases")
            }

            return PurchasesPreferencesDataStore(dataStore)
        }
    }
}