package tech.kissmyapps.android.attribution

import android.content.Context
import android.os.Build
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import tech.kissmyapps.android.attribution.data.AttributionDataSource
import tech.kissmyapps.android.attribution.data.AttributionDataStore
import tech.kissmyapps.android.attribution.model.AttributionInfo
import tech.kissmyapps.android.attribution.network.AttributionService
import tech.kissmyapps.android.attribution.network.model.InstallApplicationRequestBody
import timber.log.Timber

internal interface AttributionClient {
    suspend fun getAttributionInfo(): AttributionInfo?

    companion object {
        fun create(context: Context, apiKey: String, appsFlyerUID: String): AttributionClient {
            return AttributionClientImpl(
                appsFlyerUID,
                AttributionService.create(apiKey, true),
                AttributionDataStore.getInstance(context),
                AttributionDataSource.create(context)
            )
        }
    }
}

internal class AttributionClientImpl(
    private val appsFlyerUID: String,
    private val attributionService: AttributionService,
    private val attributionDataStore: AttributionDataStore,
    private val attributionDataSource: AttributionDataSource,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AttributionClient {
    private val mutex = Mutex()

    private var attributionInfo: AttributionInfo? = null

    override suspend fun getAttributionInfo(): AttributionInfo? {
        if (attributionInfo != null) {
            return attributionInfo
        }

        return withContext(ioDispatcher) {
            mutex.withLock {
                if (attributionInfo == null) {
                    attributionInfo = attributionDataStore.getAttributionInfo()
                }

                if (attributionInfo == null) {
                    attributionInfo = sendInstall()

                    if (attributionInfo != null) {
                        attributionDataStore.setAttributionData(attributionInfo!!)
                    }
                }

                attributionInfo
            }
        }
    }

    private suspend fun sendInstall(): AttributionInfo? {
        val advertisingId = attributionDataSource.getAdvertisingId()

        var userId = advertisingId?.id
        val isLimitAdTrackingEnabled = advertisingId?.isLimitAdTrackingEnabled ?: true

        if (userId == null) {
            userId = attributionDataSource.getAppSetId()?.id
        }

        if (userId == null) {
            userId = attributionDataSource.getUUID()
        }

        return try {
            val response = attributionService.sendInstall(
                InstallApplicationRequestBody(
                    userId = userId,
                    appVersion = attributionDataSource.getApplicationVersion()!!,
                    sdkVersion = "1.0.0",
                    osVersion = Build.VERSION.RELEASE,
                    appsFlyerUUID = appsFlyerUID,
                    isLimitAdTrackingEnabled = isLimitAdTrackingEnabled
                )
            )

            if (response?.uuid != null) {
                return AttributionInfo(
                    userId = userId,
                    uuid = response.uuid,
                    network = response.network,
                    networkType = response.networkType,
                    networkSubtype = response.networkSubtype,
                    campaignName = response.campaignName,
                    campaignType = response.campaignType,
                    adGroupName = response.adGroupName,
                    creativeName = response.creativeName,
                    attributed = response.attributed == true,
                )
            } else {
                null
            }
        } catch (e: Throwable) {
            Timber.e(e, "Failed to send install application data.")
            null
        }
    }
}