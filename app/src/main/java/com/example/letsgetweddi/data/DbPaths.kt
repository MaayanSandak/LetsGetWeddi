package com.example.letsgetweddi.data

object DbPaths {
    const val USERS = "users"
    const val SUPPLIERS = "suppliers"
    const val FAVORITES = "favorites"
    const val REVIEWS = "reviews"
    const val SUPPLIERS_AVAILABILITY = "suppliersAvailability"
    const val CHATS = "chats"

    fun supplier(id: String) = "$SUPPLIERS/$id"
    fun user(uid: String) = "$USERS/$uid"

    fun favoritesOf(userId: String) = "$FAVORITES/$userId"
    fun favoriteOf(userId: String, supplierId: String) = "$FAVORITES/$userId/$supplierId"

    fun reviewsOf(supplierId: String) = "$REVIEWS/$supplierId"
    fun availabilityOf(supplierId: String) = "$SUPPLIERS_AVAILABILITY/$supplierId"
}
