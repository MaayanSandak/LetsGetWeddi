package com.example.letsgetweddi.adapters

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.letsgetweddi.R
import com.example.letsgetweddi.model.Supplier
import com.squareup.picasso.Picasso

class SupplierAdapter(
    private val items: List<Supplier>,
    private val isFavorites: Boolean = false // <-- default fixes old call sites
) : RecyclerView.Adapter<SupplierAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val root: View = v.findViewById(R.id.itemRoot)
        val image: ImageView = v.findViewById(R.id.imageSupplier)
        val fav: ImageButton = v.findViewById(R.id.buttonFavorite)
        val name: TextView = v.findViewById(R.id.textName)
        val location: TextView = v.findViewById(R.id.textLocation)
        val subtitle: TextView = v.findViewById(R.id.textSubtitle)
        val btnCall: ImageButton = v.findViewById(R.id.buttonCall)
        val btnChat: ImageButton = v.findViewById(R.id.buttonChat)
        val btnGallery: ImageButton = v.findViewById(R.id.buttonGallery)
        val btnCalendar: ImageButton = v.findViewById(R.id.buttonCalendar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_supplier, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val s = items[position]

        holder.name.text = s.name ?: ""
        holder.location.text = s.location ?: ""
        holder.subtitle.text = s.description ?: ""

        val url = s.imageUrl?.trim().orEmpty()
        if (url.isNotBlank()) {
            Picasso.get()
                .load(url)
                .placeholder(android.R.drawable.ic_menu_report_image)
                .fit()
                .centerCrop()
                .into(holder.image)
        } else {
            holder.image.setImageResource(android.R.drawable.ic_menu_report_image)
        }

        // Call
        holder.btnCall.setOnClickListener {
            val phone = s.phone?.trim().orEmpty()
            if (phone.isNotEmpty()) {
                val uri = Uri.parse("tel:$phone")
                it.context.startActivity(Intent(Intent.ACTION_DIAL, uri))
            }
        }

        // Chat
        holder.btnChat.setOnClickListener {
            val ctx = it.context
            val intent = Intent(ctx, com.example.letsgetweddi.ui.chat.ChatActivity::class.java)
                .putExtra("otherUserId", s.id ?: "")
                .putExtra("otherUserName", s.name ?: "")
            ctx.startActivity(intent)
        }

        // Gallery
        holder.btnGallery.setOnClickListener {
            val ctx = it.context
            val intent =
                Intent(ctx, com.example.letsgetweddi.ui.gallery.GalleryViewActivity::class.java)
                    .putExtra("supplierId", s.id ?: "")
            ctx.startActivity(intent)
        }

        // Calendar
        holder.btnCalendar.setOnClickListener {
            val ctx = it.context
            val intent = Intent(
                ctx,
                com.example.letsgetweddi.ui.supplier.SupplierCalendarActivity::class.java
            )
                .putExtra("supplierId", s.id ?: "")
            ctx.startActivity(intent)
        }

        // Favorite icon visual state only
        holder.fav.setImageResource(
            if (isFavorites) R.drawable.ic_favorite else R.drawable.ic_favorite_border
        )
    }
}
