package tech.kissmyapps.android.attribution.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Request body of 'POST /install-application' endpoint.
 *
 * @param userId unique UUID (Advertising ID or AppSet ID).
 * @param isLimitAdTrackingEnabled is enabled limit ads tracking.
 * @param appsFlyerUUID AppsFlyer user ID.
 * @param appVersion application build version.
 * @param sdkVersion version of SDK (If there is no SDK version, then the application version).
 * @param osVersion OS version.
 */
@JsonClass(generateAdapter = true)
internal data class InstallApplicationRequestBody(
    @Json(name = "userId")
    val userId: String,

    @Json(name = "appVersion")
    val appVersion: String,

    @Json(name = "sdkVersion")
    val sdkVersion: String,

    @Json(name = "osVersion")
    val osVersion: String,

    @Json(name = "limitAdTracking")
    val isLimitAdTrackingEnabled: Boolean,

    @Json(name = "appsflyerId")
    val appsFlyerUUID: String?
)
