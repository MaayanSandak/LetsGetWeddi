package com.example.letsgetweddi.ui.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.letsgetweddi.data.ChatMessage
import com.example.letsgetweddi.databinding.ItemMessageMeBinding
import com.example.letsgetweddi.databinding.ItemMessageOtherBinding

class ChatAdapter(private val myId: String) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val data = mutableListOf<ChatMessage>()

    private companion object {
        const val TYPE_ME = 1
        const val TYPE_OTHER = 2
    }

    fun submit(list: List<ChatMessage>) {
        data.clear()
        data.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = data.size

    override fun getItemViewType(position: Int): Int {
        val m = data[position]
        return if (m.senderId == myId) TYPE_ME else TYPE_OTHER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_ME) {
            val b = ItemMessageMeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            MeVH(b)
        } else {
            val b =
                ItemMessageOtherBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            OtherVH(b)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val m = data[position]
        when (holder) {
            is MeVH -> holder.bind(m)
            is OtherVH -> holder.bind(m)
        }
    }

    class MeVH(private val b: ItemMessageMeBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(m: ChatMessage) {
            b.textMsg.text = m.text
        }
    }

    class OtherVH(private val b: ItemMessageOtherBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(m: ChatMessage) {
            b.textMsg.text = m.text
        }
    }
}
