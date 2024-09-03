package tech.kissmyapps.android.analytics.firebase

import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import tech.kissmyapps.android.analytics.AnalyticsEvent
import tech.kissmyapps.android.analytics.EventLogger
import tech.kissmyapps.android.common.toBundle

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
}