package tech.kissmyapps.android.purchases.logger

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tech.kissmyapps.android.attribution.AttributionClient
import tech.kissmyapps.android.attribution.model.AttributionInfo
import tech.kissmyapps.android.attribution.network.AttributionService
import tech.kissmyapps.android.attribution.network.model.InAppRequestBody
import tech.kissmyapps.android.attribution.network.model.PurchaseResponse
import tech.kissmyapps.android.attribution.network.model.SubscribeRequestBody
import tech.kissmyapps.android.database.PurchaseEntity
import tech.kissmyapps.android.database.PurchaseEntity.ProductType.Companion.INAPP
import tech.kissmyapps.android.database.PurchaseEntity.ProductType.Companion.SUBS
import tech.kissmyapps.android.database.PurchasesDao
import tech.kissmyapps.android.purchases.model.ProductType
import tech.kissmyapps.android.purchases.model.Purchase
import timber.log.Timber

internal class TLMPurchaseEventLogger(
    private val appsFlyerUID: String?,
    private val purchasesDao: PurchasesDao,
    private val attributionClient: AttributionClient,
    private val attributionService: AttributionService,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : PurchaseEventLogger {
    private val applicationScope = CoroutineScope(SupervisorJob() + ioDispatcher)

    init {
        syncPurchases()
    }

    override fun logPurchase(purchase: Purchase) {
        applicationScope.launch {
            logPurchaseInternal(purchase)
        }
    }

    private suspend fun logPurchaseInternal(purchase: Purchase) {
        val attributionInfo = attributionClient.getAttributionInfo()

        val entity = PurchaseEntity(
            productId = purchase.product.id,
            productType = when (purchase.product.type) {
                ProductType.SUBS -> SUBS
                ProductType.INAPP -> INAPP
            },
            purchaseToken = purchase.purchaseToken,
            price = purchase.price.amount,
            currency = purchase.price.currencyCode
        )

        if (attributionInfo == null) {
            purchasesDao.insert(entity)
            return
        }

        try {
            purchasesDao.insert(entity)
            sendPurchase(attributionInfo, entity)

            Timber.d("The $purchase has been successfully submitted.")
        } catch (e: Throwable) {
            Timber.e(e, "Failed to send purchase: $purchase.")
            return
        }
    }

    private fun syncPurchases() {
        applicationScope.launch {
            syncPurchaseInternal()
        }
    }

    private suspend fun syncPurchaseInternal() {
        withContext(ioDispatcher) {
            val purchases = try {
                purchasesDao.getAll()
            } catch (e: Throwable) {
                Timber.e(e, "Failed to sync purchases.")
                return@withContext
            }

            val attributionInfo = attributionClient.getAttributionInfo() ?: return@withContext

            for (purchase in purchases) {
                launch {
                    try {
                        sendPurchase(attributionInfo, purchase)
                    } catch (e: Throwable) {
                        Timber.e(e, "Failed to sync $purchase.")
                    }
                }
            }
        }
    }

    private suspend fun sendPurchase(
        attributionInfo: AttributionInfo,
        purchaseEntity: PurchaseEntity
    ): PurchaseResponse? {
        val result = when (purchaseEntity.productType) {
            SUBS -> {
                attributionService.sendSubscriptionPurchase(
                    SubscribeRequestBody(
                        userId = appsFlyerUID ?: attributionInfo.userId,
                        uuid = attributionInfo.uuid,
                        productId = purchaseEntity.productId,
                        purchaseId = purchaseEntity.purchaseToken
                    )
                )
            }

            INAPP -> {
                attributionService.sendInAppPurchase(
                    InAppRequestBody(
                        userId = appsFlyerUID ?: attributionInfo.userId,
                        uuid = attributionInfo.uuid,
                        productId = purchaseEntity.productId,
                        purchaseId = purchaseEntity.purchaseToken,
                        paymentDetails = InAppRequestBody.PaymentDetails(
                            price = purchaseEntity.price,
                            currency = purchaseEntity.currency
                        )
                    )
                )
            }

            else -> throw IllegalStateException("Unsupported purchase type.")
        }

        purchasesDao.delete(purchaseEntity.purchaseToken)

        return result
    }
}