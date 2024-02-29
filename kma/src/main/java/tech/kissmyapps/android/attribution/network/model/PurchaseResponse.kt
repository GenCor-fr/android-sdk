package tech.kissmyapps.android.attribution.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class PurchaseResponse(
    @Json(name = "isActive")
    val isActive: Boolean?,

    @Json(name = "environment")
    val environment: String?
)