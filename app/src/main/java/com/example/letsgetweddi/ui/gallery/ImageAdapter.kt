package com.example.letsgetweddi.ui.gallery

import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso

class ImageAdapter(private val items: List<String>) : RecyclerView.Adapter<ImageAdapter.VH>() {

    class VH(val imageView: ImageView) : RecyclerView.ViewHolder(imageView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val span = 3
        val parentWidth =
            parent.measuredWidth.takeIf { it > 0 } ?: parent.resources.displayMetrics.widthPixels
        val size = parentWidth / span
        val iv = ImageView(parent.context)
        iv.layoutParams = FrameLayout.LayoutParams(size, size)
        iv.scaleType = ImageView.ScaleType.CENTER_CROP
        return VH(iv)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val url = items[position]
        if (url.startsWith("android.resource://")) {
            holder.imageView.setImageURI(android.net.Uri.parse(url))
        } else {
            Picasso.get().load(url).fit().centerCrop().into(holder.imageView)
        }
    }

    override fun getItemCount(): Int = items.size
}
