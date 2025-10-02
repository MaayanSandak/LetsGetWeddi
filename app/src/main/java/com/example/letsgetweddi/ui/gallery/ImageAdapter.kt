package com.example.letsgetweddi.ui.gallery

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.letsgetweddi.R
import com.squareup.picasso.Picasso

class ImageAdapter(private val items: List<String>) :
    RecyclerView.Adapter<ImageAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val image: ImageView = v.findViewById(R.id.image)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_gallery_image, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val url = items[position]
        Picasso.get()
            .load(url)
            .placeholder(android.R.drawable.ic_menu_report_image)
            .error(android.R.drawable.ic_menu_report_image)
            .fit()
            .centerCrop()
            .into(holder.image)
    }
}
