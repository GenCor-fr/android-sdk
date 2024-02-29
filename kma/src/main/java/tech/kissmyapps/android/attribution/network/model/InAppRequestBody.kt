package tech.kissmyapps.android.attribution.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Request body of 'POST /inapp'.
 *
 * @param userId UUID from installed application or third-party.
 * @param uuid Advertising ID.
 * @param productId product id.
 * @param purchaseId purchase token.
 * @param paymentDetails price and currency.
 */
@JsonClass(generateAdapter = true)
internal data class InAppRequestBody(
    @Json(name = "userId")
    val uuid: String,

    @Json(name = "adid")
    val userId: String,

    @Json(name = "productId")
    val productId: String,

    @Json(name = "purchaseId")
    val purchaseId: String,

    @Json(name = "paymentDetails")
    val paymentDetails: PaymentDetails
) {
    /**
     * @param price product price.
     * @param currency code in ISO 4217 format.
     */
    @JsonClass(generateAdapter = true)
    data class PaymentDetails(
        @Json(name = "price")
        val price: Double,

        @Json(name = "currency")
        val currency: String
    )
}
