package tech.kissmyapps.android.config.model

import tech.kissmyapps.android.config.RemoteConfig
import tech.kissmyapps.android.config.model.RemoteConfigParams.AB_PAYWALL_FACEBOOK
import tech.kissmyapps.android.config.model.RemoteConfigParams.AB_PAYWALL_GENERAL
import tech.kissmyapps.android.config.model.RemoteConfigParams.AB_PAYWALL_GOOGLE_REDIRECT
import tech.kissmyapps.android.config.model.RemoteConfigParams.MIN_SUPPORTED_APP_VERSION
import tech.kissmyapps.android.config.model.RemoteConfigParams.RATE_US_PRIMARY_SHOWN
import tech.kissmyapps.android.config.model.RemoteConfigParams.RATE_US_SECONDARY_SHOWN
import tech.kissmyapps.android.config.model.RemoteConfigParams.SUBS_SCREEN_STYLE_FULL
import tech.kissmyapps.android.config.model.RemoteConfigParams.SUBS_SCREEN_STYLE_HARD
import tech.kissmyapps.android.core.model.MediaSource

class RemoteConfigDefaults {
    internal val values = mutableMapOf<String, RemoteConfigDefault>()

    init {
        setSubsScreenStyleFull(true)
        setSubsScreenStyleHard(false)
        setShowPrimaryRateUs(false)
        setShowSecondaryRateUs(false)
        setShowSecondaryRateUs(false)
        setGeneralPaywall("")
        setFacebookPaywall("")
        setGoogleRedirectPaywall("")
        setMinimalSupportedAppVersion(0)
    }

    fun setSubsScreenStyleFull(value: Boolean) = param(SUBS_SCREEN_STYLE_FULL, value)

    fun setSubsScreenStyleHard(value: Boolean) = param(SUBS_SCREEN_STYLE_HARD, value)

    fun setShowPrimaryRateUs(value: Boolean) = param(RATE_US_PRIMARY_SHOWN, value)

    fun setShowSecondaryRateUs(value: Boolean) = param(RATE_US_SECONDARY_SHOWN, value)

    fun setGeneralPaywall(value: String) = param(
        AB_PAYWALL_GENERAL,
        value,
        MediaSource.all()
    )

    fun setFacebookPaywall(value: String) = param(
        AB_PAYWALL_FACEBOOK,
        value,
        setOf(MediaSource.FACEBOOK_ADS)
    )

    fun setGoogleRedirectPaywall(value: String) = param(
        AB_PAYWALL_GOOGLE_REDIRECT,
        value,
        setOf(MediaSource.GOOGLE)
    )

    fun setMinimalSupportedAppVersion(value: Int) = param(MIN_SUPPORTED_APP_VERSION, value.toLong())

    fun param(
        key: String,
        value: String? = null,
        sources: Set<MediaSource>? = null
    ) = putParam(key, value, sources)

    fun param(
        key: String,
        value: Long,
        sources: Set<MediaSource>? = null
    ) = putParam(key, value, sources)

    fun param(
        key: String,
        value: Double,
        sources: Set<MediaSource>? = null
    ) = putParam(key, value, sources)

    fun param(
        key: String,
        value: Boolean,
        sources: Set<MediaSource>? = null
    ) = putParam(key, value, sources)

    private fun putParam(
        key: String,
        value: Any?,
        sources: Set<MediaSource>? = null
    ): RemoteConfigDefaults {
        val defaultValue = if (sources.isNullOrEmpty()) {
            value?.toString() ?: RemoteConfig.NONE_VALUE
        } else {
            "none_$value"
        }

        values[key] = RemoteConfigDefault(key, defaultValue, sources)

        return this
    }
}

internal class RemoteConfigDefault(
    val key: String,
    val defaultValue: String,
    val activeSources: Set<MediaSource>? = null
) {
    constructor(
        key: String,
        default: String,
        vararg sources: MediaSource
    ) : this(key, default, sources.toSet())

    fun getValue(remoteValue: String, source: MediaSource): String {
        val value = if (activeSources.isNullOrEmpty() || activeSources.contains(source)) {
            remoteValue
        } else {
            defaultValue
        }

        if (value == "none") {
            return ""
        }

        if (value.startsWith("none_")) {
            return value.drop(5)
        }

        return value
    }
}