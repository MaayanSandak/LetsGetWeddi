package com.example.letsgetweddi.ui.supplier

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.letsgetweddi.R
import com.example.letsgetweddi.data.FirebaseRefs
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class AvailabilityActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var recycler: RecyclerView
    private lateinit var textEmpty: TextView
    private lateinit var progressBar: ProgressBar

    private val rows = mutableListOf<Row>()
    private val adapter = RowAdapter(rows)

    private var supplierId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_availability)

        // views
        toolbar = findViewById(R.id.toolbar)
        recycler = findViewById(R.id.recycler)
        textEmpty = findViewById(R.id.textEmpty)
        progressBar = findViewById(R.id.progressBar)

        // toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Availability"
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // list
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter
        recycler.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        // resolve supplier id (extra or deep link)
        supplierId = resolveSupplierId(intent?.data, intent?.getStringExtra("supplierId"))
        if (supplierId.isNullOrBlank()) {
            showEmpty(true)
            return
        }

        // load data
        loadAvailability(supplierId!!)
    }

    private fun resolveSupplierId(data: Uri?, extra: String?): String? {
        if (!extra.isNullOrBlank()) return extra
        if (data != null && data.scheme == "letsgetweddi") {
            // expect: letsgetweddi://availability/<supplierId>
            val segs = data.pathSegments
            if (segs.size >= 2 && segs[0].equals("availability", ignoreCase = true)) {
                return segs[1]
            }
        }
        return null
    }

    private fun loadAvailability(id: String) {
        showLoading(true)
        rows.clear()
        adapter.notifyDataSetChanged()

        // Expecting a list under: availability/<supplierId>
        val ref = FirebaseRefs.availability(id)
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.hasChildren()) {
                    for (child in snapshot.children) {
                        // we’ll show key/value in a friendly way regardless of exact schema
                        val title = child.key ?: ""
                        val subtitle = when {
                            child.value is String -> (child.value as String)
                            child.value is Number -> (child.value as Number).toString()
                            child.value is Boolean -> ((child.value as Boolean).toString())
                            else -> child.value?.toString() ?: ""
                        }
                        rows.add(Row(title, subtitle))
                    }
                }
                adapter.notifyDataSetChanged()
                showLoading(false)
                showEmpty(rows.isEmpty())
            }

            override fun onCancelled(error: DatabaseError) {
                showLoading(false)
                showEmpty(true)
            }
        })
    }

    private fun showLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        recycler.visibility = if (loading) View.GONE else View.VISIBLE
        textEmpty.visibility = View.GONE
    }

    private fun showEmpty(empty: Boolean) {
        textEmpty.visibility = if (empty) View.VISIBLE else View.GONE
        recycler.visibility = if (empty) View.GONE else View.VISIBLE
        progressBar.visibility = View.GONE
    }

    // --- simple row model + adapter ---
    data class Row(val title: String, val subtitle: String)

    private class RowAdapter(private val items: List<Row>) :
        RecyclerView.Adapter<RowAdapter.VH>() {

        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val textTitle: TextView = v.findViewById(R.id.textTitle)
            val textSubtitle: TextView = v.findViewById(R.id.textSubtitle)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VH {
            val v = android.view.LayoutInflater.from(parent.context)
                .inflate(R.layout.item_availability, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val r = items[position]
            holder.textTitle.text = r.title
            holder.textSubtitle.text = r.subtitle
        }

        override fun getItemCount(): Int = items.size
    }
}
