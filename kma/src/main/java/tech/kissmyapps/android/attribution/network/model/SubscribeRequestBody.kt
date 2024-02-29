package tech.kissmyapps.android.attribution.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class SubscribeRequestBody(
    @Json(name = "adid")
    val userId: String,

    @Json(name = "userId")
    val uuid: String,

    @Json(name = "productId")
    val productId: String,

    @Json(name = "purchaseId")
    val purchaseId: String?
)