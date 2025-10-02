package com.example.letsgetweddi.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.example.letsgetweddi.R
import com.example.letsgetweddi.data.FirebaseRefs
import com.example.letsgetweddi.model.Supplier
import com.example.letsgetweddi.ui.gallery.GalleryViewActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso

class SupplierAdapter(
    private val items: MutableList<Supplier>,
    private val isFavorites: Boolean = false
) : RecyclerView.Adapter<SupplierAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val image: ImageView = v.findViewById(R.id.imageSupplier)
        val name: TextView = v.findViewById(R.id.textName)
        val location: TextView = v.findViewById(R.id.textLocation)
        val subtitle: TextView = v.findViewById(R.id.textSubtitle)
        val btnCall: ImageButton = v.findViewById(R.id.buttonCall)
        val btnChat: ImageButton = v.findViewById(R.id.buttonChat)
        val btnGallery: ImageButton = v.findViewById(R.id.buttonGallery)
        val btnCalendar: ImageButton = v.findViewById(R.id.buttonCalendar)
        val btnFav: ImageButton = v.findViewById(R.id.buttonFavorite)
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
        holder.subtitle.text =
            s.description ?: holder.itemView.context.getString(R.string.see_details)

        val placeholder = android.R.drawable.ic_menu_report_image
        val url = (s.imageUrl ?: "").trim()
        if (url.startsWith("gs://")) {
            val ref = FirebaseStorage.getInstance().getReferenceFromUrl(url)
            ref.downloadUrl.addOnSuccessListener { uri ->
                Picasso.get()
                    .load(uri.toString())
                    .placeholder(placeholder)
                    .error(placeholder)
                    .fit()
                    .centerCrop()
                    .into(holder.image)
            }.addOnFailureListener {
                holder.image.setImageResource(placeholder)
            }
        } else if (url.isNotEmpty()) {
            Picasso.get()
                .load(url)
                .placeholder(placeholder)
                .error(placeholder)
                .fit()
                .centerCrop()
                .into(holder.image)
        } else {
            holder.image.setImageResource(placeholder)
        }

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null && !s.id.isNullOrBlank()) {
            FirebaseRefs.favorite(uid, s.id!!).get().addOnSuccessListener { snap ->
                holder.btnFav.setImageResource(
                    if (snap.exists()) R.drawable.ic_favorite else R.drawable.ic_favorite_border
                )
            }
        } else {
            holder.btnFav.setImageResource(R.drawable.ic_favorite_border)
        }

        holder.btnCall.setOnClickListener {
            val phone = s.phone?.trim().orEmpty()
            if (phone.isNotEmpty()) {
                val uri = "tel:${phone}".toUri()
                it.context.startActivity(Intent(Intent.ACTION_DIAL, uri))
            }
        }

        holder.btnChat.setOnClickListener {
            if (!s.id.isNullOrBlank()) {
                val ctx = it.context
                val intent = Intent(ctx, com.example.letsgetweddi.ui.chat.ChatActivity::class.java)
                    .putExtra("otherUserId", s.id)
                    .putExtra("otherUserName", s.name ?: "")
                ctx.startActivity(intent)
            }
        }

        holder.btnGallery.setOnClickListener {
            if (!s.id.isNullOrBlank()) {
                val ctx = it.context
                ctx.startActivity(
                    Intent(ctx, GalleryViewActivity::class.java)
                        .putExtra("supplierId", s.id)
                )
            }
        }

        holder.btnCalendar.setOnClickListener {
            if (!s.id.isNullOrBlank()) {
                val ctx = it.context
                ctx.startActivity(
                    Intent(
                        ctx,
                        com.example.letsgetweddi.ui.supplier.AvailabilityActivity::class.java
                    )
                        .putExtra("supplierId", s.id)
                )
            }
        }

        holder.btnFav.setOnClickListener { v ->
            val user = FirebaseAuth.getInstance().currentUser ?: return@setOnClickListener
            val supId = s.id ?: return@setOnClickListener
            val ref = FirebaseRefs.favorite(user.uid, supId)
            ref.get().addOnSuccessListener { snap ->
                if (snap.exists()) {
                    ref.removeValue()
                    holder.btnFav.setImageResource(R.drawable.ic_favorite_border)
                } else {
                    ref.setValue(s)
                    holder.btnFav.setImageResource(R.drawable.ic_favorite)
                }
            }
        }

        holder.itemView.setOnClickListener {
            if (!s.id.isNullOrBlank()) {
                val ctx = it.context
                ctx.startActivity(
                    Intent(ctx, com.example.letsgetweddi.ui.ProviderDetailsActivity::class.java)
                        .putExtra("supplierId", s.id)
                )
            }
        }
    }

    fun submitList(newItems: List<Supplier>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
