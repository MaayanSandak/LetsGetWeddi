package com.example.letsgetweddi.model

import androidx.annotation.DrawableRes
import com.example.letsgetweddi.R

enum class Category(
    val id: String,
    val title: String,
    @DrawableRes val headerDrawable: Int
) {
    DJS("djs", "DJs", R.drawable.header_djs),
    PHOTOGRAPHERS("photographers", "Photographers", R.drawable.header_photographers),
    DRESSES("dresses", "Bridal Dresses", R.drawable.header_dresses),
    SUITS("suits", "Groom Suits", R.drawable.header_suits),
    HAIR_MAKEUP("hair_makeup", "Hair & Makeup", R.drawable.header_hair_makeup),
    HALLS("halls", "Halls & Gardens", R.drawable.header_halls);

    companion object {
        fun fromId(id: String): Category? = entries.firstOrNull { it.id == id }
    }
}
