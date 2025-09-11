package com.example.letsgetweddi.ui.supplier

import android.graphics.Typeface
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SupplierCalendarActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val supplierId = intent.getStringExtra("supplierId")
            ?: intent.data?.lastPathSegment
            ?: ""

        // Simple programmatic UI (no XML needed)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 48)
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val title = TextView(this).apply {
            text = "Availability"
            textSize = 20f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, 0, 0, 16)
        }

        val info = TextView(this).apply {
            text = if (supplierId.isNotEmpty()) {
                "Calendar screen placeholder for supplierId:\n$supplierId\n\n" +
                        "This placeholder prevents a crash while a full calendar screen is added."
            } else {
                "Calendar screen placeholder.\n\nNo supplierId provided."
            }
            textSize = 16f
            gravity = Gravity.CENTER
            movementMethod = LinkMovementMethod.getInstance()
        }

        root.addView(title)
        root.addView(info)
        setContentView(root)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}
