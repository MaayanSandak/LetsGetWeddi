package com.example.letsgetweddi.ui.chat

import android.os.Bundle
import android.text.TextUtils
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.letsgetweddi.data.ChatMessage
import com.example.letsgetweddi.databinding.ActivityChatBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ChatActivity : AppCompatActivity() {

    private lateinit var b: ActivityChatBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseDatabase.getInstance().reference }

    private lateinit var myId: String
    private lateinit var otherId: String
    private var otherName: String? = null
    private var myName: String? = null

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
        b.recycler.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        b.recycler.adapter = adapter

        resolveMyDisplayName {
            b.btnSend.setOnClickListener {
                val text = b.input.text?.toString()?.trim().orEmpty()
                if (!TextUtils.isEmpty(text)) {
                    sendMessage(text)
                    b.input.setText("")
                }
            }
        }

        listenMessages()
    }

    private fun resolveMyDisplayName(onReady: () -> Unit) {
        val ref = FirebaseDatabase.getInstance().getReference("Users").child(myId)
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                myName = snapshot.child("name").getValue(String::class.java)
                    ?: snapshot.child("email").getValue(String::class.java)
                onReady()
            }

            override fun onCancelled(error: DatabaseError) {
                onReady()
            }
        })
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        msgsListener?.let { l -> msgsRef?.removeEventListener(l) }
    }

    private fun listenMessages() {
        val ref = db.child("chats").child(chatId).child("messages")
        msgsRef = ref
        msgsListener = ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<ChatMessage>()
                for (s in snapshot.children) {
                    val m = s.getValue(ChatMessage::class.java) ?: continue
                    list.add(m)
                }
                adapter.submit(list)
                b.recycler.scrollToPosition(list.size.coerceAtLeast(1) - 1)
                markSeenIfNeeded(list)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun sendMessage(text: String) {
        val msgKey = db.child("chats").child(chatId).child("messages").push().key ?: return
        val msg = ChatMessage(
            id = msgKey,
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
                "otherUserName" to (otherName ?: ""),
                "lastText" to text,
                "lastTs" to ServerValue.TIMESTAMP
            ),
            "inbox/$otherId/$chatId" to mapOf(
                "chatId" to chatId,
                "otherUserId" to myId,
                "otherUserName" to (myName ?: ""),
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
        notSeen.forEach { updates["chats/$chatId/messages/${it.id}/seen"] = true }
        db.updateChildren(updates)
    }
}
