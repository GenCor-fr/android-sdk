package tech.kissmyapps.android.analytics.amplitude

import android.content.Context
import com.amplitude.android.Amplitude
import com.amplitude.core.events.Identify
import tech.kissmyapps.android.analytics.Analytics
import tech.kissmyapps.android.analytics.AnalyticsEvent
import tech.kissmyapps.android.core.AnalyticsProperties
import timber.log.Timber
import java.util.Calendar

internal class AmplitudeAnalytics(
    context: Context,
    apiKey: String
) : Analytics {
    private val amplitude = Amplitude(apiKey, context) {
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

    fun flush() {
        amplitude.flush()
    }

    override fun setUserProperties(properties: Map<String, Any>?) {
        amplitude.identify(properties)
    }

    internal fun sendCohort() {
        val calendar = Calendar.getInstance()

        val identify = Identify()
            .setOnce(AnalyticsProperties.COHORT_DAY, calendar[Calendar.DAY_OF_YEAR])
            .setOnce(AnalyticsProperties.COHORT_MONTH, calendar[Calendar.MONTH] + 1)
            .setOnce(AnalyticsProperties.COHORT_WEEK, calendar[Calendar.WEEK_OF_YEAR])

        amplitude.identify(identify)
    }
}