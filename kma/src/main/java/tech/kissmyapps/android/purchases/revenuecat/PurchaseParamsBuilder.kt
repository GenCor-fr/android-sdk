package tech.kissmyapps.android.purchases.revenuecat

class PurchaseConfigBuilder {
    class Builder(private val productId: String) {
        private var subscriptionPlan: SubscriptionPlan? = null

        fun setSubscriptionPlan(subscriptionPlan: SubscriptionPlan?) =
            apply { this.subscriptionPlan = subscriptionPlan }

        fun build(): PurchaseConfig {
            return PurchaseConfig(productId, subscriptionPlan)
        }
    }
}

data class PurchaseConfig(
    val productId: String,
    val subscriptionPlan: SubscriptionPlan?
)