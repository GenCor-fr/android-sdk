package tech.kissmyapps.android.attribution.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.create
import retrofit2.http.Body
import retrofit2.http.POST
import tech.kissmyapps.android.attribution.network.model.InAppRequestBody
import tech.kissmyapps.android.attribution.network.model.InstallApplicationRequestBody
import tech.kissmyapps.android.attribution.network.model.InstallApplicationResponse
import tech.kissmyapps.android.attribution.network.model.PurchaseResponse
import tech.kissmyapps.android.attribution.network.model.SubscribeRequestBody
import java.util.concurrent.TimeUnit

internal interface AttributionService {
    @POST("/install-application")
    suspend fun sendInstall(@Body body: InstallApplicationRequestBody): InstallApplicationResponse?

    @POST("/inapp")
    suspend fun sendInAppPurchase(@Body body: InAppRequestBody): PurchaseResponse?

    @POST("/subscribe")
    suspend fun sendSubscriptionPurchase(@Body body: SubscribeRequestBody): PurchaseResponse?

    companion object Factory {
        fun create(apiKey: String, isLoggingEnabled: Boolean): AttributionService {
            val httpLoggingInterceptor = HttpLoggingInterceptor()
                .setLevel(
                    if (isLoggingEnabled) {
                        HttpLoggingInterceptor.Level.BODY
                    } else {
                        HttpLoggingInterceptor.Level.NONE
                    }
                )

            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(httpLoggingInterceptor)
                .addInterceptor(ApiKeyInterceptor(apiKey))
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .build()

            val moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()

            val converterFactory = MoshiConverterFactory.create(moshi)

            return Retrofit.Builder()
                .baseUrl("https://subscriptions.apitlm.com/")
                .client(okHttpClient)
                .addConverterFactory(converterFactory)
                .build()
                .create()
        }
    }
}