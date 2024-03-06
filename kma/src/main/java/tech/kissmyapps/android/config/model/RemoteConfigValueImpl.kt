package tech.kissmyapps.android.config.model

import com.google.firebase.remoteconfig.FirebaseRemoteConfig.VALUE_SOURCE_STATIC
import com.google.firebase.remoteconfig.FirebaseRemoteConfigValue
import tech.kissmyapps.android.core.model.MediaSource
import java.util.regex.Pattern

internal class RemoteConfigValueImpl internal constructor(
    val key: String,
    override val rawValue: String?,
    private val value: String,
    private val source: Int,
) : RemoteConfigValue {
    override fun asLong(): Long {
        if (source == VALUE_SOURCE_STATIC) {
            return 0L
        }

        val valueAsString = asTrimmedString()

        if (valueAsString.isEmpty()) {
            return 0L
        }

        return try {
            valueAsString.toLong()
        } catch (e: NumberFormatException) {
            throw IllegalArgumentException(
                String.format(
                    ILLEGAL_ARGUMENT_STRING_FORMAT,
                    valueAsString,
                    "long"
                ), e
            )
        }
    }

    override fun asDouble(): Double {
        if (source == VALUE_SOURCE_STATIC) {
            return 0.0
        }

        val valueAsString = asTrimmedString()

        if (valueAsString.isEmpty()) {
            return 0.0
        }

        return try {
            valueAsString.toDouble()
        } catch (e: NumberFormatException) {
            throw IllegalArgumentException(
                String.format(
                    ILLEGAL_ARGUMENT_STRING_FORMAT,
                    valueAsString,
                    "double"
                ), e
            )
        }
    }

    override fun asString(): String {
        if (source == VALUE_SOURCE_STATIC) {
            return ""
        }

        return value
    }

    override fun asByteArray(): ByteArray {
        if (source == VALUE_SOURCE_STATIC) {
            return ByteArray(0)
        }

        val value = asString()

        if (value.isEmpty()) {
            return ByteArray(0)
        }

        return asString().toByteArray()
    }

    override fun asBoolean(): Boolean {
        if (source == VALUE_SOURCE_STATIC) {
            return false
        }

        val valueAsString = asTrimmedString()

        if (valueAsString.isEmpty()) {
            return false
        }

        return if (TRUE_REGEX.matcher(valueAsString).matches()) {
            true
        } else if (FALSE_REGEX.matcher(valueAsString).matches()) {
            false
        } else {
            throw IllegalArgumentException(
                String.format(
                    ILLEGAL_ARGUMENT_STRING_FORMAT,
                    valueAsString,
                    "boolean"
                )
            )
        }
    }

    /** Returns a trimmed version of [.asString].  */
    private fun asTrimmedString(): String {
        return asString().trim { it <= ' ' }
    }

    companion object {
        const val ILLEGAL_ARGUMENT_STRING_FORMAT = "[Value: %s] cannot be converted to a %s."

        val TRUE_REGEX: Pattern = Pattern.compile("^(1|true|t|yes|y|on)$", Pattern.CASE_INSENSITIVE)

        val FALSE_REGEX: Pattern =
            Pattern.compile("^(0|false|f|no|n|off|none)$", Pattern.CASE_INSENSITIVE)

        fun from(
            key: String,
            value: FirebaseRemoteConfigValue,
            mediaSource: MediaSource,
            default: RemoteConfigDefault? = null
        ): RemoteConfigValue {
            val rawValue = try {
                val remoteValue = value.asString()
                remoteValue
            } catch (e: Throwable) {
                null
            }

            return from(
                key = key,
                value = rawValue.orEmpty(),
                source = value.source,
                mediaSource = mediaSource,
                default = default
            )
        }

        fun from(
            key: String,
            value: String,
            source: Int,
            mediaSource: MediaSource,
            default: RemoteConfigDefault? = null
        ): RemoteConfigValue {
            return RemoteConfigValueImpl(
                key = key,
                rawValue = value,
                source = source,
                value = default?.getValue(value, mediaSource)
                    ?: if (value.contains("none_")) {
                        value.drop(5)
                    } else if (value == "none") {
                        ""
                    } else {
                        value
                    }
            )
        }
    }
}