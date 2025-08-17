package com.example.letsgetweddi.data

object DbPaths {
    const val USERS = "Users"
    const val SUPPLIERS = "Suppliers"
    const val FAVORITES = "Favorites"
    const val SUPPLIER_REVIEWS = "SupplierReviews"
    const val SUPPLIERS_AVAILABILITY = "SuppliersAvailability"
    const val CHATS = "Chats"

    fun supplier(id: String) = "$SUPPLIERS/$id"
    fun user(uid: String) = "$USERS/$uid"

    fun favoritesOf(userId: String) = "$FAVORITES/$userId"
    fun favoriteOf(userId: String, supplierId: String) = "$FAVORITES/$userId/$supplierId"

    fun reviewsOf(supplierId: String) = "$SUPPLIER_REVIEWS/$supplierId"
    fun availabilityOf(supplierId: String) = "$SUPPLIERS_AVAILABILITY/$supplierId"
}
