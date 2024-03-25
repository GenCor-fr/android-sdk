package tech.kissmyapps.android.purchases.revenuecat

import android.app.Activity
import com.revenuecat.purchases.ProductType.INAPP
import com.revenuecat.purchases.ProductType.SUBS
import com.revenuecat.purchases.ProductType.UNKNOWN
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.awaitGetProducts
import com.revenuecat.purchases.awaitPurchase
import com.revenuecat.purchases.awaitRestore
import com.revenuecat.purchases.awaitSyncPurchases
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.models.StoreProduct
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import tech.kissmyapps.android.purchases.Purchases
import tech.kissmyapps.android.purchases.PurchasesDataStore
import tech.kissmyapps.android.purchases.PurchasesError.NetworkError
import tech.kissmyapps.android.purchases.PurchasesError.ProductAlreadyPurchasedError
import tech.kissmyapps.android.purchases.PurchasesError.PurchaseCancelledError
import tech.kissmyapps.android.purchases.PurchasesError.UnknownError
import tech.kissmyapps.android.purchases.PurchasesException
import tech.kissmyapps.android.purchases.model.CustomerInfo
import tech.kissmyapps.android.purchases.model.Period
import tech.kissmyapps.android.purchases.model.Price
import tech.kissmyapps.android.purchases.model.PricingPhase
import tech.kissmyapps.android.purchases.model.Product
import tech.kissmyapps.android.purchases.model.ProductType
import tech.kissmyapps.android.purchases.model.Purchase
import tech.kissmyapps.android.purchases.model.SubscriptionOption
import tech.kissmyapps.android.purchases.model.SubscriptionOptions
import timber.log.Timber
import com.revenuecat.purchases.CustomerInfo as RcCustomerInfo
import com.revenuecat.purchases.Purchases as RevenueCat
import com.revenuecat.purchases.models.Period as RcPeriod
import com.revenuecat.purchases.models.Price as RcPrice

internal class RevenueCatPurchases(
    configuration: RevenueCatConfiguration,
    private val dataStore: PurchasesDataStore,
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : Purchases {
    private val purchases: RevenueCat = RevenueCat.configure(
        PurchasesConfiguration.Builder(configuration.context, configuration.apiKey)
            .appUserID(configuration.appUserId)
            .build()
    )
    
    init {
        purchases.collectDeviceIdentifiers()

        if (!configuration.appsFlyerUID.isNullOrBlank()) {
            purchases.setAppsflyerID(configuration.appsFlyerUID)
        }

        if (!configuration.appUserId.isNullOrBlank()) {
            purchases.setAttributes(mapOf("\$amplitudeUserId" to configuration.appUserId))
        }

        purchases.updatedCustomerInfoListener = UpdatedCustomerInfoListener(::onCustomerUpdated)
    }

    override suspend fun getCustomerInfo(): CustomerInfo {
        return syncPurchases() ?: try {
            val customerInfo = purchases.awaitCustomerInfo().toInternalModel()
            dataStore.setCustomerInfo(customerInfo)

            Timber.d("Customer info: $customerInfo.")

            customerInfo
        } catch (e: Throwable) {
            Timber.e(e)
            throw e.toPurchasesException()
        }
    }

    override fun getCustomerInfoFlow(): Flow<CustomerInfo?> {
        return dataStore.getCustomerInfo()
            .catch { error ->
                Timber.e(error)
                emit(null)
            }
    }

    override suspend fun getProducts(productIds: List<String>): List<Product> {
        return try {
            purchases
                .awaitGetProducts(productIds)
                .map { storeProduct -> storeProduct.toProduct() }
        } catch (e: Throwable) {
            Timber.e(e)
            throw e.toPurchasesException()
        }
    }

    override suspend fun purchase(activity: Activity, productId: String): Purchase {
        return try {
            val storeProduct = purchases.awaitGetProducts(listOf(productId)).first()

            val result = purchases.awaitPurchase(
                PurchaseParams.Builder(activity, storeProduct)
                    .build()
            )

            Purchase(
                product = storeProduct.toProduct(),
                purchaseToken = result.storeTransaction.purchaseToken,
                subscriptionOptionId = result.storeTransaction.subscriptionOptionId,
            )
        } catch (e: Throwable) {
            Timber.e(e)
            throw e.toPurchasesException()
        }
    }

    override suspend fun restorePurchases(): CustomerInfo {
        return try {
            purchases.awaitRestore().toInternalModel()
        } catch (e: Throwable) {
            Timber.e(e)
            throw e.toPurchasesException()
        }
    }

    private fun onCustomerUpdated(customerInfo: RcCustomerInfo) {
        Timber.d("Customer info updated: $customerInfo.")

        coroutineScope.launch {
            dataStore.setCustomerInfo(customerInfo = customerInfo.toInternalModel())
        }
    }

    private suspend fun syncPurchases(): CustomerInfo? {
        if (dataStore.isPurchasesSynced()) {
            Timber.d("All purchases already synced.")
            return null
        }

        return try {
            val customerInfo = purchases.awaitSyncPurchases().toInternalModel()
            dataStore.setPurchasesSynced(true)
            dataStore.setCustomerInfo(customerInfo)
            Timber.d("All purchases were successfully synced.")
            customerInfo
        } catch (e: Throwable) {
            Timber.e("Failed to sync purchases. Error %s.", e)
            null
        }
    }

    private fun StoreProduct.toProduct(): Product {
        return Product(
            id = id.split(":").firstOrNull() ?: id,
            type = when (type) {
                SUBS -> ProductType.SUBS
                INAPP -> ProductType.INAPP
                UNKNOWN -> throw PurchasesException(UnknownError)
            },
            price = price.toInternalModel(),
            period = period?.toInternalModel(),
            subscriptionOptions = subscriptionOptions?.map { subscriptionOption ->
                SubscriptionOption(
                    id = subscriptionOption.id,
                    pricingPhases = subscriptionOption.pricingPhases.map {
                        PricingPhase(
                            price = it.price.toInternalModel(),
                            period = it.billingPeriod.toInternalModel()
                        )
                    }
                )
            }?.let(::SubscriptionOptions)
        )
    }

    private fun Throwable.toPurchasesException(): PurchasesException {
        if (this !is com.revenuecat.purchases.PurchasesException) {
            return PurchasesException(UnknownError)
        }

        return PurchasesException(
            when (this.code) {
                PurchasesErrorCode.PurchaseCancelledError -> PurchaseCancelledError
                PurchasesErrorCode.ProductAlreadyPurchasedError -> ProductAlreadyPurchasedError
                PurchasesErrorCode.NetworkError -> NetworkError
                else -> UnknownError
            }
        )
    }

    private fun RcPrice.toInternalModel() = Price(
        formatted = formatted,
        amountMicros = amountMicros,
        currencyCode = currencyCode
    )

    private fun RcPeriod.toInternalModel() = Period(
        value = value,
        unit = when (unit) {
            RcPeriod.Unit.DAY -> Period.Unit.DAY
            RcPeriod.Unit.WEEK -> Period.Unit.WEEK
            RcPeriod.Unit.MONTH -> Period.Unit.MONTH
            RcPeriod.Unit.YEAR -> Period.Unit.YEAR
            else -> Period.Unit.UNKNOWN
        }
    )

    private fun RcCustomerInfo.toInternalModel(): CustomerInfo {
        return CustomerInfo(
            activeSubscriptions = activeSubscriptions.map { it.split(":").first() }.toSet(),
            allPurchasedProductIds = allPurchasedProductIds.map { it.split(":").first() }.toSet(),
        )
    }
}