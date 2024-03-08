package tech.kissmyapps.android.analytics.amplitude

import android.content.Context
import com.amplitude.android.Amplitude
import com.amplitude.core.events.Identify
import tech.kissmyapps.android.analytics.Analytics
import tech.kissmyapps.android.analytics.AnalyticsEvent
import tech.kissmyapps.android.core.AnalyticsEvents
import tech.kissmyapps.android.core.AnalyticsProperties
import tech.kissmyapps.android.purchases.model.Purchase
import timber.log.Timber
import java.util.Calendar

internal class AmplitudeAnalytics(
    context: Context,
    apiKey: String
) : Analytics {
    private val amplitude = Amplitude(apiKey, context) {
        defaultTracking.sessions = true
        minTimeBetweenSessionsMillis = 0
    }

    internal fun setUserId(userId: String) {
        amplitude.setUserId(userId)
    }

    override fun logEvent(event: String, properties: Map<String, Any?>?) {
        amplitude.track(event, properties)
        Timber.d("Log event[$event] with properties[$properties].")
    }

    override fun logEvent(event: AnalyticsEvent) {
        logEvent(event.type, event.properties)
    }

    internal fun logPurchase(purchase: Purchase) {
        if (purchase.price.amountMicros == 0L) {
            logEvent(AnalyticsEvents.TRIAL_STARTED)
        } else {
            logEvent(
                event = AnalyticsEvents.PURCHASE,
                properties = mapOf(
                    "productId" to purchase.product.id,
                    "price" to purchase.price.amount,
                    "currency" to purchase.price.currencyCode,
                )
            )
        }
    }

    fun flush() {
        amplitude.flush()
    }

    override fun setUserProperties(properties: Map<String, Any>?) {
        amplitude.identify(properties)
    }

    internal fun sendCohort() {
        val calendar = Calendar.getInstance()

        val identify = Identify()
            .setOnce(AnalyticsProperties.COHORT_YEAR, calendar[Calendar.YEAR])
            .setOnce(AnalyticsProperties.COHORT_MONTH, calendar[Calendar.MONTH] + 1)
            .setOnce(AnalyticsProperties.COHORT_WEEK, calendar[Calendar.WEEK_OF_YEAR])

        amplitude.identify(identify)
    }
}