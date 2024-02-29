package tech.kissmyapps.android.core

import android.content.Context
import android.os.RemoteException
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.android.installreferrer.api.ReferrerDetails
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import tech.kissmyapps.android.ConfigurationRequestListener
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
import tech.kissmyapps.android.config.RemoteConfig.Companion.NONE_VALUE
import tech.kissmyapps.android.config.model.RemoteConfigParams
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
import tech.kissmyapps.android.purchases.revenuecat.RevenueCatConfiguration
import tech.kissmyapps.android.purchases.revenuecat.RevenueCatPurchases
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class KissMyAppsSdkImpl constructor(
    private val applicationContext: Context,
    override val purchases: Purchases,
    firebaseAnalytics: FirebaseAnalytics,
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
    private val appUpdateManager = AppUpdateManager(applicationContext)

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

    override fun start(
        isFirstLaunch: Boolean?,
        configurationRequestListener: ConfigurationRequestListener
    ) {
        val configurationResult = configurationResult
        if (configurationResult != null) {
            configurationRequestListener.onSuccess(configurationResult)
            return
        }

        applicationScope.launch {
            val result = getConfigurationResult(isFirstLaunch)
            configurationRequestListener.onSuccess(result)
        }
    }

    override suspend fun start(isFirstLaunch: Boolean?): ConfigurationResult {
        return suspendCoroutine { continuation ->
            start(isFirstLaunch) { result ->
                continuation.resume(result)
            }
        }
    }

    override fun getConfigurationResult(): ConfigurationResult? {
        return configurationResult
    }

    private suspend fun getConfigurationResult(isFirstLaunch: Boolean?): ConfigurationResult {
        if (configurationResult != null) {
            configurationResult!!
        }

        return withContext(coroutineDispatcher) {
            measureTime("Configuration") {
                val isFirstAppLaunch = isFirstLaunch == true || preferencesDataStore.isFirstLaunch()

                if (isFirstAppLaunch) {
                    amplitudeAnalytics.logEvent(AnalyticsEvents.FIRST_LAUNCH)

                    amplitudeAnalytics.flush()
                    amplitudeAnalytics.sendCohort()
                    preferencesDataStore.setFirstLaunch(true)
                }

                amplitudeAnalytics.logEvent(
                    event = AnalyticsEvents.APP_LAUNCH,
                    properties = mapOf("first_launch" to isFirstLaunch)
                )

                launch {
                    attributionClient.getAttributionInfo()
                }

                val remoteConfigsDeferred = async {
                    measureTime("RemoteConfig") {
                        firebaseRemoteConfig.fetchAndActivate()
                        remoteConfig.getAll()
                    }
                }

                val attributionDeferred = async {
                    measureTime("Attribution") {
                        getAttributionData()
                    }
                }

                val customerInfoDeferred = async(SupervisorJob()) {
                    measureTime("Purchases") {
                        try {
                            val result = purchases.getCustomerInfo()
                            result
                        } catch (e: Throwable) {
                            null
                        }
                    }
                }

                val attributionData = attributionDeferred.await()

                val remoteConfigs = withTimeoutOrNull(MAX_TIMEOUT_TIME) {
                    remoteConfigsDeferred.await()
                }

                val customerInfo = withTimeoutOrNull(MAX_TIMEOUT_TIME) {
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
                    NONE_VALUE
                } else if (value.contains("${NONE_VALUE}_")) {
                    NONE_VALUE
                } else {
                    value
                }
            }

        val allProperties = attribution?.toMap().orEmpty() + configsProperties

        amplitudeAnalytics.setUserProperties(configsProperties)
        amplitudeAnalytics.logEvent(AnalyticsEvents.TEST_DISTRIBUTION, allProperties)
    }

    private suspend fun getAttributionData(): AttributionData? {
        return withContext(coroutineDispatcher) {
            val installReferrerAttDeferred = async {
                getInstallReferrerAttribution()
            }

            val appsFlyerAttDeferred = async {
                getAppsFlyerAttribution()
            }

            val installReferrerAtt = withTimeoutOrNull(MAX_TIMEOUT_TIME) {
                installReferrerAttDeferred.await()
            }

            val appsFlyerAtt = withTimeoutOrNull(MAX_TIMEOUT_TIME) {
                appsFlyerAttDeferred.await()
            }

            appsFlyerAtt ?: installReferrerAtt
        }
    }

    private suspend fun getAppsFlyerAttribution(): AttributionData? {
        return measureTime("AppsFlyer ATT") {
            var attributionData = preferencesDataStore.getAttributionData()

            if (attributionData == null) {
                val conversionData = appsFlyerAnalytics.awaitConversionData()

                if (!conversionData.isNullOrEmpty()) {
                    attributionData = AttributionData.fromConversionData(conversionData)
                    preferencesDataStore.setAttributionData(attributionData)
                }
            }

            attributionData
        }
    }

    private suspend fun getInstallReferrerAttribution(): AttributionData? {
        return measureTime("Install Referrer ATT") {
            var installReferrer: String? = preferencesDataStore.getInstallReferrer()

            if (installReferrer == null) {
                val referrerDetails = getReferrerDetails(applicationContext) ?: return null
                installReferrer = referrerDetails.installReferrer
                preferencesDataStore.setInstallReferrer(installReferrer)
            }

            AttributionData.fromInstallReferrer(installReferrer)
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
        const val MAX_TIMEOUT_TIME = 6_500L

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