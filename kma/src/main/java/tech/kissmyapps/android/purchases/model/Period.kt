package tech.kissmyapps.android.purchases.model

data class Period(
    val value: Int,
    val unit: Unit
) {
    enum class Unit {
        DAY,
        WEEK,
        MONTH,
        YEAR,
        UNKNOWN;
    }
}