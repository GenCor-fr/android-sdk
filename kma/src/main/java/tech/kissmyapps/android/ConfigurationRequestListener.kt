package tech.kissmyapps.android

import tech.kissmyapps.android.core.model.ConfigurationResult

fun interface ConfigurationRequestListener {
    fun onSuccess(result: ConfigurationResult)
}