package tech.kissmyapps.android.purchases.revenuecat

import android.content.Context

internal data class RevenueCatConfiguration @JvmOverloads internal constructor(
    internal val context: Context,
    internal val apiKey: String,
    internal val appUserId: String? = null,
    internal val appsFlyerUID: String? = null,
    internal val amplitudeUserId: String? = null,
    internal val syncPurchases: Boolean = true,
) {
    constructor(builder: Builder) : this(
        builder.context,
        builder.apiKey,
        builder.appUserId,
        builder.appsFlyerUID,
        builder.amplitudeUserId,
        builder.syncPurchases,
    )

    class Builder(
        internal val context: Context,
        internal val apiKey: String
    ) {
        internal var appUserId: String? = null
        internal var appsFlyerUID: String? = null
        internal var amplitudeUserId: String? = null
        internal var syncPurchases: Boolean = true

        fun setAppUserId(appUserId: String?) = apply {
            this.appUserId = appUserId
        }

        fun setAppsFlyerUID(appsFlyerUID: String?) = apply {
            this.appsFlyerUID = appsFlyerUID
        }

        fun setAmplitudeUserId(amplitudeUserId: String?) = apply {
            this.amplitudeUserId = amplitudeUserId
        }

        fun build() = RevenueCatConfiguration(this)
    }
}