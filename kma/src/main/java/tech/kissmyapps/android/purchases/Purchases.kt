package tech.kissmyapps.android.purchases

import android.app.Activity
import kotlinx.coroutines.flow.Flow
import tech.kissmyapps.android.purchases.model.CustomerInfo
import tech.kissmyapps.android.purchases.model.Product
import tech.kissmyapps.android.purchases.model.Purchase

interface Purchases {
    /**
     * Get latest available customer info.
     *
     * @throws [PurchasesException] with a [PurchasesError] if there's an error retrieving the customer info.
     * @return The [CustomerInfo] associated to the current user.
     */
    @Throws(PurchasesException::class)
    suspend fun getCustomerInfo(): CustomerInfo

    /**
     * Get latest available cached customer info.
     *
     * @return The [Flow] with [CustomerInfo] associated to the current user.
     */
    fun getCustomerInfoFlow(): Flow<CustomerInfo?>

    /**
     * Gets the product(s) for the given list of product ids.
     *
     * @param [productIds] List of productIds
     *
     * @return A list of [Product] with the products that have been able to be fetched from the store successfully.
     * Not found products will be ignored.
     */
    @Throws(PurchasesException::class)
    suspend fun getProducts(productIds: List<String>): List<Product>

    @Throws(PurchasesException::class)
    suspend fun purchase(activity: Activity, productId: String): Purchase

    @Throws(PurchasesException::class)
    suspend fun restorePurchases(): CustomerInfo
}