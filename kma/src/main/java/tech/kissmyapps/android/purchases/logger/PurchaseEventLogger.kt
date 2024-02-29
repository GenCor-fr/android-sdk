package tech.kissmyapps.android.purchases.logger

import tech.kissmyapps.android.purchases.model.Purchase

internal interface PurchaseEventLogger {
    fun logPurchase(purchase: Purchase)
}