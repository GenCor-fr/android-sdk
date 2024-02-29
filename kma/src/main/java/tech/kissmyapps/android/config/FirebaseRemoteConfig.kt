package tech.kissmyapps.android.config

import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfigValue
import com.google.firebase.remoteconfig.get
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import kotlinx.coroutines.tasks.await
import tech.kissmyapps.android.config.model.RemoteConfigDefaults
import tech.kissmyapps.android.config.model.RemoteConfigParams
import tech.kissmyapps.android.config.model.RemoteConfigValue
import tech.kissmyapps.android.config.model.RemoteConfigValueImpl
import tech.kissmyapps.android.core.model.MediaSource
import timber.log.Timber

internal class FirebaseRemoteConfig(
    private val defaults: RemoteConfigDefaults
) : RemoteConfig {
    private val remoteConfig = Firebase.remoteConfig

    private var mediaSource: MediaSource? = null

    internal suspend fun fetchAndActivate(): Boolean {
        return try {
            remoteConfig.setConfigSettingsAsync(
                remoteConfigSettings {
                    minimumFetchIntervalInSeconds = 1
                }
            ).await()

            remoteConfig.setDefaultsAsync(defaults.values.mapValues { it.value.defaultValue })
                .await()

            remoteConfig.fetchAndActivate().await()
        } catch (e: Exception) {
            Timber.e(e)
            false
        }
    }

    fun setMediaSource(mediaSource: MediaSource) {
        this.mediaSource = mediaSource
    }

    override operator fun get(key: String): RemoteConfigValue {
        return get(key, remoteConfig[key])
    }

    override fun getLong(key: String) = get(key).asLong()

    override fun getDouble(key: String) = get(key).asDouble()

    override fun getBoolean(key: String) = get(key).asBoolean()

    override fun getString(key: String) = get(key).asString()

    override fun getAll(): Map<String, RemoteConfigValue> {
        return remoteConfig.all.mapValues { get(it.key, it.value) }
    }

    override fun getActivePaywallName(): String {
        return getString(
            when (mediaSource) {
                MediaSource.GOOGLE -> RemoteConfigParams.AB_PAYWALL_GOOGLE_REDIRECT
                MediaSource.FACEBOOK_ADS -> RemoteConfigParams.AB_PAYWALL_FACEBOOK
                else -> RemoteConfigParams.AB_PAYWALL_GENERAL
            }
        )
    }

    override fun isSubscriptionStyleFull() = getBoolean(RemoteConfigParams.SUBS_SCREEN_STYLE_FULL)

    override fun isSubscriptionStyleHard() = getBoolean(RemoteConfigParams.SUBS_SCREEN_STYLE_HARD)

    override fun rateUsPrimaryShow() = getBoolean(RemoteConfigParams.RATE_US_PRIMARY_SHOWN)

    override fun rateUsSecondaryShow() = getBoolean(RemoteConfigParams.RATE_US_SECONDARY_SHOWN)

    private fun get(key: String, value: FirebaseRemoteConfigValue): RemoteConfigValue {
        return RemoteConfigValueImpl.from(
            key = key,
            value = value,
            mediaSource = mediaSource ?: MediaSource.ORGANIC,
            default = defaults.values[key]
        )
    }
}