package com.example.letsgetweddi.model

import com.google.firebase.database.DataSnapshot

data class Review(
    val userId: String? = null,
    val name: String? = null,
    val rating: Float = 0f,
    val comment: String? = null,
    val timestamp: Long = 0L
) {
    companion object {
        fun fromSnapshot(snapshot: DataSnapshot): Review? {
            if (!snapshot.exists()) return null

            val userId = snapshot.child("userId").getValue(String::class.java)
            val name = snapshot.child("name").getValue(String::class.java)
            val comment = snapshot.child("comment").getValue(String::class.java)

            val ratingAny = snapshot.child("rating").value
            val rating = when (ratingAny) {
                is Number -> ratingAny.toFloat()
                is String -> ratingAny.toFloatOrNull() ?: 0f
                else -> 0f
            }

            val tsAny = snapshot.child("timestamp").value
            val timestamp = when (tsAny) {
                is Number -> tsAny.toLong()
                is String -> tsAny.toLongOrNull() ?: 0L
                else -> 0L
            }

            return Review(userId, name, rating, comment, timestamp)
        }
    }
}
