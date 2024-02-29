package tech.kissmyapps.android.attribution.model

data class InstallData(
    val uuid: String,
    val userId: String,
    val network: String?,
    val adGroup: String?,
    val networkType: String?,
    val networkSubtype: String?,
    val campaignName: String?,
    val campaignType: String?,
    val adGroupName: String?,
    val creativeName: String?,
    val attributed: Boolean?
)