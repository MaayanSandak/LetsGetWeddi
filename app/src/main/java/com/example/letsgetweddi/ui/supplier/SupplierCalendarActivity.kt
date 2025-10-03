package com.example.letsgetweddi.ui.supplier

import android.os.Bundle
import android.view.ViewGroup
import android.widget.CalendarView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase

class SupplierCalendarActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val supplierId = intent.getStringExtra("supplierId") ?: ""

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setPadding(32, 48, 32, 48)
        }

        val title = TextView(this).apply {
            text = "Availability Calendar"
            textSize = 20f
        }

        val calendarView = CalendarView(this)

        root.addView(title)
        root.addView(calendarView)
        setContentView(root)

        if (supplierId.isNotEmpty()) {
            val ref = FirebaseDatabase.getInstance().getReference("Availability").child(supplierId)
            ref.get().addOnSuccessListener { snapshot ->
                val availableDates = snapshot.children.mapNotNull {
                    it.key?.toLongOrNull()
                }
                calendarView.setDate(
                    availableDates.firstOrNull() ?: System.currentTimeMillis(),
                    false,
                    true
                )
            }
        }
    }
}
