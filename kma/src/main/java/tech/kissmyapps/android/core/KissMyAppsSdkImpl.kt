package tech.kissmyapps.android.core

import android.content.Context
import android.os.RemoteException
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.android.installreferrer.api.ReferrerDetails
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import tech.kissmyapps.android.KissMyAppsSdk
import tech.kissmyapps.android.analytics.Analytics
import tech.kissmyapps.android.analytics.af.AppsFlyerAnalytics
import tech.kissmyapps.android.analytics.amplitude.AmplitudeAnalytics
import tech.kissmyapps.android.analytics.facebook.FacebookAnalytics
import tech.kissmyapps.android.analytics.firebase.FirebaseAnalytics
import tech.kissmyapps.android.appupdates.AppUpdateManager
import tech.kissmyapps.android.attribution.AttributionClient
import tech.kissmyapps.android.attribution.network.AttributionService
import tech.kissmyapps.android.common.measureTime
import tech.kissmyapps.android.config.FirebaseRemoteConfig
import tech.kissmyapps.android.config.RemoteConfig
import tech.kissmyapps.android.config.model.RemoteConfigParams.MIN_SUPPORTED_APP_VERSION
import tech.kissmyapps.android.config.model.RemoteConfigValue
import tech.kissmyapps.android.core.model.AttributionData
import tech.kissmyapps.android.core.model.ConfigurationResult
import tech.kissmyapps.android.core.model.MediaSource
import tech.kissmyapps.android.data.PreferencesDataStore
import tech.kissmyapps.android.database.Database
import tech.kissmyapps.android.purchases.Purchases
import tech.kissmyapps.android.purchases.PurchasesFacade
import tech.kissmyapps.android.purchases.PurchasesPreferencesDataStore
import tech.kissmyapps.android.purchases.logger.TLMPurchaseEventLogger
import tech.kissmyapps.android.purchases.model.CustomerInfo
import tech.kissmyapps.android.purchases.revenuecat.RevenueCatConfiguration
import tech.kissmyapps.android.purchases.revenuecat.RevenueCatPurchases
import timber.log.Timber

internal class KissMyAppsSdkImpl constructor(
    private val applicationContext: Context,
    override val purchases: Purchases,
    firebaseAnalytics: FirebaseAnalytics,
    private val appUpdateManager: AppUpdateManager,
    private val firebaseRemoteConfig: FirebaseRemoteConfig,
    private val amplitudeAnalytics: AmplitudeAnalytics,
    private val appsFlyerAnalytics: AppsFlyerAnalytics,
    private val facebookAnalytics: FacebookAnalytics,
    private val configuration: KissMyAppsConfiguration,
    private val attributionClient: AttributionClient,
    private val preferencesDataStore: PreferencesDataStore,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : KissMyAppsSdk {
    private val applicationScope = CoroutineScope(SupervisorJob() + coroutineDispatcher)

    override val analytics: Analytics = amplitudeAnalytics
    override val remoteConfig: RemoteConfig = firebaseRemoteConfig

    private var configurationResult: ConfigurationResult? = null

    init {
        val appsFlyerUID = appsFlyerAnalytics.appsFlyerUID

        if (appsFlyerUID != null) {
            amplitudeAnalytics.setUserId(appsFlyerUID)
            firebaseAnalytics.setUserId(appsFlyerUID)
            facebookAnalytics.setUserId(appsFlyerUID)
        }
    }

    override suspend fun start(isFirstLaunch: Boolean?): ConfigurationResult {
        val configurationResult = configurationResult
        if (configurationResult != null) {
            return configurationResult
        }

        return getConfigurationResult(isFirstLaunch)
    }

    override fun getConfigurationResult(): ConfigurationResult? {
        return configurationResult
    }

    private suspend fun getConfigurationResult(isFirstLaunch: Boolean?): ConfigurationResult {
        return withContext(coroutineDispatcher) {
            measureTime("CONFIGURATION") {

                val isFirstAppLaunch = isFirstLaunch != false
                        && preferencesDataStore.isFirstLaunch()

                if (isFirstAppLaunch) {
                    amplitudeAnalytics.logEvent(AnalyticsEvents.FIRST_LAUNCH)
                    amplitudeAnalytics.flush()
                    amplitudeAnalytics.sendCohort()
                    preferencesDataStore.setFirstLaunch(false)
                }

                amplitudeAnalytics.logEvent(
                    event = AnalyticsEvents.APP_LAUNCH,
                    properties = mapOf("first_launch" to isFirstLaunch)
                )

                applicationScope.launch {
                    attributionClient.getAttributionInfo()
                }

                val remoteConfigsDeferred = getRemoteConfigs()

                val attributionDeferred = async {
                    getAttributionData()
                }

                val customerInfoDeferred = getCustomerInfo()

                val attributionData = attributionDeferred.await()
                Timber.d(attributionData.toString())

                val remoteConfigs = withTimeoutOrNull(MAX_TIMEOUT_IN_MILLIS) {
                    remoteConfigsDeferred.await()
                }

                val customerInfo = withTimeoutOrNull(MAX_TIMEOUT_IN_MILLIS) {
                    customerInfoDeferred.await()
                }

                val mediaSource = MediaSource.fromRawValue(attributionData?.mediaSource)

                firebaseRemoteConfig.setMediaSource(mediaSource)

                if (isFirstAppLaunch && attributionData != null) {
                    amplitudeAnalytics.setUserProperties(attributionData.toMap())
                }

                sendTestDistribution(mediaSource, attributionData, remoteConfigs)

                appUpdateManager.setMinSupportedVersionCode(
                    remoteConfig.getLong(MIN_SUPPORTED_APP_VERSION)
                )

                val paywall = remoteConfig.getActivePaywallName()

                configurationResult = ConfigurationResult(
                    activePaywall = paywall,
                    mediaSource = mediaSource,
                    customerInfo = customerInfo
                )

                Timber.d("Finished with $configurationResult.")

                configurationResult!!
            }
        }
    }

    private fun sendTestDistribution(
        mediaSource: MediaSource,
        attribution: AttributionData?,
        remoteConfigs: Map<String, RemoteConfigValue>?
    ) {
        val configsProperties = remoteConfigs.orEmpty()
            .filter {
                val value = configuration.remoteConfigDefaults.values[it.key]
                !value?.activeSources.isNullOrEmpty()
            }
            .mapValues {
                val shouldSend = configuration.remoteConfigDefaults
                    .values[it.key]
                    ?.activeSources?.contains(mediaSource)
                    ?: false

                val value = it.value.rawValue

                if (!shouldSend || value.isNullOrBlank()) {
                    "none"
                } else if (value.startsWith("none_")) {
                    "none"
                } else {
                    value
                }
            }

        val allProperties = attribution?.toMap().orEmpty() + configsProperties

        amplitudeAnalytics.setUserProperties(configsProperties)
        amplitudeAnalytics.logEvent(AnalyticsEvents.TEST_DISTRIBUTION, allProperties)
    }

    private fun getRemoteConfigs(): Deferred<Map<String, RemoteConfigValue>> {
        return applicationScope.async {
            measureTime("Remote configs") {
                firebaseRemoteConfig.fetchAndActivate()
                val configs = remoteConfig.getAll()

                Timber.d(
                    "Configs: \n%s.",
                    configs
                        .mapValues { it.value.rawValue }
                        .entries
                        .joinToString(",\n") { "${it.key} = ${it.value}" }
                )

                configs
            }
        }
    }

    private fun getCustomerInfo(): Deferred<CustomerInfo?> {
        return applicationScope.async {
            measureTime("Customer info") {
                try {
                    val result = purchases.getCustomerInfo()
                    result
                } catch (e: Throwable) {
                    null
                }
            }
        }
    }

    private suspend fun getAttributionData(): AttributionData? {
        return measureTime("Attribution data") {
            val installReferrerAttDeferred = getInstallReferrerAttribution()
            val appsFlyerAttDeferred = getAppsFlyerAttribution()

            val installReferrerAtt = withTimeout(MAX_TIMEOUT_IN_MILLIS) {
                installReferrerAttDeferred.await()
            }

            val appsFlyerAtt = withTimeoutOrNull(MAX_TIMEOUT_IN_MILLIS) {
                appsFlyerAttDeferred.await()
            }

            appsFlyerAtt ?: installReferrerAtt
        }
    }

    private fun getAppsFlyerAttribution(): Deferred<AttributionData?> {
        return applicationScope.async {
            measureTime("AppsFlyer ATT") {
                var attributionData = preferencesDataStore.getAttributionData()

                Timber.d("Preferences AF data = $attributionData")

                if (attributionData == null) {
                    val conversionData = appsFlyerAnalytics.awaitConversionData()

                    if (!conversionData.isNullOrEmpty()) {
                        attributionData = AttributionData.fromConversionData(conversionData)
                        preferencesDataStore.setAttributionData(attributionData)
                    }

                    Timber.d("AF data = $attributionData")
                }

                attributionData
            }
        }
    }

    private fun getInstallReferrerAttribution(): Deferred<AttributionData?> {
        return applicationScope.async {
            measureTime("Install Referrer ATT") {
                var installReferrer: String? = preferencesDataStore.getInstallReferrer()

                if (installReferrer == null) {
                    val referrerDetails = getReferrerDetails(applicationContext)
                    installReferrer = referrerDetails?.installReferrer

                    if (installReferrer != null) {
                        preferencesDataStore.setInstallReferrer(installReferrer)
                    }
                }

                AttributionData.fromInstallReferrer(installReferrer)
            }
        }
    }

    private suspend fun getReferrerDetails(context: Context): ReferrerDetails? {
        val deferredReferrerDetails = CompletableDeferred<ReferrerDetails?>()
        val client = InstallReferrerClient.newBuilder(context.applicationContext).build()
        client.startConnection(object : InstallReferrerStateListener {
            override fun onInstallReferrerSetupFinished(responseInt: Int) {
                if (responseInt == InstallReferrerClient.InstallReferrerResponse.OK) {
                    deferredReferrerDetails.complete(
                        try {
                            client.installReferrer
                        } catch (e: RemoteException) {
                            null
                        }
                    )
                } else {
                    deferredReferrerDetails.complete(null)
                }
                client.endConnection()
            }

            override fun onInstallReferrerServiceDisconnected() {
                if (!deferredReferrerDetails.isCompleted) {
                    deferredReferrerDetails.complete(null)
                }
            }
        })
        return deferredReferrerDetails.await()
    }

    internal companion object {
        const val MAX_TIMEOUT_IN_MILLIS = 6_500L

        fun create(configuration: KissMyAppsConfiguration): KissMyAppsSdk {
            val attributionClient = AttributionClient.create(
                configuration.context,
                configuration.attributionApiKey,
                configuration.appsFlyerDevKey
            )

            val amplitudeAnalytics = AmplitudeAnalytics(
                context = configuration.context,
                apiKey = configuration.amplitudeApiKey
            )

            val appsFlyerAnalytics = AppsFlyerAnalytics(
                devKey = configuration.appsFlyerDevKey,
                applicationContext = configuration.context,
            )

            val facebookAnalytics = FacebookAnalytics(
                configuration.facebookConfiguration,
                configuration.context
            )

            val purchaseLogger = TLMPurchaseEventLogger(
                Database.getInstance(configuration.context).purchasesDao(),
                attributionClient,
                AttributionService.create(configuration.attributionApiKey, false)
            )

            val purchases = PurchasesFacade(
                purchases = RevenueCatPurchases(
                    configuration = RevenueCatConfiguration.Builder(
                        configuration.context,
                        configuration.revenueCatApiKey
                    )
                        .setAppUserId(appsFlyerAnalytics.appsFlyerUID)
                        .setAppsFlyerUID(appsFlyerAnalytics.appsFlyerUID)
                        .setFbAnonymousID(facebookAnalytics.getAnonymousID())
                        .build(),
                    dataStore = PurchasesPreferencesDataStore.create(configuration.context)
                ),
                appsFlyerAnalytics = appsFlyerAnalytics,
                firebaseAnalytics = FirebaseAnalytics(),
                amplitudeAnalytics = amplitudeAnalytics,
                facebookAnalytics = facebookAnalytics,
                tlmPurchaseLogger = purchaseLogger,
            )

            return KissMyAppsSdkImpl(
                applicationContext = configuration.context,
                firebaseAnalytics = FirebaseAnalytics(),
                appUpdateManager = AppUpdateManager(configuration.context),
                firebaseRemoteConfig = FirebaseRemoteConfig(configuration.remoteConfigDefaults),
                attributionClient = attributionClient,
                facebookAnalytics = facebookAnalytics,
                amplitudeAnalytics = amplitudeAnalytics,
                appsFlyerAnalytics = appsFlyerAnalytics,
                purchases = purchases,
                configuration = configuration,
                preferencesDataStore = PreferencesDataStore(configuration.context)
            )
        }
    }
}