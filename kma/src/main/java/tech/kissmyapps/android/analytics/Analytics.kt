package tech.kissmyapps.android.analytics

interface Analytics : EventLogger {
    fun setUserProperties(properties: Map<String, Any>?)
}