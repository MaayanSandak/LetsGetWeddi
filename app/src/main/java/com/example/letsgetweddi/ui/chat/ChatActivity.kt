package com.example.letsgetweddi.ui.chat

import android.os.Bundle
import android.text.TextUtils
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.example.letsgetweddi.databinding.ActivityChatBinding
import com.example.letsgetweddi.data.ChatMessage

class ChatActivity : AppCompatActivity() {

    private lateinit var b: ActivityChatBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseDatabase.getInstance().reference }

    private lateinit var myId: String
    private lateinit var otherId: String
    private var otherName: String? = null

    private lateinit var chatId: String
    private lateinit var adapter: ChatAdapter
    private var msgsRef: DatabaseReference? = null
    private var msgsListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        b = ActivityChatBinding.inflate(layoutInflater)
        setContentView(b.root)

        myId = auth.currentUser?.uid.orEmpty()
        otherId = intent.getStringExtra("otherUserId").orEmpty()
        otherName = intent.getStringExtra("otherUserName")

        if (myId.isEmpty() || otherId.isEmpty()) {
            finish()
            return
        }

        chatId = if (myId < otherId) "${myId}_${otherId}" else "${otherId}_${myId}"

        setSupportActionBar(b.toolbar)
        supportActionBar?.title = otherName ?: "Chat"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        adapter = ChatAdapter(myId)
        b.recycler.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        b.recycler.adapter = adapter

        b.btnSend.setOnClickListener {
            val text = b.input.text?.toString()?.trim().orEmpty()
            if (!TextUtils.isEmpty(text)) {
                sendMessage(text)
                b.input.setText("")
            }
        }

        listenMessages()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        msgsListener?.let { msgsRef?.removeEventListener(it) }
    }

    private fun listenMessages() {
        msgsRef = db.child("chats").child(chatId).child("messages")
        msgsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<ChatMessage>()
                for (c in snapshot.children) {
                    c.getValue(ChatMessage::class.java)?.let { list.add(it) }
                }
                list.sortBy { it.timestamp }
                adapter.submitList(list)
                b.recycler.scrollToPosition(list.lastIndex.coerceAtLeast(0))
                markSeenIfNeeded(list)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        msgsRef?.addValueEventListener(msgsListener as ValueEventListener)
    }

    private fun sendMessage(text: String) {
        val msgKey = db.child("chats").child(chatId).child("messages").push().key ?: return
        val msg = ChatMessage(
            id = msgKey,
            chatId = chatId,
            senderId = myId,
            receiverId = otherId,
            text = text,
            timestamp = System.currentTimeMillis(),
            seen = false
        )
        val updates = hashMapOf<String, Any>(
            "chats/$chatId/messages/$msgKey" to msg,
            "inbox/$myId/$chatId" to mapOf(
                "chatId" to chatId,
                "otherUserId" to otherId,
                "lastText" to text,
                "lastTs" to ServerValue.TIMESTAMP
            ),
            "inbox/$otherId/$chatId" to mapOf(
                "chatId" to chatId,
                "otherUserId" to myId,
                "lastText" to text,
                "lastTs" to ServerValue.TIMESTAMP
            )
        )
        db.updateChildren(updates)
    }

    private fun markSeenIfNeeded(list: List<ChatMessage>) {
        val notSeen = list.filter { it.receiverId == myId && !it.seen }
        if (notSeen.isEmpty()) return
        val updates = hashMapOf<String, Any>()
        notSeen.forEach {
            updates["chats/$chatId/messages/${it.id}/seen"] = true
        }
        db.updateChildren(updates)
    }
}

