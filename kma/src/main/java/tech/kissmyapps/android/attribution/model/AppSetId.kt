package tech.kissmyapps.android.attribution.model

import androidx.annotation.IntDef

data class AppSetId(
    val id: String,
    @Scope val scope: Int,
) {

    companion object {
        const val SCOPE_APP = 1
        const val SCOPE_DEVELOPER = 2
    }

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(value = [SCOPE_APP, SCOPE_DEVELOPER])
    annotation class Scope
}