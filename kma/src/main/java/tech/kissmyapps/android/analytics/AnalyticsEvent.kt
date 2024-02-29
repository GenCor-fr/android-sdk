package tech.kissmyapps.android.analytics

interface AnalyticsEvent {
    val type: String
    val properties: Map<String, Any?>?
}