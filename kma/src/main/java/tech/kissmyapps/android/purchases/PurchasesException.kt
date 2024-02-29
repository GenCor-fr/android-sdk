package tech.kissmyapps.android.purchases

class PurchasesException(val error: PurchasesError) : Exception() {
    override val message = error.description
}

enum class PurchasesError(val description: String) {
    NetworkError("Error performing request."),
    UnknownError("Unknown error."),
    ProductAlreadyPurchasedError("This product is already active for the user."),
    PurchaseCancelledError("Purchase was cancelled."),
}