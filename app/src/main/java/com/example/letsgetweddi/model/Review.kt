package com.example.letsgetweddi.model

import com.google.firebase.database.DataSnapshot

data class Review(
    val userId: String? = null,
    val name: String? = null,
    val rating: Float = 0f,
    val comment: String? = null,
    val timestamp: Long = 0
) {
    companion object {
        fun fromSnapshot(snapshot: DataSnapshot): Review? {
            val userId = snapshot.child("userId").getValue(String::class.java)
            val name = snapshot.child("name").getValue(String::class.java)
            val rating = snapshot.child("rating").getValue(Float::class.java)
                ?: snapshot.child("rating").getValue(Double::class.java)?.toFloat()
                ?: 0f
            val comment = snapshot.child("comment").getValue(String::class.java)
            val timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L

            return Review(userId, name, rating, comment, timestamp)
        }
    }
}
