package tech.kissmyapps.android

import com.revenuecat.purchases.Purchases.Companion.configure
import tech.kissmyapps.android.analytics.Analytics
import tech.kissmyapps.android.common.ConfigureStrings
import tech.kissmyapps.android.config.RemoteConfig
import tech.kissmyapps.android.core.KissMyAppsConfiguration
import tech.kissmyapps.android.core.KissMyAppsSdkImpl
import tech.kissmyapps.android.core.model.ConfigurationResult
import tech.kissmyapps.android.purchases.Purchases

interface KissMyAppsSdk {
    /**
     * Instance of Amplitude analytics.
     */
    val analytics: Analytics

    /**
     * Instance of Firebase Remote Config.
     */
    val remoteConfig: RemoteConfig

    /**
     * Instance of purchases with RevenueCat under the hood.
     */
    val purchases: Purchases

    /**
     * Starts the SDK.
     * @return A configuration result.
     */
    suspend fun start(isFirstLaunch: Boolean? = null): ConfigurationResult

    /**
     * @return latest configuration result.
     */
    fun getConfigurationResult(): ConfigurationResult?

    fun getUserId(): String?

    companion object {
        @Volatile
        private var INSTANCE: KissMyAppsSdk? = null

        /**
         * Singleton instance of [KissMyAppsSdk]. [configure] will set this
         * @return A previously set singleton [KissMyAppsSdk] instance
         * @throws UninitializedPropertyAccessException if the shared instance has not been configured.
         */
        @JvmStatic
        val sharedInstance: KissMyAppsSdk
            get() {
                return INSTANCE
                    ?: throw UninitializedPropertyAccessException(ConfigureStrings.NO_SINGLETON_INSTANCE)
            }

        /**
         * Configures an instance of the Kiss My Apps SDK with a specified configuration. The instance will
         * be set as a singleton. You should access the singleton instance using [KissMyAppsSdk.sharedInstance]
         * @param configuration: the [KissMyAppsConfiguration] object you wish to use to configure [KissMyAppsSdk].
         * @return An instantiated `[KissMyAppsSdk] object that has been set as a singleton.
         */
        @JvmStatic
        fun configure(configuration: KissMyAppsConfiguration): KissMyAppsSdk {
            return INSTANCE ?: synchronized(this) {
                if (INSTANCE == null) {
                    INSTANCE = KissMyAppsSdkImpl.create(configuration)
                }

                INSTANCE!!
            }
        }
    }
}