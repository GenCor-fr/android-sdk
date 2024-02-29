package tech.kissmyapps.android.purchases.model

/**
 * @property activeSubscriptions active subscription productIds.
 * @property allPurchasedProductIds purchased productIds, active and inactive.
 */
data class CustomerInfo(
    val activeSubscriptions: Set<String>,
    val allPurchasedProductIds: Set<String>
)