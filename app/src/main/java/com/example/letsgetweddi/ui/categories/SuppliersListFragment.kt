package com.example.letsgetweddi.ui.categories

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.letsgetweddi.adapters.SupplierAdapter
import com.example.letsgetweddi.databinding.FragmentSuppliersListBinding
import com.example.letsgetweddi.model.Category
import com.example.letsgetweddi.model.Supplier
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener

class SuppliersListFragment : Fragment() {

    private var _binding: FragmentSuppliersListBinding? = null
    private val binding get() = _binding!!

    private val shown = mutableListOf<Supplier>()
    private lateinit var adapter: SupplierAdapter

    private var wantedRaw: String = "all"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val raw = listOfNotNull(
            arguments?.getString("category"),
            arguments?.getString("category_id"),
            arguments?.getString("categoryId"),
            arguments?.getString("title")
        ).firstOrNull()
        wantedRaw = raw?.trim().orEmpty().ifBlank { "all" }
        Log.d(TAG, "args category='$wantedRaw'")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSuppliersListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = SupplierAdapter(shown, isFavorites = false)
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.setHasFixedSize(true)
        binding.recycler.adapter = adapter

        binding.progressBar.visibility = View.VISIBLE
        if (wantedRaw.equals("all", ignoreCase = true)) {
            loadAll()
        } else {
            loadByCategoryOrId(wantedRaw)
        }
    }

    private fun loadAll() {
        val db = FirebaseDatabase.getInstance()
        val refs = listOf(
            db.getReference("Suppliers"),
            db.getReference("suppliers")
        )
        collectFromRefs(refs) { list ->
            shown.clear()
            shown.addAll(list.sortedBy { it.name ?: "" })
            binding.progressBar.visibility = View.GONE
            binding.textEmpty.visibility = if (shown.isEmpty()) View.VISIBLE else View.GONE
            adapter.notifyDataSetChanged()
            Log.d(TAG, "loadAll -> shown=${shown.size}")
        }
    }

    private fun loadByCategoryOrId(categoryArg: String) {
        val db = FirebaseDatabase.getInstance()
        val slug = slugify(categoryArg)
        val baseRefs = listOf(
            db.getReference("Suppliers"),
            db.getReference("suppliers")
        )

        // Build queries against common fields and map keys
        val queries = mutableListOf<Query>()
        for (base in baseRefs) {
            queries += base.orderByChild("categoryId").equalTo(categoryArg)
            queries += base.orderByChild("categoryId").equalTo(slug)
            queries += base.orderByChild("category").equalTo(categoryArg)
            queries += base.orderByChild("category").equalTo(slug)
            queries += base.orderByChild("type").equalTo(categoryArg)
            queries += base.orderByChild("type").equalTo(slug)
            queries += base.orderByChild("supplierType").equalTo(categoryArg)
            queries += base.orderByChild("supplierType").equalTo(slug)
            queries += base.orderByChild("profession").equalTo(categoryArg)
            queries += base.orderByChild("profession").equalTo(slug)
            queries += base.orderByChild("categories/$categoryArg").equalTo(true)
            queries += base.orderByChild("categories/$slug").equalTo(true)
            queries += base.orderByChild("tags/$categoryArg").equalTo(true)
            queries += base.orderByChild("tags/$slug").equalTo(true)
        }

        collectFromQueries(queries) { collected ->
            if (collected.isNotEmpty()) {
                shown.clear()
                shown.addAll(collected.sortedBy { it.name ?: "" })
                binding.progressBar.visibility = View.GONE
                binding.textEmpty.visibility = if (shown.isEmpty()) View.VISIBLE else View.GONE
                adapter.notifyDataSetChanged()
                Log.d(TAG, "loadByCategoryOrId (queried) '$categoryArg' -> shown=${shown.size}")
            } else {
                // Fallback: fetch all and filter client-side (very permissive)
                Log.d(TAG, "no results from queries, fallback to client filter")
                val refs = baseRefs
                collectFromRefs(refs) { list ->
                    val wantedNorm = normalizeBasic(categoryArg)
                    val filtered = list.filter { matchesByAnyField(it, wantedNorm) }
                    shown.clear()
                    shown.addAll(filtered.sortedBy { it.name ?: "" })
                    binding.progressBar.visibility = View.GONE
                    binding.textEmpty.visibility = if (shown.isEmpty()) View.VISIBLE else View.GONE
                    adapter.notifyDataSetChanged()
                    Log.d(TAG, "fallback filter '$categoryArg' -> shown=${shown.size}")
                }
            }
        }
    }

    private fun collectFromRefs(
        refs: List<DatabaseReference>,
        done: (List<Supplier>) -> Unit
    ) {
        if (refs.isEmpty()) {
            done(emptyList())
            return
        }
        val all = LinkedHashMap<String, Supplier>()
        var remaining = refs.size
        refs.forEach { ref ->
            ref.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var added = 0
                    for (child in snapshot.children) {
                        val s = Supplier.fromSnapshot(child)
                        val id = s.id
                        if (id != null && !all.containsKey(id)) {
                            all[id] = s
                            added++
                        }
                    }
                    Log.d(TAG, "collectFromRefs '${ref.path}': added=$added total=${all.size}")
                    if (--remaining == 0) done(all.values.toList())
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "collectFromRefs cancelled '${ref.path}': ${error.message}")
                    if (--remaining == 0) done(all.values.toList())
                }
            })
        }
    }

    private fun collectFromQueries(
        queries: List<Query>,
        done: (List<Supplier>) -> Unit
    ) {
        if (queries.isEmpty()) {
            done(emptyList())
            return
        }
        val all = LinkedHashMap<String, Supplier>()
        var remaining = queries.size
        queries.forEach { q ->
            q.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var added = 0
                    for (child in snapshot.children) {
                        val s = Supplier.fromSnapshot(child)
                        val id = s.id
                        if (id != null && !all.containsKey(id)) {
                            all[id] = s
                            added++
                        }
                    }
                    Log.d(TAG, "collectFromQueries '${q.ref.path}' added=$added accum=${all.size}")
                    if (--remaining == 0) done(all.values.toList())
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "collectFromQueries cancelled '${q.ref.path}': ${error.message}")
                    if (--remaining == 0) done(all.values.toList())
                }
            })
        }
    }

    private fun matchesByAnyField(s: Supplier, wantedNorm: String): Boolean {
        if (wantedNorm.isBlank()) return true
        val cands = listOfNotNull(
            s.category, s.categoryId
        ) + (s.categories ?: emptyList())
        return cands.any {
            normalizeBasic(it).let { n ->
                n == wantedNorm || n.contains(wantedNorm) || wantedNorm.contains(
                    n
                )
            }
        }
    }

    private fun slugify(text: String): String = normalizeBasic(text)
    private fun normalizeBasic(text: String?): String {
        if (text.isNullOrBlank()) return ""
        var t = text.trim().lowercase()
        t = t.replace(Regex("[`'\"’]"), "")
            .replace(Regex("[_/|]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        return t
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "SuppliersListFragment"

        fun newInstance(category: Category): SuppliersListFragment {
            return SuppliersListFragment().apply {
                arguments = Bundle().apply {
                    putString("category", category.id ?: category.title ?: "all")
                    putString("category_id", category.id ?: "")
                    putString("categoryId", category.id ?: "")
                    putString("title", category.title ?: "")
                }
            }
        }

        fun newInstance(categoryId: String): SuppliersListFragment {
            return SuppliersListFragment().apply {
                arguments = Bundle().apply { putString("category", categoryId) }
            }
        }
    }
}
