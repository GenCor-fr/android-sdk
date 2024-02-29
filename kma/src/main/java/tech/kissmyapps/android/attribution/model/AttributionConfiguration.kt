package tech.kissmyapps.android.attribution.model

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher

class AttributionConfiguration(
    val applicationContext: Context,
    val apiKey: String,
    val appsFlyerUID: String?,
    val coroutineDispatcher: CoroutineDispatcher,
)