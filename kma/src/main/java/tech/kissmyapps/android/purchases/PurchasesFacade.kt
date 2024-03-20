package tech.kissmyapps.android.purchases

import android.app.Activity
import com.appsflyer.AFInAppEventType.INITIATED_CHECKOUT
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import tech.kissmyapps.android.analytics.af.AppsFlyerAnalytics
import tech.kissmyapps.android.analytics.amplitude.AmplitudeAnalytics
import tech.kissmyapps.android.analytics.facebook.FacebookAnalytics
import tech.kissmyapps.android.analytics.firebase.FirebaseAnalytics
import tech.kissmyapps.android.core.AnalyticsEvents.PURCHASE_ERROR
import tech.kissmyapps.android.core.AnalyticsProperties.ACTIVE_SUBS
import tech.kissmyapps.android.core.AnalyticsProperties.ALL_PURCHASED_PRODUCT_IDS
import tech.kissmyapps.android.purchases.logger.TLMPurchaseEventLogger
import tech.kissmyapps.android.purchases.model.CustomerInfo
import tech.kissmyapps.android.purchases.model.Purchase

internal class PurchasesFacade(
    private val purchases: Purchases,
    private val appsFlyerAnalytics: AppsFlyerAnalytics,
    private val firebaseAnalytics: FirebaseAnalytics,
    private val amplitudeAnalytics: AmplitudeAnalytics,
    private val facebookAnalytics: FacebookAnalytics,
    private val tlmPurchaseLogger: TLMPurchaseEventLogger,
    coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : Purchases by purchases {
    init {
        purchases.getCustomerInfoFlow()
            .onEach(::onCustomerInfoUpdated)
            .launchIn(coroutineScope)
    }

    override suspend fun purchase(
        activity: Activity,
        productId: String
    ): Purchase {
        appsFlyerAnalytics.logEvent(INITIATED_CHECKOUT)

        return try {
            val purchase = purchases.purchase(activity, productId)

            tlmPurchaseLogger.logPurchase(purchase)
            appsFlyerAnalytics.logPurchase(purchase)
            amplitudeAnalytics.logPurchase(purchase)
            firebaseAnalytics.logPurchase(purchase)

            purchase
        } catch (exception: PurchasesException) {
            if (exception.error != PurchasesError.PurchaseCancelledError) {
                amplitudeAnalytics.logEvent(PURCHASE_ERROR)
            }

            throw exception
        }
    }

    private fun onCustomerInfoUpdated(customerInfo: CustomerInfo?) {
        if (customerInfo == null) {
            return
        }

        amplitudeAnalytics.setUserProperties(
            mapOf(
                ACTIVE_SUBS to customerInfo.activeSubscriptions.toTypedArray(),
                ALL_PURCHASED_PRODUCT_IDS to customerInfo.allPurchasedProductIds.toTypedArray(),
            )
        )
    }
}