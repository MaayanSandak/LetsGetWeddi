package com.example.letsgetweddi.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.letsgetweddi.R
import com.example.letsgetweddi.databinding.ItemHomeFavoriteBinding

class HomeFavoritesAdapter(
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<HomeFavoritesAdapter.VH>() {

    private val items = mutableListOf<HomeFavoriteItem>()

    fun submit(list: List<HomeFavoriteItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemHomeFavoriteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b, onClick)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position], position == itemCount - 1)
    }

    override fun getItemCount(): Int = items.size

    class VH(
        private val b: ItemHomeFavoriteBinding,
        private val onClick: (String) -> Unit
    ) : RecyclerView.ViewHolder(b.root) {

        fun bind(item: HomeFavoriteItem, isLast: Boolean) {
            b.textName.text = item.supplierName
            if (!item.category.isNullOrBlank()) {
                b.textSubtitle.visibility = View.VISIBLE
                b.textSubtitle.text = item.category
            } else {
                b.textSubtitle.visibility = View.GONE
            }

            if (!item.imageUrl.isNullOrBlank()) {
                Glide.with(b.icon).load(item.imageUrl).centerCrop().into(b.icon)
            } else {
                b.icon.setImageResource(R.mipmap.ic_launcher_round)
            }

            if (b.divider != null) {
                b.divider.visibility = if (isLast) View.GONE else View.VISIBLE
            }

            b.root.setOnClickListener { onClick(item.supplierId) }
        }
    }
}
