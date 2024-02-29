package tech.kissmyapps.android.purchases.model

/**
 * Encapsulates how a user pays for a subscription at a given point in time.
 */
data class PricingPhase(
    /**
     * [Price] of the [PricingPhase]
     */
    val price: Price,

    /**
     * Billing period for which the [PricingPhase] applies.
     */
    val period: Period,
)