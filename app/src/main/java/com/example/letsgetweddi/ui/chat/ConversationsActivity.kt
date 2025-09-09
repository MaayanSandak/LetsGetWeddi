package com.example.letsgetweddi.ui.chat

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.letsgetweddi.data.Conversation
import com.example.letsgetweddi.databinding.ActivityConversationsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ConversationsActivity : AppCompatActivity() {

    private lateinit var b: ActivityConversationsBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseDatabase.getInstance().reference }
    private lateinit var adapter: ConversationsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityConversationsBinding.inflate(layoutInflater)
        setContentView(b.root)

        setSupportActionBar(b.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        b.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        adapter = ConversationsAdapter { openChat(it) }
        b.recycler.layoutManager = LinearLayoutManager(this)
        b.recycler.adapter = adapter

        val uid = auth.currentUser?.uid ?: return
        db.child("inbox").child(uid)
            .orderByChild("lastTs")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<Conversation>()
                    for (s in snapshot.children) {
                        val chatId = s.child("chatId").getValue(String::class.java).orEmpty()
                        val otherUserId =
                            s.child("otherUserId").getValue(String::class.java).orEmpty()
                        val otherUserName =
                            s.child("otherUserName").getValue(String::class.java).orEmpty()
                        val lastText = s.child("lastText").getValue(String::class.java).orEmpty()
                        val lastTs = s.child("lastTs").getValue(Long::class.java) ?: 0L
                        list.add(Conversation(chatId, otherUserId, otherUserName, lastText, lastTs))
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
