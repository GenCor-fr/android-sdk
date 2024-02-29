package tech.kissmyapps.android.analytics.af

import android.content.Context
import com.appsflyer.AFInAppEventType
import com.appsflyer.AppsFlyerConversionListener
import com.appsflyer.AppsFlyerLib
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import tech.kissmyapps.android.analytics.AnalyticsEvent
import tech.kissmyapps.android.analytics.EventLogger
import tech.kissmyapps.android.purchases.logger.PurchaseEventLogger
import tech.kissmyapps.android.purchases.model.Purchase
import timber.log.Timber

class AppsFlyerAnalytics(
    devKey: String,
    private val applicationContext: Context
) : EventLogger, PurchaseEventLogger {
    private val appsFlyer = AppsFlyerLib.getInstance()

    private val conversionDataFlow = MutableStateFlow<Map<String, Any?>?>(null)

    internal var appsFlyerUID: String? = null
        private set
        get() {
            if (field == null) {
                field = appsFlyer.getAppsFlyerUID(applicationContext)
            }

            return field
        }

    init {
        appsFlyer.init(
            devKey,
            object : AppsFlyerConversionListener {
                override fun onConversionDataSuccess(conversionData: MutableMap<String, Any>?) {
                    Timber.d("onConversionDataSuccess: $conversionData.")
                    conversionDataFlow.value = conversionData
                    appsFlyer.unregisterConversionListener()
                }

                override fun onConversionDataFail(errorMessage: String?) {
                    Timber.d("onConversionDataFail: $errorMessage.")
                    conversionDataFlow.value = emptyMap()
                    appsFlyer.unregisterConversionListener()
                }

                override fun onAppOpenAttribution(attributionData: MutableMap<String, String>?) {
                    Timber.d("onAppOpenAttribution: $attributionData.")
                }

                override fun onAttributionFailure(errorMessage: String?) {
                    Timber.d("onAttributionFailure: $errorMessage.")
                }
            },
            applicationContext
        )

        appsFlyer.start(applicationContext)
    }

    override fun logEvent(event: String, properties: Map<String, Any?>?) {
        appsFlyer.logEvent(applicationContext, event, properties)
    }

    override fun logEvent(event: AnalyticsEvent) {
        logEvent(event.type, event.properties)
    }

    override fun logPurchase(purchase: Purchase) {
        if (purchase.price.amountMicros == 0L) {
            logEvent(AFInAppEventType.START_TRIAL)
        } else {
            logEvent(AFInAppEventType.PURCHASE, mapOf("productId" to purchase.product.id))
        }
    }

    internal suspend fun awaitConversionData(): Map<String, Any?>? {
        return conversionDataFlow
            .filterNotNull()
            .firstOrNull()
    }
}