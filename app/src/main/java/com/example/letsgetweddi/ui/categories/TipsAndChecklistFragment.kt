package com.example.letsgetweddi.ui.categories

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
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
    private lateinit var uid: String

    private val defaultChecklist = listOf(
        "Define budget",
        "Create guest list",
        "Pick a date",
        "Book venue",
        "Book photographer",
        "Book DJ/band",
        "Choose catering",
        "Choose dress/suit",
        "Hire hair & makeup",
        "Send invitations"
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentTipsAndChecklistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        database = FirebaseDatabase.getInstance().reference

        binding.recyclerChecklist.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerTips.layoutManager = LinearLayoutManager(requireContext())

        checklistAdapter = ChecklistAdapter(filteredChecklistItems, uid)
        tipAdapter = TipAdapter(filteredTips)

        binding.recyclerChecklist.adapter = checklistAdapter
        binding.recyclerTips.adapter = tipAdapter

        // load role (for gating if you want)
        database.child("Users").child(uid).child("role")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    userRole = snapshot.getValue(String::class.java) ?: "client"
                    applyGating()
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        // checklist live (seed defaults once if empty)
        val checklistRef = database.child("checklist").child(uid)
        checklistRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.hasChildren()) {
                    val updates = HashMap<String, Any?>()
                    defaultChecklist.forEach { taskText ->
                        val id = checklistRef.push().key ?: return@forEach
                        updates[id] = ChecklistItem(id = id, task = taskText, isDone = false)
                    }
                    checklistRef.updateChildren(updates)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
        checklistRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allChecklistItems.clear()
                for (c in snapshot.children) {
                    val item = c.getValue(ChecklistItem::class.java)
                    val id = c.key
                    if (item != null && id != null) allChecklistItems.add(item.copy(id = id))
                }
                filterChecklist(binding.searchViewChecklist.query?.toString().orEmpty())
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        // tips per user (live)
        val tipsRef = database.child("tips").child(uid)
        tipsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allTips.clear()
                for (c in snapshot.children) {
                    val tip = c.getValue(Tip::class.java)
                    if (tip != null) allTips.add(tip)
                }
                filterTips(binding.searchViewTips.query?.toString().orEmpty())
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        binding.buttonAddTask.setOnClickListener {
            val input = EditText(requireContext()).apply { hint = "Enter your task" }
            AlertDialog.Builder(requireContext())
                .setTitle("New Task")
                .setView(input)
                .setPositiveButton("Add") { _, _ ->
                    val taskText = input.text.toString().trim()
                    if (taskText.isNotEmpty()) {
                        val id = database.child("checklist").child(uid).push().key ?: return@setPositiveButton
                        val newTask = ChecklistItem(id = id, task = taskText, isDone = false)
                        database.child("checklist").child(uid).child(id).setValue(newTask)
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.buttonAddTip.setOnClickListener {
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
                    val title = titleEt.text.toString().trim()
                    val content = contentEt.text.toString().trim()
                    if (title.isNotEmpty() && content.isNotEmpty()) {
                        val id = database.child("tips").child(uid).push().key ?: return@setPositiveButton
                        val newTip = Tip(title = title, content = content)
                        database.child("tips").child(uid).child(id).setValue(newTip)
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.searchViewChecklist.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = true
            override fun onQueryTextChange(newText: String?) = true.also { filterChecklist(newText.orEmpty()) }
        })
        binding.searchViewTips.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = true
            override fun onQueryTextChange(newText: String?) = true.also { filterTips(newText.orEmpty()) }
        })
    }

    private fun applyGating() {
        // If you want supplier-only tip creation, switch to: View.VISIBLE only for suppliers.
        binding.buttonAddTip.visibility = View.VISIBLE
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
        filteredTips.addAll(allTips.filter {
            it.title?.lowercase()?.contains(q) == true || it.content?.lowercase()?.contains(q) == true
        })
        tipAdapter.notifyDataSetChanged()
    }
}
