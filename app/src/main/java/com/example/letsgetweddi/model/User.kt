package com.example.letsgetweddi.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "client", // "client" or "supplier"
    val supplierId: String? = null // if role == "supplier", link to Suppliers/{id}
)
