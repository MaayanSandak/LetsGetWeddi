package com.example.letsgetweddi.data

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

object FirebaseRefs {
    private val db: FirebaseDatabase get() = FirebaseDatabase.getInstance()

    fun ref(path: String): DatabaseReference = db.getReference(path)

    fun suppliers(): DatabaseReference = ref(DbPaths.SUPPLIERS)
    fun users(): DatabaseReference = ref(DbPaths.USERS)

    fun favorites(userId: String): DatabaseReference = ref(DbPaths.favoritesOf(userId))
    fun favorite(userId: String, supplierId: String): DatabaseReference =
        ref(DbPaths.favoriteOf(userId, supplierId))

    fun reviews(supplierId: String): DatabaseReference =
        ref(DbPaths.reviewsOf(supplierId))

    fun availability(supplierId: String): DatabaseReference =
        ref(DbPaths.availabilityOf(supplierId))
}
