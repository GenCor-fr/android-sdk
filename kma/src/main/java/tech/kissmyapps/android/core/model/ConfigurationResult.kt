package tech.kissmyapps.android.core.model

import tech.kissmyapps.android.purchases.model.CustomerInfo

data class ConfigurationResult(
    val activePaywall: String,
    val mediaSource: MediaSource,
    val customerInfo: CustomerInfo? = null,
)