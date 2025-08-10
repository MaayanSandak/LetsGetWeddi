package com.example.letsgetweddi.ui.chat

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.letsgetweddi.data.Conversation
import com.example.letsgetweddi.databinding.ActivityConversationsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ConversationsActivity : AppCompatActivity() {

    private lateinit var b: ActivityConversationsBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseDatabase.getInstance().reference }

    private lateinit var adapter: ConversationsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        b = ActivityConversationsBinding.inflate(layoutInflater)
        setContentView(b.root)

        setSupportActionBar(b.toolbar)
        supportActionBar?.title = "Conversations"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        adapter = ConversationsAdapter { openChat(it) }
        b.recycler.layoutManager = LinearLayoutManager(this)
        b.recycler.adapter = adapter

        loadConversations()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun loadConversations() {
        val myId = auth.currentUser?.uid ?: return
        db.child("inbox").child(myId).orderByChild("lastTs")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<Conversation>()
                    for (c in snapshot.children) {
                        c.getValue(Conversation::class.java)?.let { list.add(it) }
                    }
                    list.sortByDescending { it.lastTs }
                    adapter.submitList(list)
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun openChat(c: Conversation) {
        val i = Intent(this, ChatActivity::class.java)
        i.putExtra("otherUserId", c.otherUserId)
        i.putExtra("otherUserName", c.otherUserName)
        startActivity(i)
    }
}
