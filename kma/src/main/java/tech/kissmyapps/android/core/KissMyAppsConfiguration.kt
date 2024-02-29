package tech.kissmyapps.android.core

import android.content.Context
import tech.kissmyapps.android.analytics.facebook.FacebookConfiguration
import tech.kissmyapps.android.config.model.RemoteConfigDefaults

class KissMyAppsConfiguration(
    val context: Context,
    val appsFlyerDevKey: String,
    val amplitudeApiKey: String,
    val revenueCatApiKey: String,
    val attributionApiKey: String,
    val facebookConfiguration: FacebookConfiguration,
    val remoteConfigDefaults: RemoteConfigDefaults,
)