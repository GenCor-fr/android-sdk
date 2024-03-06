package tech.kissmyapps.android

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import org.junit.Test
import tech.kissmyapps.android.config.model.RemoteConfigDefault
import tech.kissmyapps.android.config.model.RemoteConfigDefaults
import tech.kissmyapps.android.config.model.RemoteConfigParams
import tech.kissmyapps.android.config.model.RemoteConfigValueImpl
import tech.kissmyapps.android.core.model.MediaSource

class RemoteConfigDefaults {
    @Test
    fun test_remote_config_default() {
        val defaults = RemoteConfigDefaults()
            .setGeneralPaywall("3vertical_boxes")
            .setFacebookPaywall("3vertical_boxes")
            .setGoogleRedirectPaywall("3vertical_boxes")
            .setSubsScreenStyleFull(true)
            .setMinimalSupportedAppVersion(71)
            .param(key = "ab_paywall_soft", sources = MediaSource.all())

        val abPaywallGeneral = defaults.values[RemoteConfigParams.AB_PAYWALL_GENERAL]?.defaultValue

        assert(abPaywallGeneral == "none_3vertical_boxes")

        val organicResult = defaults.values["android_ab_paywall_general"]!!.getValue(
            "none_3vertical_boxes",
            MediaSource.ORGANIC
        )

        assert(organicResult == "3vertical_boxes")

        val fbResult = defaults.values["android_ab_paywall_facebook"]!!.getValue(
            "fb_3vertical_boxes",
            MediaSource.FACEBOOK_ADS
        )

        assert(fbResult == "fb_3vertical_boxes")

        val paywallHard = defaults.values["android_subscription_screen_style_hard"]
        assert(paywallHard?.getValue("true", MediaSource.ORGANIC) == "true")
    }

    @Test
    fun test_paywall_hard_value() {
        val defaults = RemoteConfigDefaults()
            .setGeneralPaywall("3vertical_boxes")
            .setFacebookPaywall("3vertical_boxes")
            .setGoogleRedirectPaywall("3vertical_boxes")
            .setSubsScreenStyleHard(false)
            .setMinimalSupportedAppVersion(71)
            .param(key = "ab_paywall_soft", sources = MediaSource.all())

        val default = defaults.values["android_subscription_screen_style_hard"]

        val remoteConfigValue = RemoteConfigValueImpl.from(
            key = "android_subscription_screen_style_hard",
            value = "true",
            source = FirebaseRemoteConfig.VALUE_SOURCE_DEFAULT,
            mediaSource = MediaSource.ORGANIC,
            default = default
        )

        val value = remoteConfigValue.asBoolean()
        assert(value)
    }

    @Test
    fun test_abt_config_default() {
        val default = RemoteConfigDefault(
            "ab_paywall_facebook",
            "none_3vertical_boxes",
            MediaSource.FACEBOOK_ADS
        )

        val organicResult = default.getValue("fb_paywall", MediaSource.ORGANIC)
        assert(organicResult == "3vertical_boxes")

        val fbResult = default.getValue("fb_paywall", MediaSource.FACEBOOK_ADS)
        assert(fbResult == "fb_paywall")
    }

    @Test
    fun test_abt_config_value_for_fb_source() {
        val remoteConfigValue = RemoteConfigValueImpl.from(
            key = "ab_paywall_facebook",
            value = "none_3vertical_boxes",
            source = FirebaseRemoteConfig.VALUE_SOURCE_DEFAULT,
            mediaSource = MediaSource.ORGANIC,
            default = RemoteConfigDefault(
                "ab_paywall_facebook",
                "none_3vertical_boxes",
                MediaSource.FACEBOOK_ADS
            )
        )

        assert(remoteConfigValue.asString() == "3vertical_boxes")
    }

    @Test
    fun test_abt_config_raw_value_for_fb_source() {
        val remoteConfigValue = RemoteConfigValueImpl.from(
            key = "ab_paywall_facebook",
            value = "none_3vertical_boxes",
            source = FirebaseRemoteConfig.VALUE_SOURCE_DEFAULT,
            mediaSource = MediaSource.ORGANIC,
            default = RemoteConfigDefault(
                "ab_paywall_facebook",
                "none_3vertical_boxes",
                MediaSource.FACEBOOK_ADS
            )
        )

        assert(remoteConfigValue.rawValue == "none_3vertical_boxes")
    }

    @Test
    fun test_abt_all_sources_config_value() {
        val remoteConfigValue = RemoteConfigValueImpl.from(
            key = "ab_paywall_soft",
            value = "1",
            source = FirebaseRemoteConfig.VALUE_SOURCE_DEFAULT,
            mediaSource = MediaSource.FACEBOOK_ADS,
            default = RemoteConfigDefault(
                "ab_paywall_soft",
                "none",
                MediaSource.all()
            )
        )

        assert(remoteConfigValue.asBoolean())
    }

    @Test
    fun test_abt_config_value_with_none_for_all_sources() {
        val remoteConfigValue = RemoteConfigValueImpl.from(
            key = "ab_paywall_soft",
            value = "none",
            source = FirebaseRemoteConfig.VALUE_SOURCE_DEFAULT,
            mediaSource = MediaSource.FACEBOOK_ADS,
            default = RemoteConfigDefault(
                "ab_paywall_soft",
                "none",
                MediaSource.all()
            )
        )

        assert(!remoteConfigValue.asBoolean())
    }

    @Test
    fun test_abt_config_value_with_none_as_boolean_for_fb_source() {
        val remoteConfigValue = RemoteConfigValueImpl.from(
            key = "ab_paywall_soft",
            value = "1",
            source = FirebaseRemoteConfig.VALUE_SOURCE_DEFAULT,
            mediaSource = MediaSource.GOOGLE,
            default = RemoteConfigDefault(
                "ab_paywall_soft",
                "none",
                MediaSource.FACEBOOK_ADS
            )
        )

        assert(remoteConfigValue.asString() == "")
        assert(!remoteConfigValue.asBoolean())
    }

    @Test
    fun test_abt_config_value_with_none_as_string_for_fb_source() {
        val remoteConfigValue = RemoteConfigValueImpl.from(
            key = "ab_paywall_soft",
            value = "value",
            source = FirebaseRemoteConfig.VALUE_SOURCE_DEFAULT,
            mediaSource = MediaSource.GOOGLE,
            default = RemoteConfigDefault(
                "ab_paywall_soft",
                "none",
                MediaSource.FACEBOOK_ADS
            )
        )

        assert(remoteConfigValue.asString() == "")
    }
}