package tech.kissmyapps.android.attribution.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import tech.kissmyapps.android.attribution.model.AttributionInfo

internal interface AttributionDataStore {
    suspend fun setAttributionData(attributionInfo: AttributionInfo)

    suspend fun getAttributionInfo(): AttributionInfo?

    companion object {
        @Volatile
        private var INSTANCE: AttributionDataStore? = null

        fun getInstance(applicationContext: Context): AttributionDataStore {
            return INSTANCE ?: synchronized(this) {
                if (INSTANCE == null) {
                    INSTANCE = create(applicationContext)
                }

                INSTANCE!!
            }
        }

        private fun create(applicationContext: Context): AttributionDataStore {
            val dataStore = PreferenceDataStoreFactory.create {
                applicationContext.preferencesDataStoreFile("tlm_attribution")
            }

            return AttributionDataStoreImpl(dataStore)
        }
    }
}

internal class AttributionDataStoreImpl(
    private val dataStore: DataStore<Preferences>
) : AttributionDataStore {
    private val uuid = stringPreferencesKey("uuid")
    private val userId = stringPreferencesKey("user_id")
    private val network = stringPreferencesKey("network")
    private val networkType = stringPreferencesKey("network_type")
    private val networkSubtype = stringPreferencesKey("network_subtype")
    private val campaignName = stringPreferencesKey("campaign_name")
    private val campaignType = stringPreferencesKey("campaign_type")
    private val adGroupName = stringPreferencesKey("ad_group_name")
    private val creativeName = stringPreferencesKey("creative_name")
    private val attributed = booleanPreferencesKey("attributed")

    override suspend fun setAttributionData(attributionInfo: AttributionInfo) {
        dataStore.edit {
            it[uuid] = attributionInfo.uuid
            it[userId] = attributionInfo.userId

            if (attributionInfo.network != null) {
                it[network] = attributionInfo.network
            } else {
                it -= network
            }

            if (attributionInfo.networkType != null) {
                it[networkType] = attributionInfo.networkType
            } else {
                it -= networkType
            }

            if (attributionInfo.networkSubtype != null) {
                it[networkSubtype] = attributionInfo.networkSubtype
            } else {
                it -= networkSubtype
            }

            if (attributionInfo.campaignName != null) {
                it[networkSubtype] = attributionInfo.campaignName
            } else {
                it -= campaignName
            }

            if (attributionInfo.campaignType != null) {
                it[campaignType] = attributionInfo.campaignType
            } else {
                it -= campaignType
            }

            if (attributionInfo.adGroupName != null) {
                it[adGroupName] = attributionInfo.adGroupName
            } else {
                it -= adGroupName
            }

            if (attributionInfo.creativeName != null) {
                it[creativeName] = attributionInfo.creativeName
            } else {
                it -= creativeName
            }
        }
    }

    override suspend fun getAttributionInfo(): AttributionInfo? {
        return dataStore.data
            .map {
                val uuid = it[uuid]
                val userId = it[userId]

                if (uuid != null && userId != null) {
                    AttributionInfo(
                        uuid = uuid,
                        userId = userId,
                        network = it[network],
                        networkType = it[networkType],
                        networkSubtype = it[networkSubtype],
                        campaignName = it[campaignName],
                        campaignType = it[campaignType],
                        adGroupName = it[adGroupName],
                        creativeName = it[creativeName],
                        attributed = it[attributed] == true
                    )
                } else {
                    null
                }
            }
            .firstOrNull()
    }
}