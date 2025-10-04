package com.example.letsgetweddi.adapters

import android.view.LayoutInflater
import android.view.View
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
        val ctx = holder.itemView.context

        val authorName = review.name?.takeIf { it.isNotBlank() }
            ?: ctx.getString(R.string.anonymous)
        holder.author.text = authorName

        val text = review.comment?.trim().orEmpty()
        if (text.isEmpty()) {
            holder.content.visibility = View.GONE
        } else {
            holder.content.visibility = View.VISIBLE
            holder.content.text = text
        }

        holder.ratingBar.rating = review.rating.coerceIn(0f, 5f)
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
