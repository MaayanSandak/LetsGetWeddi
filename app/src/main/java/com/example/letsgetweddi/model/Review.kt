package com.example.letsgetweddi.model

data class Review(
    val userId: String? = null,
    val name: String? = null,
    val rating: Float = 0f,
    val comment: String? = null,
    val timestamp: Long = 0
)