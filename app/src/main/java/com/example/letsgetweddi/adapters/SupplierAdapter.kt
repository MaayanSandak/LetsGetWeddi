package com.example.letsgetweddi.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.letsgetweddi.R
import com.example.letsgetweddi.databinding.ItemSupplierBinding
import com.example.letsgetweddi.model.Supplier
import com.example.letsgetweddi.ui.ProviderDetailsActivity
import com.example.letsgetweddi.ui.gallery.GalleryViewActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SupplierAdapter(
    private val items: MutableList<Supplier> = mutableListOf()
) : RecyclerView.Adapter<SupplierAdapter.VH>() {

    inner class VH(val binding: ItemSupplierBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemSupplierBinding.inflate(inflater, parent, false)
        return VH(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val s = items[position]
        val b = holder.binding

        // Texts
        b.textName.text = s.name.orEmpty()
        b.textLocation.text = s.location.orEmpty()
        b.textSubtitle.text =
            s.description ?: holder.itemView.context.getString(R.string.see_details)

        // Image
        val url = s.imageUrl
        if (!url.isNullOrBlank()) {
            Glide.with(b.imageSupplier).load(url).into(b.imageSupplier)
        } else {
            b.imageSupplier.setImageDrawable(null)
        }

        // Favorites (heart icon)
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val favRef: DatabaseReference? =
            if (uid != null && !s.id.isNullOrBlank())
                FirebaseDatabase.getInstance().reference
                    .child("favorites").child(uid).child(s.id!!)
            else
                null

        fun setHeart(fav: Boolean) {
            b.buttonFavorite.setImageResource(
                if (fav) R.drawable.ic_favorite else R.drawable.ic_favorite_border
            )
            b.buttonFavorite.isSelected = fav
        }

        if (favRef != null) {
            favRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val exists =
                        snapshot.exists() && snapshot.getValue(Boolean::class.java) != false
                    setHeart(exists)
                }

                override fun onCancelled(error: DatabaseError) {}
            })
        } else {
            setHeart(false)
        }

        b.buttonFavorite.setOnClickListener {
            if (favRef == null) return@setOnClickListener
            val now = !b.buttonFavorite.isSelected
            setHeart(now)
            if (now) favRef.setValue(true) else favRef.removeValue()
        }

        // Open gallery
        b.buttonGallery.setOnClickListener {
            val ctx = it.context
            val intent = Intent(ctx, GalleryViewActivity::class.java)
            intent.putStringArrayListExtra("images", ArrayList(s.images ?: emptyList()))
            intent.putExtra("title", s.name ?: "Gallery")
            ctx.startActivity(intent)
        }

        // Open details card (optional; keep if you already had this behavior)
        b.itemRoot.setOnClickListener {
            val ctx = it.context
            val i = Intent(ctx, ProviderDetailsActivity::class.java)
            i.putExtra("supplierId", s.id)
            ctx.startActivity(i)
        }
    }

    fun submit(list: List<Supplier>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }
}
