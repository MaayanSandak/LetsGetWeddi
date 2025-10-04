package com.example.letsgetweddi.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.letsgetweddi.R
import com.example.letsgetweddi.model.Review
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReviewAdapter(
    private val items: MutableList<Review>
) : RecyclerView.Adapter<ReviewAdapter.VH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_review, parent, false) as ViewGroup
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val review = items[position]
        val context = holder.itemView.context

        holder.author.text = if (review.name.isNullOrBlank()) {
            context.getString(R.string.anonymous)
        } else {
            review.name
        }

        holder.content.text = review.comment ?: ""
        holder.ratingBar.rating = review.rating
        holder.date.text = formatDate(review.timestamp)
    }

    override fun getItemCount(): Int = items.size

    fun submitList(newItems: List<Review>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    inner class VH(root: ViewGroup) : RecyclerView.ViewHolder(root) {
        val author: TextView = root.findViewById(R.id.textAuthor)
        val content: TextView = root.findViewById(R.id.textContent)
        val ratingBar: RatingBar = root.findViewById(R.id.ratingBar)
        val date: TextView = root.findViewById(R.id.textDate)
    }

    private fun formatDate(timestamp: Long): String {
        if (timestamp <= 0L) return ""
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

}
