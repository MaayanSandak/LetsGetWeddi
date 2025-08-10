package com.example.letsgetweddi.ui.chat

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.letsgetweddi.R

class ChatActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.app_name)
        findViewById<ImageButton>(R.id.toolbarNavBack)?.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val name = intent.getStringExtra("supplierName").orEmpty()
        findViewById<TextView>(R.id.textTitle).text = if (name.isBlank()) "Chat" else name
    }
}
