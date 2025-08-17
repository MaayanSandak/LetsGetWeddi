package com.example.letsgetweddi.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
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
import com.squareup.picasso.Picasso

class SupplierAdapter(
    private val items: MutableList<Supplier>,
    private val isFavorites: Boolean = false
) : RecyclerView.Adapter<SupplierAdapter.VH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_supplier, parent, false)
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

        fun bind(ctx: Context, s: Supplier, isFavScreen: Boolean, onFavToggled: (Int) -> Unit) {
            name.text = s.name ?: ""
            location.text = s.location ?: ""
            subtitle.text = ctx.getString(R.string.see_details)

            val url = s.imageUrl.orEmpty()
            if (url.isNotBlank()) {
                Picasso.get()
                    .load(url)
                    .placeholder(R.drawable.rounded_card_placeholder)
                    .fit()
                    .centerCrop()
                    .into(image)
            } else {
                image.setImageResource(R.drawable.rounded_card_placeholder)
            }

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
        }
    }
}
