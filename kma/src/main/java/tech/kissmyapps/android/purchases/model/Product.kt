package tech.kissmyapps.android.purchases.model

import com.revenuecat.purchases.models.SubscriptionOption

/**
 * Represents an in-app product's or subscription's listing details.
 */
data class Product(
    /**
     * The product ID.
     */
    val id: String,

    /**
     * Price information for a non-subscription product.
     * Base plan price for a subscription.
     * For subscriptions, use SubscriptionOption's pricing phases for offer pricing.
     */
    val price: Price,

    /**
     * Subscription period.
     *
     * Note: Returned only for Google subscriptions. Null for INAPP products.
     */
    val period: Period?,

    /**
     * Type of product. One of [ProductType].
     */
    val type: ProductType,

    /**
     * Contains all [SubscriptionOption]s. Null for INAPP products.
     */
    val subscriptionOptions: SubscriptionOptions?
)