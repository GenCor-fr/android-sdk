package tech.kissmyapps.android.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import tech.kissmyapps.android.core.model.AttributionData
import timber.log.Timber

internal class PreferencesDataStore(
    private val context: Context
) {
    private val Context.preferences by preferencesDataStore("kma_preferences")

    private val keyFirstLaunch = booleanPreferencesKey("is_first_launch")
    private val keyMediaSource = stringPreferencesKey("media_source")
    private val keyCampaignName = stringPreferencesKey("campaign_name")
    private val keyAdSet = stringPreferencesKey("ad_set")
    private val keyAdGroup = stringPreferencesKey("ad_group")
    private val keyDeepLinkValue = stringPreferencesKey("deep_link_value")
    private val keyInstallReferrer = stringPreferencesKey("gp_referrer")

    suspend fun isFirstLaunch(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                context.preferences.data
                    .map { it[keyFirstLaunch] ?: true }
                    .first()
            } catch (e: Throwable) {
                Timber.e(e)
                true
            }
        }
    }

    suspend fun setFirstLaunch(isFirstLaunch: Boolean) {
        withContext(Dispatchers.IO) {
            try {
                context.preferences.edit {
                    it[keyFirstLaunch] = isFirstLaunch
                }
            } catch (e: Throwable) {
                Timber.e(e)
            }
        }
    }

    suspend fun getInstallReferrer() = getValue(keyInstallReferrer)

    suspend fun setInstallReferrer(installReferrer: String) {
        setValue(keyInstallReferrer, installReferrer)
    }

    suspend fun setAttributionData(attributionData: AttributionData) {
        withContext(Dispatchers.IO) {
            try {
                context.preferences.edit {
                    if (attributionData.mediaSource != null) {
                        it[keyMediaSource] = attributionData.mediaSource
                    }

                    if (attributionData.campaign != null) {
                        it[keyCampaignName] = attributionData.campaign
                    }

                    if (attributionData.ad != null) {
                        it[keyAdSet] = attributionData.ad
                    }

                    if (attributionData.adGroup != null) {
                        it[keyAdGroup] = attributionData.adGroup
                    }

                    if (attributionData.deepLinkValue != null) {
                        it[keyDeepLinkValue] = attributionData.deepLinkValue
                    }
                }
            } catch (e: Throwable) {
                Timber.e(e)
            }
        }
    }

    suspend fun getAttributionData(): AttributionData? {
        return withContext(Dispatchers.IO) {
            try {
                context.preferences.data
                    .map {
                        val mediaSource = it[keyMediaSource]

                        if (mediaSource != null) {
                            AttributionData(
                                mediaSource = mediaSource,
                                campaign = it[keyCampaignName],
                                adGroup = it[keyAdGroup],
                                ad = it[keyAdSet],
                                deepLinkValue = it[keyDeepLinkValue]
                            )
                        } else {
                            null
                        }
                    }
                    .firstOrNull()
            } catch (e: Throwable) {
                Timber.e(e)
                null
            }
        }
    }

    private suspend fun <T> getValue(key: Preferences.Key<T>): T? {
        return withContext(Dispatchers.IO) {
            try {
                context.preferences.data
                    .map { it[key] }
                    .firstOrNull()
            } catch (e: Throwable) {
                Timber.e(e)
                null
            }
        }
    }

    private suspend fun <T> setValue(key: Preferences.Key<T>, value: T?) {
        withContext(Dispatchers.IO) {
            try {
                context.preferences.edit {
                    if (value != null) {
                        it[key] = value
                    } else {
                        it -= key
                    }
                }
            } catch (e: Throwable) {
                Timber.e(e)
            }
        }
    }
}