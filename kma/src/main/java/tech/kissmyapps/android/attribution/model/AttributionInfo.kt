package tech.kissmyapps.android.attribution.model

internal data class AttributionInfo(
    val userId: String,
    val uuid: String,
    val network: String?,
    val networkType: String?,
    val networkSubtype: String?,
    val campaignName: String?,
    val campaignType: String?,
    val adGroupName: String?,
    val creativeName: String?,
    val attributed: Boolean
)