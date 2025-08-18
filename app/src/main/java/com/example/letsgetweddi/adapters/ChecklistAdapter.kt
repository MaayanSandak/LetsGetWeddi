package com.example.letsgetweddi.adapters

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.letsgetweddi.R
import com.example.letsgetweddi.model.ChecklistItem
import com.google.firebase.database.FirebaseDatabase

class ChecklistAdapter(
    private val items: MutableList<ChecklistItem>,
    private val uid: String
) : RecyclerView.Adapter<ChecklistAdapter.VH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_checklist, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position]) { adapterPos, newState ->
            // update local model first (no notify needed; checkbox already reflects state)
            val curr = items[adapterPos]
            items[adapterPos] = curr.copy(isDone = newState)

            // persist to DB
            val id = curr.id ?: return@bind
            FirebaseDatabase.getInstance()
                .getReference("checklist")
                .child(uid)
                .child(id)
                .child("isDone")
                .setValue(newState)
        }
    }

    override fun getItemCount(): Int = items.size

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val check: CheckBox = itemView.findViewById(R.id.checkbox)
        private val text: TextView = itemView.findViewById(R.id.text)

        fun bind(item: ChecklistItem, onToggle: (Int, Boolean) -> Unit) {
            check.setOnCheckedChangeListener(null)

            val done = item.isDone == true
            check.isChecked = done
            text.text = item.task ?: ""
            applyStrike(done)

            // Only the CheckBox handles clicks
            check.setOnCheckedChangeListener { _, isChecked ->
                applyStrike(isChecked)
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) onToggle(pos, isChecked)
            }
        }

        private fun applyStrike(done: Boolean) {
            text.paintFlags = if (done)
                (text.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG)
            else
                (text.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv())
        }
    }
}
