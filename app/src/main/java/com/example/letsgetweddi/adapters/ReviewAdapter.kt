package com.example.letsgetweddi.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.letsgetweddi.R

class ReviewAdapter(
    private val items: MutableList<Pair<String, String>>
) : RecyclerView.Adapter<ReviewAdapter.VH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_review, parent, false) as ViewGroup
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val (author, content) = items[position]
        holder.author.text = if (author.isBlank()) holder.itemView.context.getString(R.string.anonymous) else author
        holder.content.text = content
    }

    override fun getItemCount(): Int = items.size

    inner class VH(root: ViewGroup) : RecyclerView.ViewHolder(root) {
        val author: TextView = root.findViewById(R.id.textAuthor)
        val content: TextView = root.findViewById(R.id.textContent)
    }
}
