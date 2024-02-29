package tech.kissmyapps.android

import androidx.annotation.Keep
import tech.kissmyapps.android.core.model.ConfigurationResult

@Keep
fun interface ConfigurationRequestListener {
    fun onSuccess(result: ConfigurationResult)
}