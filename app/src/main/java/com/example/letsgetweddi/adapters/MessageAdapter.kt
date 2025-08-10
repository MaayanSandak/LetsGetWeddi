package com.example.letsgetweddi.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.letsgetweddi.databinding.ItemMessageLeftBinding
import com.example.letsgetweddi.databinding.ItemMessageRightBinding
import com.example.letsgetweddi.model.Message

class MessageAdapter(
    private val messages: List<Message>,
    private val currentUserId: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val VIEW_TYPE_LEFT = 0
    private val VIEW_TYPE_RIGHT = 1

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].senderId == currentUserId) VIEW_TYPE_RIGHT else VIEW_TYPE_LEFT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_RIGHT) {
            val binding = ItemMessageRightBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            RightViewHolder(binding)
        } else {
            val binding = ItemMessageLeftBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            LeftViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        if (holder is RightViewHolder) {
            holder.bind(message)
        } else if (holder is LeftViewHolder) {
            holder.bind(message)
        }
    }

    override fun getItemCount(): Int = messages.size

    inner class RightViewHolder(private val binding: ItemMessageRightBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            binding.textMessageRight.text = message.text
        }
    }

    inner class LeftViewHolder(private val binding: ItemMessageLeftBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            binding.textMessageLeft.text = message.text
        }
    }
}
