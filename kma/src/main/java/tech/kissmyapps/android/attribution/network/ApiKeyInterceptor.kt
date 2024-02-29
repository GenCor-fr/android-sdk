package tech.kissmyapps.android.attribution.network

import okhttp3.Interceptor
import okhttp3.Response

internal class ApiKeyInterceptor(private val apiKey: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
            .newBuilder()
            .addHeader("Authorization", apiKey)
            .addHeader("Platform", "android")
            .build()

        return chain.proceed(request)
    }
}