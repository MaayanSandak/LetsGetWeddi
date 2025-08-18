package com.example.letsgetweddi.adapters

import android.content.Context
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
import com.example.letsgetweddi.data.FirebaseRefs
import com.example.letsgetweddi.model.Supplier
import com.example.letsgetweddi.ui.ProviderDetailsActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso

class SupplierAdapter(
    private val items: MutableList<Supplier>,
    private val isFavorites: Boolean = false
) : RecyclerView.Adapter<SupplierAdapter.VH>() {

    companion object {
        private const val STORAGE_BUCKET = "gs://letsgetweddi.firebasestorage.app"
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_supplier, parent, false)
        return VH(view as ViewGroup)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.bind(holder.itemView.context, item, isFavorites) { pos ->
            if (pos != RecyclerView.NO_POSITION) notifyItemChanged(pos)
        }
        holder.itemView.setOnClickListener {
            val ctx = holder.itemView.context
            val intent = Intent(ctx, ProviderDetailsActivity::class.java)
            intent.putExtra("supplierId", item.id ?: "")
            ctx.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = items.size

    inner class VH(private val root: ViewGroup) : RecyclerView.ViewHolder(root) {
        private val image: ImageView = root.findViewById(R.id.imageSupplier)
        private val name: TextView = root.findViewById(R.id.textName)
        private val location: TextView = root.findViewById(R.id.textLocation)
        private val subtitle: TextView = root.findViewById(R.id.textSubtitle)
        private val fav: ImageButton = root.findViewById(R.id.buttonFavorite)

        private val btnCall: ImageButton = root.findViewById(R.id.buttonCall)
        private val btnChat: ImageButton = root.findViewById(R.id.buttonChat)
        private val btnGallery: ImageButton = root.findViewById(R.id.buttonGallery)
        private val btnCalendar: ImageButton = root.findViewById(R.id.buttonCalendar)

        fun bind(ctx: Context, s: Supplier, isFavScreen: Boolean, onFavToggled: (Int) -> Unit) {
            name.text = s.name ?: "(No name)"
            location.text = s.location ?: ""
            subtitle.text = ctx.getString(R.string.see_details)

            // Image can be a direct URL or a Storage path (gs:// or plain path)
            val raw = s.imageUrl.orEmpty().trim()
            if (raw.isBlank()) {
                image.setImageResource(R.drawable.rounded_card_placeholder)
            } else if (raw.startsWith("http", ignoreCase = true)) {
                // Direct URL
                Picasso.get()
                    .load(raw)
                    .placeholder(R.drawable.rounded_card_placeholder)
                    .fit()
                    .centerCrop()
                    .into(image)
            } else {
                // Storage path
                val storage = FirebaseStorage.getInstance()
                val ref = when {
                    raw.startsWith("gs://", ignoreCase = true) ->
                        storage.getReferenceFromUrl(raw)

                    raw.startsWith("/", ignoreCase = true) ->
                        storage.getReferenceFromUrl("$STORAGE_BUCKET$raw")

                    else ->
                        storage.reference.child(raw) // relative path like "suppliers/img.jpg"
                }

                ref.downloadUrl
                    .addOnSuccessListener { uri ->
                        Picasso.get()
                            .load(uri)
                            .placeholder(R.drawable.rounded_card_placeholder)
                            .fit()
                            .centerCrop()
                            .into(image)
                    }
                    .addOnFailureListener {
                        image.setImageResource(R.drawable.rounded_card_placeholder)
                    }
            }

            // Favorites
            fav.setImageResource(if (isFavScreen) R.drawable.ic_favorite else R.drawable.ic_favorite_border)
            fav.contentDescription = ctx.getString(
                if (isFavScreen) R.string.remove_from_favorites else R.string.add_to_favorites
            )
            fav.setOnClickListener {
                val user = FirebaseAuth.getInstance().currentUser ?: return@setOnClickListener
                val ref = FirebaseRefs.favorites(user.uid)
                val id = s.id ?: return@setOnClickListener

                if (isFavScreen) {
                    ref.child(id).removeValue()
                    val pos = bindingAdapterPosition
                    if (pos != RecyclerView.NO_POSITION) {
                        items.removeAt(pos)
                        notifyItemRemoved(pos)
                    }
                } else {
                    ref.child(id).setValue(s)
                    fav.setImageResource(R.drawable.ic_favorite)
                    fav.contentDescription = ctx.getString(R.string.remove_from_favorites)
                    onFavToggled(bindingAdapterPosition)
                }
            }

            val hasPhone = !s.phone.isNullOrBlank()
            btnCall.visibility = if (hasPhone) View.VISIBLE else View.GONE
            btnChat.visibility = View.VISIBLE
            btnGallery.visibility = View.VISIBLE
            btnCalendar.visibility = View.VISIBLE

            btnCall.setOnClickListener {
                val phone = s.phone ?: return@setOnClickListener
                val dial = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                ctx.startActivity(dial)
            }
            btnChat.setOnClickListener {
                val peer = s.id ?: return@setOnClickListener
                val intent = Intent(ctx, com.example.letsgetweddi.ui.chat.ChatActivity::class.java)
                intent.putExtra("peerId", peer)
                ctx.startActivity(intent)
            }
            btnGallery.setOnClickListener {
                val intent = Intent(ctx, ProviderDetailsActivity::class.java)
                    .putExtra("supplierId", s.id ?: "")
                ctx.startActivity(intent)
            }
            btnCalendar.setOnClickListener {
                val intent = Intent(ctx, ProviderDetailsActivity::class.java)
                    .putExtra("supplierId", s.id ?: "")
                ctx.startActivity(intent)
            }
        }
    }
}
