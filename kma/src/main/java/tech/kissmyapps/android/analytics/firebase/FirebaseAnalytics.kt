package tech.kissmyapps.android.analytics.firebase

import com.google.firebase.abt.FirebaseABTesting
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import tech.kissmyapps.android.analytics.AnalyticsEvent
import tech.kissmyapps.android.analytics.EventLogger
import tech.kissmyapps.android.common.toBundle
import tech.kissmyapps.android.core.AnalyticsEvents
import tech.kissmyapps.android.purchases.model.Purchase

class FirebaseAnalytics : EventLogger {
    private val analytics = Firebase.analytics

    internal fun setUserId(userId: String) {
        analytics.setUserId(userId)
    }

    override fun logEvent(event: String, properties: Map<String, Any?>?) {
        analytics.logEvent(event, properties?.toBundle())
    }

    override fun logEvent(event: AnalyticsEvent) {
        logEvent(event.type, event.properties)
    }

    internal fun logPurchase(purchase: Purchase) {
        if (purchase.price.amountMicros == 0L) {
            logEvent(AnalyticsEvents.TRIAL_STARTED)
        } else {
            logEvent(
                event = "in_app_purchased",
                properties = mapOf(
                    "productId" to purchase.product.id,
                    "purchaseId" to purchase.purchaseToken,
                    "price" to purchase.price.amount,
                    "currency" to purchase.price.currencyCode
                )
            )
        }
    }
}