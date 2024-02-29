package tech.kissmyapps.android.core.model

import tech.kissmyapps.android.common.mapOfNotNull
import java.net.URLDecoder

internal data class AttributionData internal constructor(
    val mediaSource: String? = null,
    val campaign: String? = null,
    val ad: String? = null,
    val adGroup: String? = null,
    val deepLinkValue: String? = null,
) {
    fun toMap() = mapOfNotNull(
        "network" to mediaSource,
        "campaignName" to campaign,
        "adGroupName" to adGroup,
        "ad" to ad,
        "deep_link_value" to deepLinkValue
    )

    companion object {
        fun fromConversionData(conversionData: Map<String, Any?>): AttributionData {
            val mediaSource = conversionData["media_source"] as? String
            val campaign = conversionData["campaign"] as? String
            var ad = conversionData["af_ad"] as? String
            if (ad.isNullOrBlank()) {
                ad = conversionData["adgroup"] as? String
            }

            val adGroup = conversionData["af_adset"] as? String
            val deepLinkValue = (conversionData["deep_link_value"]
                ?: conversionData["af_dp"]) as? String

            return AttributionData(
                mediaSource = mediaSource,
                campaign = campaign,
                adGroup = adGroup,
                ad = ad,
                deepLinkValue = deepLinkValue,
            )
        }

        fun fromInstallReferrer(installReferrer: String?): AttributionData {
            if (installReferrer.isNullOrBlank()) {
                return AttributionData()
            }

            val referrer = try {
                URLDecoder.decode(installReferrer.trim(), "UTF-8")
            } catch (e: Throwable) {
                installReferrer
            }

            val data = referrer.split("&").associate {
                val (key, value) = it.split("=")
                key to value
            }

            // example: 'google-play', 'apps.facebook.com'
            var mediaSource = data["utm_source"]

            if (data["utm_medium"] == "organic") {
                mediaSource = null
            } else if (mediaSource?.contains("apps.facebook.com") == true) {
                mediaSource = "Facebook Ads"
            } else if (mediaSource.isNullOrBlank()) {
                mediaSource = data["pid"]
            }

            val campaign = data["c"]
            val deepLinkValue = data["deep_link_value"]
            val ad = data["af_ad"]
            val adSet = data["af_adset"]

            return AttributionData(
                mediaSource = mediaSource,
                ad = ad,
                campaign = campaign,
                deepLinkValue = deepLinkValue,
                adGroup = adSet
            )
        }
    }
}