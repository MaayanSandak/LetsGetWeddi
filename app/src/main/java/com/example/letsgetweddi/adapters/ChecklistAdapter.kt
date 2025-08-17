package com.example.letsgetweddi.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.letsgetweddi.databinding.ItemChecklistBinding
import com.example.letsgetweddi.model.ChecklistItem
import com.google.firebase.database.FirebaseDatabase

class ChecklistAdapter(
    private val items: List<ChecklistItem>,
    private val userId: String
) : RecyclerView.Adapter<ChecklistAdapter.CheckListViewHolder>() {

    inner class CheckListViewHolder(private val binding: ItemChecklistBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ChecklistItem) {
            binding.checkboxTask.text = item.task
            binding.checkboxTask.isChecked = item.isDone

            binding.checkboxTask.setOnCheckedChangeListener { _, isChecked ->
                val itemRef = FirebaseDatabase.getInstance()
                    .getReference("checklist")
                    .child(userId)
                    .child(item.id ?: return@setOnCheckedChangeListener)

                itemRef.child("isDone").setValue(isChecked)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CheckListViewHolder {
        val binding = ItemChecklistBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CheckListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CheckListViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
