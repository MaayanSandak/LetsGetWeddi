package com.example.letsgetweddi.model

enum class Category(
    val id: String,
    val title: String
) {
    DJS("djs", "DJs"),
    PHOTOGRAPHERS("photographers", "Photographers"),
    DRESSES("dresses", "Bridal Dresses"),
    SUITS("suits", "Groom Suits"),
    HAIR_MAKEUP("hair_makeup", "Hair & Makeup"),
    HALLS("halls", "Halls & Gardens");
}
