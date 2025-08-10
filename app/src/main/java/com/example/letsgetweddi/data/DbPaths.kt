package com.example.letsgetweddi.data

object DbPaths {
    // Root collections
    const val SUPPLIERS = "Suppliers"
    const val USERS = "Users"
    const val FAVORITES = "Favorites"
    const val REVIEWS = "Reviews"
    const val SUPPLIERS_AVAILABILITY = "SuppliersAvailability"

    // Convenience builders (avoid manual string concat everywhere)
    fun supplier(supplierId: String) = "$SUPPLIERS/$supplierId"
    fun user(userId: String) = "$USERS/$userId"
    fun favoritesOf(userId: String) = "$FAVORITES/$userId"
    fun favoriteOf(userId: String, supplierId: String) = "$FAVORITES/$userId/$supplierId"
    fun reviewsOf(supplierId: String) = "$REVIEWS/$supplierId"
    fun availabilityOf(supplierId: String) = "$SUPPLIERS_AVAILABILITY/$supplierId"
    fun availabilityDate(supplierId: String, isoDate: String) =
        "$SUPPLIERS_AVAILABILITY/$supplierId/$isoDate"
}
