package tech.kissmyapps.android.attribution.data

import android.content.Context
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.android.gms.appset.AppSet
import kotlinx.coroutines.tasks.await
import tech.kissmyapps.android.attribution.model.AdvertisingId
import tech.kissmyapps.android.attribution.model.AppSetId
import timber.log.Timber
import java.util.UUID

internal interface AttributionDataSource {
    suspend fun getAdvertisingId(): AdvertisingId?

    suspend fun getAppSetId(): AppSetId?

    fun getUUID(): String

    fun getApplicationVersion(): String?

    companion object Factory {
        fun create(applicationContext: Context): AttributionDataSource {
            return AttributionDataSourceImpl(applicationContext)
        }
    }
}

internal class AttributionDataSourceImpl(
    private val applicationContext: Context
) : AttributionDataSource {
    override fun getApplicationVersion(): String? {
        return try {
            applicationContext.packageManager
                .getPackageInfo(applicationContext.packageName, 0)
                .versionName
        } catch (e: Throwable) {
            Timber.e(e)
            null
        }
    }

    override suspend fun getAdvertisingId(): AdvertisingId? {
        return try {
            val info = AdvertisingIdClient.getAdvertisingIdInfo(applicationContext)

            if (info.id == "00000000-0000-0000-0000-000000000000") {
                AdvertisingId(id = null, isLimitAdTrackingEnabled = true)
            }

            AdvertisingId(info.id, info.isLimitAdTrackingEnabled)
        } catch (e: Throwable) {
            Timber.e(e)
            AdvertisingId(id = null, isLimitAdTrackingEnabled = true)
        }
    }

    override suspend fun getAppSetId(): AppSetId? {
        return try {
            val client = AppSet.getClient(applicationContext)
            val info = client.appSetIdInfo.await()
            AppSetId(info.id, info.scope)
        } catch (e: Throwable) {
            Timber.e(e)
            null
        }
    }

    override fun getUUID(): String {
        return UUID.randomUUID().toString()
    }
}