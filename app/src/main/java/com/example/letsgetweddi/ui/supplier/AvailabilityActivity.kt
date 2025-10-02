package com.example.letsgetweddi.ui.supplier

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.CalendarView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.letsgetweddi.R
import com.example.letsgetweddi.data.FirebaseRefs
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class AvailabilityActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var calendarView: CalendarView
    private lateinit var textEmpty: TextView
    private lateinit var progressBar: ProgressBar

    private var supplierId: String? = null
    private val availabilityMap = mutableMapOf<String, Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_availability)

        toolbar = findViewById(R.id.toolbar)
        calendarView = findViewById(R.id.calendarView)
        textEmpty = findViewById(R.id.textEmpty)
        progressBar = findViewById(R.id.progressBar)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Availability"
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        supplierId = resolveSupplierId(intent?.data, intent?.getStringExtra("supplierId"))
        if (supplierId.isNullOrBlank()) {
            showEmpty(true)
            return
        }

        loadAvailability(supplierId!!)
    }

    private fun resolveSupplierId(data: Uri?, extra: String?): String? {
        if (!extra.isNullOrBlank()) return extra
        if (data != null && data.scheme == "letsgetweddi") {
            val segs = data.pathSegments
            if (segs.size >= 2 && segs[0].equals("availability", ignoreCase = true)) {
                return segs[1]
            }
        }
        return null
    }

    private fun loadAvailability(id: String) {
        showLoading(true)
        val ref = FirebaseRefs.availability(id)
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                availabilityMap.clear()
                for (child in snapshot.children) {
                    val date = child.key ?: continue
                    val isAvailable = child.getValue(Boolean::class.java) ?: false
                    availabilityMap[date] = isAvailable
                }
                showLoading(false)
                showEmpty(availabilityMap.isEmpty())
                setupCalendar()
            }

            override fun onCancelled(error: DatabaseError) {
                showLoading(false)
                showEmpty(true)
            }
        })
    }

    private fun setupCalendar() {
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
            val status = availabilityMap[selectedDate]
            if (status == true) {
                Toast.makeText(this, "$selectedDate is available", Toast.LENGTH_SHORT).show()
            } else if (status == false) {
                Toast.makeText(this, "$selectedDate is busy", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "$selectedDate has no data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        calendarView.visibility = if (loading) View.GONE else View.VISIBLE
        textEmpty.visibility = View.GONE
    }

    private fun showEmpty(empty: Boolean) {
        textEmpty.visibility = if (empty) View.VISIBLE else View.GONE
        calendarView.visibility = if (empty) View.GONE else View.VISIBLE
        progressBar.visibility = View.GONE
    }
}
