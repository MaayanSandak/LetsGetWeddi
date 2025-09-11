package com.example.letsgetweddi.utils

import android.net.Uri
import android.widget.ImageView
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso

object Images {

    private const val FALLBACK = android.R.drawable.ic_menu_report_image

    /**
     * Loads an image from:
     *  - http(s):// full URL
     *  - gs://bucket/path
     *  - raw Firebase Storage object path (e.g. "suppliers/sup_001/cover.jpg")
     */
    fun loadInto(view: ImageView, urlOrPath: String?, placeholderRes: Int = FALLBACK) {
        val target = urlOrPath?.trim().orEmpty()
        if (target.isBlank()) {
            view.setImageResource(placeholderRes)
            return
        }

        if (target.startsWith("http://", true) || target.startsWith("https://", true)) {
            Picasso.get()
                .load(target)
                .placeholder(placeholderRes)
                .error(placeholderRes)
                .fit()
                .centerCrop()
                .into(view)
            return
        }

        try {
            val storage = FirebaseStorage.getInstance()
            val ref = if (target.startsWith("gs://", true)) {
                storage.getReferenceFromUrl(target)
            } else {
                storage.getReference(target.removePrefix("/"))
            }

            ref.downloadUrl
                .addOnSuccessListener { uri: Uri ->
                    Picasso.get()
                        .load(uri)
                        .placeholder(placeholderRes)
                        .error(placeholderRes)
                        .fit()
                        .centerCrop()
                        .into(view)
                }
                .addOnFailureListener {
                    view.setImageResource(placeholderRes)
                }
        } catch (_: Throwable) {
            view.setImageResource(placeholderRes)
        }
    }
}
