package com.example.letsgetweddi.ui.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.letsgetweddi.data.Conversation
import com.example.letsgetweddi.databinding.ItemConversationBinding

class ConversationsAdapter(
    private val onClick: (Conversation) -> Unit
) : RecyclerView.Adapter<ConversationsAdapter.VH>() {

    private val items = mutableListOf<Conversation>()

    fun submitList(list: List<Conversation>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemConversationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class VH(private val b: ItemConversationBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(c: Conversation) {
            b.textName.text = c.otherUserName
            b.textLastMsg.text = c.lastText
            b.root.setOnClickListener { onClick(c) }
        }
    }
}
