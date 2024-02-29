# KMA Android SDK

## Installation

```gradle
repositories {
  ...
  maven {
    url "https://git.netpeak.net/api/v4/projects/PROJECT_ID/packages/maven"
    credentials(HttpHeaderCredentials) {
        name = "Private-Token"
        value = "TOKEN"
    }
    authentication {
        header(HttpHeaderAuthentication)
    }
    name "Gitlab"
  }
}

dependencies {
  implementation 'tech.kissmyapps.android:1.0.0'
}
```

## Usage

```kotlin
// On Application class
KissMyAppsSdk.configure(
    KissMyAppsConfiguration(
        applicationContext,
        appsFlyerDevKey = BuildConfig.APPSFLYER_DEV_KEY,
        amplitudeApiKey = BuildConfig.AMPLITUDE_API_KEY,
        revenueCatApiKey = BuildConfig.REVENUE_CAT_API_KEY,
        attributionApiKey = BuildConfig.TLM_SUBSCRIPTION_API_KEY,
        facebookConfiguration = FacebookConfiguration(
            applicationId = BuildConfig.FB_APP_ID,
            clientToken = BuildConfig.FB_CLIENT_TOKEN
        ),
        remoteConfigDefaults = RemoteConfigDefaults()
            .setGeneralPaywall(PaywallTypes.VERTICAL_BOXES)
            .setFacebookPaywall(PaywallTypes.VERTICAL_BOXES)
            .setGoogleRedirectPaywall(PaywallTypes.VERTICAL_BOXES)
            .setMinimalSupportedAppVersion(BuildConfig.VERSION_CODE)
            .param(
                key = RemoteConfigs.AB_PAYWALL_SOFT,
                sources = MediaSource.all()
            ), // custom A/B test remote config
    )
)

// Start SDK and keep splash screen
class MainViewModel : ViewModel() {
    var shouldKeepSplashOnScreen = true
        private set

    init {
        viewModelScope.launch {
            KissMyAppsSdk.sharedInstance.start()
            // YOUR INIT LOGIC ...
            shouldKeepSplashOnScreen = false
        }
    }
}

// Keep splash on screen logic
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition {
            viewModel.shouldKeepSplashOnScreen
        }
    }
}

// Amplitude analytics usage
KissMyAppsSdk.sharedInstance.analytics.logEvent("event_type", mapOf("key" to "value"))

// Purchases analytics usage
KissMyAppsSdk.sharedInstance.purchases.purchase(activity, "product_id")

// Subscription state
KissMyAppsSdk.sharedInstance.purchases.getCustomerInfoFlow()
    .map { customerInfo ->
        val isSubscribed = !customerInfo?.activeSubscriptions.isNullOrEmpty()

        val hasLifetime = customerInfo
            ?.allPurchasedProductIds
            ?.contains(Products.PAYWALL_LIFETIME)
            ?: false

        Result.Success(isSubscribed || hasLifetime)
    }

// Remote Configs usage
KissMyAppsSdk.sharedInstance.remoteConfig.getBoolean("config_key")

```

## Integrations

| Library                | Version |
|------------------------|---------|
| Amplitude              | 1.12.2  |
| AppsFlyer              | 6.13.0  |
| Facebook Core          | 16.0.1  |
| RevenueCat             | 7.6.0   |
| Firebase Analytics     | 21.5.1  |
| Firebase Remote Config | 21.6.1  |

## Amplitude events

| Event             | Description                           |
|-------------------|---------------------------------------|
| app_launch        | Launching the application             |
| first_launch      | First launch of the application       |
| test_distribution | Attribution and A/B test distribution |
| trial_started     | Start of subscription trial           |

## Amplitude properties

| Property                  | Description                                                  |
|---------------------------|--------------------------------------------------------------|
| cohort_year               | Cohort year                                                  |
| cohort_week               | Cohort week                                                  |
| cohort_month              | Cohort month                                                 |
| network                   | User media source (none, Facebook Ads, Google_StoreRedirect) |
| ad                        | Ad name                                                      |
| campaignName              | Ad campaign name                                             |
| adGroupName               | Ad group name                                                |
| deep_link_value           | OneLink deep link value                                      |
| active_subscriptions      | Active Google Play subscriptions                             |
| all_purchased_product_ids | All purchased Google Play product ids                        |

## AppsFlyer events

| Event                 | Description                       |
|-----------------------|-----------------------------------|
| af_initiated_checkout | Initiated checkout for a purchase |
| af_start_trial        | Start of subscription trial       |
| af_purchase           | Successful purchase               |

## Firebase events

| Event            | Description                 |
|------------------|-----------------------------|
| trial_started    | Start of subscription trial |
| in_app_purchased | Successful purchase         |

## Facebook events

| Event              | Description                 |
|--------------------|-----------------------------|
| StartTrial         | Start of subscription trial |
| fb_mobile_purchase | Successful purchase         |

## Remote Configs

| Name                                   | Type    | Default value | Description                                             |
|----------------------------------------|---------|---------------|---------------------------------------------------------|
| android_minimal_supported_app_version  | number  | 0             | Minimum supported version (build number)                |
| android_rate_us_primary_shown          | boolean | false         | Determines whether to show the primary In-App Review    |
| android_rate_us_secondary_shown        | boolean | false         | Determines whether to show the secondary In-App Review  |
| android_subscription_screen_style_full | boolean | true          | Paywall style (true - In Review / false - After Review) |
| android_subscription_screen_style_hard | boolean | false         | Paywall type (true - hard / false - soft)               |
| android_ab_paywall_general             | string  | none          | General paywall name                                    |
| android_ab_paywall_google_redirect     | string  | none          | Paywall name for Google Ads                             |
| android_ab_paywall_facebook            | string  | none          | Paywall name for Facebook Ads                           |