package tech.kissmyapps.android.attribution.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class InstallApplicationResponse(
    @Json(name = "uuid")
    val uuid: String?,

    @Json(name = "network")
    val network: String?,

    @Json(name = "networkType")
    val networkType: String?,

    @Json(name = "networkSubtype")
    val networkSubtype: String?,

    @Json(name = "campaignName")
    val campaignName: String?,

    @Json(name = "campaignType")
    val campaignType: String?,

    @Json(name = "adGroupName")
    val adGroupName: String?,

    @Json(name = "creativeName")
    val creativeName: String?,

    @Json(name = "attributed")
    val attributed: Boolean?
)
