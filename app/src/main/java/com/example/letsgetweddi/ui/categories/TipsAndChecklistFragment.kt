package com.example.letsgetweddi.ui.categories

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.appcompat.widget.SearchView
import com.example.letsgetweddi.adapters.ChecklistAdapter
import com.example.letsgetweddi.adapters.TipAdapter
import com.example.letsgetweddi.databinding.FragmentTipsAndChecklistBinding
import com.example.letsgetweddi.model.ChecklistItem
import com.example.letsgetweddi.model.Tip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class TipsAndChecklistFragment : Fragment() {

    private lateinit var binding: FragmentTipsAndChecklistBinding
    private lateinit var database: DatabaseReference

    private lateinit var checklistAdapter: ChecklistAdapter
    private lateinit var tipAdapter: TipAdapter

    private val allChecklistItems = mutableListOf<ChecklistItem>()
    private val filteredChecklistItems = mutableListOf<ChecklistItem>()

    private val allTips = mutableListOf<Tip>()
    private val filteredTips = mutableListOf<Tip>()

    private var userRole: String = "client"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentTipsAndChecklistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        database = FirebaseDatabase.getInstance().reference

        database.child("Users").child(userId).child("role")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    userRole = snapshot.getValue(String::class.java) ?: "client"
                    applyGating()
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        binding.recyclerChecklist.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerTips.layoutManager = LinearLayoutManager(requireContext())

        checklistAdapter = ChecklistAdapter(filteredChecklistItems, userId)
        tipAdapter = TipAdapter(filteredTips)

        binding.recyclerChecklist.adapter = checklistAdapter
        binding.recyclerTips.adapter = tipAdapter

        binding.buttonAddTask.setOnClickListener {
            val input = EditText(requireContext()).apply { hint = "Enter your task" }
            AlertDialog.Builder(requireContext())
                .setTitle("New Task")
                .setView(input)
                .setPositiveButton("Add") { _, _ ->
                    val taskText = input.text.toString()
                    if (taskText.isNotEmpty()) {
                        val taskId = database.child("checklist").child(userId).push().key ?: return@setPositiveButton
                        val newTask = ChecklistItem(id = taskId, task = taskText, isDone = false)
                        database.child("checklist").child(userId).child(taskId).setValue(newTask)
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.buttonAddTip.setOnClickListener {
            if (userRole != "supplier") return@setOnClickListener

            val container = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(32, 16, 32, 16)
            }
            val titleEt = EditText(requireContext()).apply { hint = "Tip title" }
            val contentEt = EditText(requireContext()).apply { hint = "Tip content" }
            container.addView(titleEt)
            container.addView(contentEt)

            AlertDialog.Builder(requireContext())
                .setTitle("New Tip")
                .setView(container)
                .setPositiveButton("Add") { _, _ ->
                    val title = titleEt.text.toString()
                    val content = contentEt.text.toString()
                    if (title.isNotEmpty() && content.isNotEmpty()) {
                        val tipId = database.child("tips").push().key ?: return@setPositiveButton
                        val newTip = Tip(title = title, content = content)
                        database.child("tips").child(tipId).setValue(newTip)
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        loadChecklist(userId)
        loadTips()

        binding.searchViewChecklist.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = true
            override fun onQueryTextChange(newText: String?): Boolean {
                filterChecklist(newText.orEmpty()); return true
            }
        })
        binding.searchViewTips.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = true
            override fun onQueryTextChange(newText: String?): Boolean {
                filterTips(newText.orEmpty()); return true
            }
        })
    }

    private fun applyGating() {
        binding.buttonAddTip.visibility = if (userRole == "supplier") View.VISIBLE else View.GONE
    }

    private fun loadChecklist(userId: String) {
        database.child("checklist").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    allChecklistItems.clear()
                    for (c in snapshot.children) {
                        val item = c.getValue(ChecklistItem::class.java)
                        val id = c.key
                        if (item != null && id != null) allChecklistItems.add(item.copy(id = id))
                    }
                    filterChecklist("")
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun loadTips() {
        database.child("tips")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    allTips.clear()
                    for (c in snapshot.children) {
                        val tip = c.getValue(Tip::class.java)
                        if (tip != null) allTips.add(tip)
                    }
                    filterTips("")
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun filterChecklist(query: String) {
        val q = query.lowercase()
        filteredChecklistItems.clear()
        filteredChecklistItems.addAll(allChecklistItems.filter { it.task?.lowercase()?.contains(q) == true })
        checklistAdapter.notifyDataSetChanged()
    }

    private fun filterTips(query: String) {
        val q = query.lowercase()
        filteredTips.clear()
        filteredTips.addAll(allTips.filter { it.title?.lowercase()?.contains(q) == true || it.content?.lowercase()?.contains(q) == true })
        tipAdapter.notifyDataSetChanged()
    }
}
