package com.example.letsgetweddi.data

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

object FirebaseRefs {
    private val db: FirebaseDatabase get() = FirebaseDatabase.getInstance()

    fun ref(path: String): DatabaseReference = db.getReference(path)
    fun suppliers() = ref(DbPaths.SUPPLIERS)
    fun users() = ref(DbPaths.USERS)
    fun favorites(userId: String) = ref(DbPaths.favoritesOf(userId))
    fun favorite(userId: String, supplierId: String) = ref(DbPaths.favoriteOf(userId, supplierId))
    fun reviews(supplierId: String) = ref(DbPaths.reviewsOf(supplierId))
    fun availability(supplierId: String) = ref(DbPaths.availabilityOf(supplierId))
}
