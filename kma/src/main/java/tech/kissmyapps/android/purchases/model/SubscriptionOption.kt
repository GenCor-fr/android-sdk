package tech.kissmyapps.android.purchases.model

/**
 * A purchase-able entity for a subscription product.
 */
data class SubscriptionOption(
    val id: String,
    val pricingPhases: List<PricingPhase>,
) {
    /**
     * True if this SubscriptionOption represents a subscription base plan (rather than an offer).
     */
    val isBasePlan: Boolean
        get() = pricingPhases.size == 1

    /**
     * The free trial [PricingPhase] of the subscription.
     * Looks for the first pricing phase of the SubscriptionOption where `amountMicros` is 0.
     * There can be a `freeTrialPhase` and an `introductoryPhase` in the same [SubscriptionOption].
     */
    val freePhase: PricingPhase?
        get() = pricingPhases.dropLast(1).firstOrNull {
            it.price.amountMicros == 0L
        }

    /**
     * The full price [PricingPhase] of the subscription.
     * Looks for the last price phase of the SubscriptionOption.
     */
    val fullPricePhase: PricingPhase?
        get() = pricingPhases.lastOrNull()
}