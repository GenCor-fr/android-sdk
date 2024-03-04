package tech.kissmyapps.android.sample

import android.app.Application
import tech.kissmyapps.android.KissMyAppsSdk
import tech.kissmyapps.android.analytics.facebook.FacebookConfiguration
import tech.kissmyapps.android.config.model.RemoteConfigDefaults
import tech.kissmyapps.android.core.KissMyAppsConfiguration
import tech.kissmyapps.android.core.model.MediaSource

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        KissMyAppsSdk.configure(
            KissMyAppsConfiguration(
                applicationContext,
                appsFlyerDevKey = "YOUR_APPSFLYER_DEV_KEY",
                amplitudeApiKey = "YOUR_AMPLITUDE_API_KEY",
                revenueCatApiKey = "YOUR_REVENUE_CAT_API_KEY",
                attributionApiKey = "YOUR_TLM_SUBSCRIPTION_API_KEY",
                facebookConfiguration = FacebookConfiguration(
                    applicationId = "YOUR_FB_APP_ID",
                    clientToken = "YOUR_FB_CLIENT_TOKEN"
                ),
                remoteConfigDefaults = RemoteConfigDefaults()
                    .setGeneralPaywall("YOUR_GENERAL_PAYWALL_NAME")
                    .setFacebookPaywall("YOUR_GENERAL_FACEBOOK_NAME")
                    .setGoogleRedirectPaywall("YOUR_GENERAL_GOOGLE_REDIRECT_NAME")
                    .setMinimalSupportedAppVersion(0)
                    .param(
                        key = "YOUR_CUSTOM_AB_CONFIG",
                        sources = MediaSource.all()
                    ), // custom A/B test remote config
            )
        )

    }
}