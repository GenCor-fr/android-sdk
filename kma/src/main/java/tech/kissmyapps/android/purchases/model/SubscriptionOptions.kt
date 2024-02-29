package tech.kissmyapps.android.purchases.model

import com.revenuecat.purchases.models.PricingPhase

class SubscriptionOptions(
    private val subscriptionOptions: List<SubscriptionOption>
) : List<SubscriptionOption> by subscriptionOptions {
    /**
     * The base plan [SubscriptionOption].
     */
    val basePlan: SubscriptionOption?
        get() = this.firstOrNull { it.isBasePlan }

    /**
     * The first [SubscriptionOption] with a free trial [PricingPhase].
     */
    val freeTrial: SubscriptionOption?
        get() = this.firstOrNull { it.freePhase != null }
}