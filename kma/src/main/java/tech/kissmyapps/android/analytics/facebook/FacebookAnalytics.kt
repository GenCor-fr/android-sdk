package tech.kissmyapps.android.analytics.facebook

import android.app.Application
import android.content.Context
import androidx.core.os.bundleOf
import com.facebook.FacebookSdk
import com.facebook.LoggingBehavior
import com.facebook.appevents.AppEventsConstants
import com.facebook.appevents.AppEventsLogger
import tech.kissmyapps.android.analytics.AnalyticsEvent
import tech.kissmyapps.android.analytics.EventLogger
import tech.kissmyapps.android.common.toBundle
import tech.kissmyapps.android.purchases.model.Purchase
import java.util.Currency

class FacebookAnalytics(
    configuration: FacebookConfiguration,
    private val applicationContext: Context,
) : EventLogger {
    private val logger: AppEventsLogger

    init {
        FacebookSdk.setApplicationId(configuration.applicationId)
        FacebookSdk.setClientToken(configuration.clientToken)
        FacebookSdk.addLoggingBehavior(LoggingBehavior.APP_EVENTS)
        FacebookSdk.setAutoInitEnabled(true)
        FacebookSdk.setAdvertiserIDCollectionEnabled(true)
        FacebookSdk.sdkInitialize(applicationContext)
        FacebookSdk.fullyInitialize()
        AppEventsLogger.activateApp(applicationContext as Application, configuration.applicationId)
        logger = AppEventsLogger.newLogger(applicationContext, configuration.applicationId)
    }

    internal fun setUserId(userId: String) {
        AppEventsLogger.setUserID(userId)
    }

    override fun logEvent(event: String, properties: Map<String, Any?>?) {
        logger.logEvent(event, properties?.toBundle())
    }

    override fun logEvent(event: AnalyticsEvent) {
        logEvent(event.type, event.properties)
    }

    internal fun logPurchase(purchase: Purchase) {
        val price = purchase.product.price

        if (price.amountMicros == 0L) {
            logger.logEvent(
                AppEventsConstants.EVENT_NAME_START_TRIAL,
                bundleOf(
                    AppEventsConstants.EVENT_PARAM_CONTENT_ID to purchase.product.id,
                    AppEventsConstants.EVENT_PARAM_CURRENCY to price.currencyCode,
                    AppEventsConstants.EVENT_PARAM_NUM_ITEMS to 1
                )
            )

            return
        }

        logger.logPurchase(
            price.amountMicros.div(1_000_000.0).toBigDecimal(),
            Currency.getInstance(price.currencyCode),
            bundleOf(
                AppEventsConstants.EVENT_PARAM_CONTENT_ID to purchase.product.id,
                AppEventsConstants.EVENT_PARAM_CURRENCY to price.currencyCode,
                AppEventsConstants.EVENT_PARAM_NUM_ITEMS to 1
            )
        )
    }

    fun getAnonymousID(): String {
        return AppEventsLogger.getAnonymousAppDeviceGUID(applicationContext)
    }
}