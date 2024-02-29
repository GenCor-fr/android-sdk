package tech.kissmyapps.android.core.model

enum class MediaSource {
    ORGANIC,
    GOOGLE,
    FACEBOOK_ADS;

    operator fun plus(other: MediaSource): Set<MediaSource> = setOf(this, other)

    companion object {
        internal fun fromRawValue(rawValue: String?): MediaSource {
            if (rawValue.isNullOrBlank()) {
                return ORGANIC
            }

            val network = rawValue.replace(" ", "_")

            if (network.contains("Facebook_Ads")) {
                return FACEBOOK_ADS
            }

            if (network.contains("Google_StoreRedirect")) {
                return GOOGLE
            }

            return ORGANIC
        }

        fun all() = MediaSource.values().toSet()
    }
}