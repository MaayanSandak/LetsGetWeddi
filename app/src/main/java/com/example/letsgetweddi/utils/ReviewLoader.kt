package com.example.letsgetweddi.utils

import android.widget.TextView
import com.example.letsgetweddi.adapters.ReviewAdapter
import com.example.letsgetweddi.model.Review
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Locale

fun loadReviews(supplierId: String, ratingTextView: TextView, reviewAdapter: ReviewAdapter) {
    val ref = FirebaseDatabase.getInstance().getReference("reviews").child(supplierId)
    ref.addListenerForSingleValueEvent(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val reviews = mutableListOf<Pair<String, String>>()
            var totalRating = 0f
            var count = 0

            for (child in snapshot.children) {
                val r = child.getValue(Review::class.java)
                if (r != null) {
                    val name = r.name ?: ""
                    val comment = r.comment ?: ""
                    reviews.add(name to comment)
                    totalRating += r.rating
                    count++
                }
            }

            val avg = if (count > 0) totalRating / count else 0f
            ratingTextView.text = String.format(Locale.getDefault(), "%.1f", avg)
            reviewAdapter.submitList(reviews)
        }

        override fun onCancelled(error: DatabaseError) {}
    })
}
