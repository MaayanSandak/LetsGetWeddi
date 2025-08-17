package com.example.letsgetweddi.ui.supplier

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class SupplierEditActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            val fragment = EditSupplierFragment().apply {
                // If you ever want to pass arguments:
                // arguments = Bundle().apply {
                //     putString("supplierId", intent.getStringExtra("supplierId"))
                // }
            }
            supportFragmentManager
                .beginTransaction()
                .replace(android.R.id.content, fragment)
                .commit()
        }
    }
}
