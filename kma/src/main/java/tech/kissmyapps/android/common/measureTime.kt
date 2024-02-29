package tech.kissmyapps.android.common

import timber.log.Timber
import kotlin.time.measureTimedValue

inline fun <T> measureTime(message: String, body: () -> T): T {
    Timber.d("$message started.")
    val timedValue = measureTimedValue(body)
    Timber.d("$message elapsed time: ${timedValue.duration}.")
    return timedValue.value
}