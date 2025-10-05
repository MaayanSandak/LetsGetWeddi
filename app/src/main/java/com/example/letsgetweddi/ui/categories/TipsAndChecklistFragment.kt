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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class TipsAndChecklistFragment : Fragment() {

    private lateinit var binding: FragmentTipsAndChecklistBinding
    private lateinit var database: DatabaseReference

    private lateinit var checklistAdapter: ChecklistAdapter
    private lateinit var tipAdapter: TipAdapter

    private val allChecklistItems = mutableListOf<ChecklistItem>()
    private val filteredChecklistItems = mutableListOf<ChecklistItem>()

    private val allTips = mutableListOf<Tip>()
    private val filteredTips = mutableListOf<Tip>()

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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

        val checklistRef = database.child("checklist").child(uid)
        checklistRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.hasChildren()) {
                    val updates = HashMap<String, Any?>()
                    defaultChecklist.forEach { taskText ->
                        val id = checklistRef.push().key ?: return@forEach
                        updates[id] = mapOf(
                            "id" to id,
                            "task" to taskText,
                            "isDone" to false
                        )
                    }
                    if (updates.isNotEmpty()) checklistRef.updateChildren(updates)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        checklistRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allChecklistItems.clear()
                for (c in snapshot.children) {
                    val item = ChecklistItem.fromSnapshot(c)
                    if (item.id != null) allChecklistItems.add(item)
                }
                filterChecklist(binding.searchViewChecklist.query?.toString().orEmpty())
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        val userTipsRef = database.child("tips").child(uid)
        val globalTipsRef = database.child("tips").child("global")

        val userListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                mergeAndDisplayTips(userSnap = snapshot, globalSnap = lastGlobalSnap)
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        val globalListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                lastGlobalSnap = snapshot
                mergeAndDisplayTips(userSnap = lastUserSnap, globalSnap = snapshot)
            }

            override fun onCancelled(error: DatabaseError) {}
        }

        userTipsRef.addValueEventListener(userListener)
        globalTipsRef.addValueEventListener(globalListener)

        binding.buttonAddTask.setOnClickListener {
            val input = EditText(requireContext()).apply { hint = "Enter your task" }
            AlertDialog.Builder(requireContext())
                .setTitle("New Task")
                .setView(input)
                .setPositiveButton("Add") { _, _ ->
                    val taskText = input.text.toString().trim()
                    if (taskText.isNotEmpty()) {
                        val id = database.child("checklist").child(uid).push().key
                            ?: return@setPositiveButton
                        val newTask = mapOf(
                            "id" to id,
                            "task" to taskText,
                            "isDone" to false
                        )
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
                        val id =
                            database.child("tips").child(uid).push().key ?: return@setPositiveButton
                        val newTip = Tip(title = title, content = content)
                        database.child("tips").child(uid).child(id).setValue(newTip)
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.searchViewChecklist.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = true
            override fun onQueryTextChange(newText: String?) =
                true.also { filterChecklist(newText.orEmpty()) }
        })
        binding.searchViewTips.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = true
            override fun onQueryTextChange(newText: String?) =
                true.also { filterTips(newText.orEmpty()) }
        })
    }

    private var lastUserSnap: DataSnapshot? = null
    private var lastGlobalSnap: DataSnapshot? = null

    private fun mergeAndDisplayTips(userSnap: DataSnapshot?, globalSnap: DataSnapshot?) {
        lastUserSnap = userSnap ?: lastUserSnap
        lastGlobalSnap = globalSnap ?: lastGlobalSnap

        if (lastUserSnap == null && lastGlobalSnap == null) return

        fun parseTips(snap: DataSnapshot?): List<Tip> {
            if (snap == null) return emptyList()
            val out = mutableListOf<Tip>()
            for (c in snap.children) {
                val v = c.value
                when (v) {
                    is String -> {
                        val content = v.trim()
                        if (content.isNotEmpty()) {
                            val title = content.lineSequence().firstOrNull()?.take(40) ?: "Tip"
                            out.add(Tip(title = title, content = content))
                        }
                    }

                    is Map<*, *> -> {
                        val title = (v["title"] as? String)?.takeIf { it.isNotBlank() }
                            ?: (v["name"] as? String)?.takeIf { it.isNotBlank() }
                            ?: (v["header"] as? String)?.takeIf { it.isNotBlank() }
                        val content = (v["content"] as? String)?.takeIf { it.isNotBlank() }
                            ?: (v["text"] as? String)?.takeIf { it.isNotBlank() }
                            ?: (v["body"] as? String)?.takeIf { it.isNotBlank() }
                            ?: (v["message"] as? String)?.takeIf { it.isNotBlank() }
                        if (!title.isNullOrBlank() || !content.isNullOrBlank()) {
                            out.add(Tip(title = title ?: "Tip", content = content ?: ""))
                        }
                    }
                }
            }
            return out
        }

        val userList = parseTips(lastUserSnap)
        val globalList = parseTips(lastGlobalSnap)

        allTips.clear()
        allTips.addAll(userList + globalList)

        filterTips(binding.searchViewTips.query?.toString().orEmpty())
    }

    private fun filterChecklist(query: String) {
        val q = query.lowercase()
        filteredChecklistItems.clear()
        filteredChecklistItems.addAll(allChecklistItems.filter {
            it.task?.lowercase()?.contains(q) == true
        })
        checklistAdapter.notifyDataSetChanged()
    }

    private fun filterTips(query: String) {
        val q = query.lowercase()
        filteredTips.clear()
        filteredTips.addAll(allTips.filter {
            it.title?.lowercase()?.contains(q) == true ||
                    it.content?.lowercase()?.contains(q) == true
        })
        tipAdapter.notifyDataSetChanged()
    }
}
