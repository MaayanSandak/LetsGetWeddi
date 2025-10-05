package com.example.letsgetweddi.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.letsgetweddi.R
import com.example.letsgetweddi.data.Conversation
import com.example.letsgetweddi.data.DbPaths
import com.example.letsgetweddi.databinding.FragmentHomeBinding
import com.example.letsgetweddi.ui.ProviderDetailsActivity
import com.example.letsgetweddi.ui.categories.TipsAndChecklistFragment
import com.example.letsgetweddi.ui.chat.ChatActivity
import com.example.letsgetweddi.ui.chat.ConversationsAdapter
import com.example.letsgetweddi.ui.favorites.FavoritesFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import kotlin.math.ceil

data class HomeFavoriteItem(
    val supplierId: String,
    val supplierName: String,
    val category: String? = null,
    val imageUrl: String? = null
)

class HomeFragment : Fragment() {

    private var _b: FragmentHomeBinding? = null
    private val b get() = _b!!

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseDatabase.getInstance().reference }

    private lateinit var favAdapter: HomeFavoritesAdapter
    private lateinit var msgAdapter: ConversationsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _b = FragmentHomeBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        favAdapter = HomeFavoritesAdapter { supplierId ->
            val i = Intent(requireContext(), ProviderDetailsActivity::class.java)
            i.putExtra(ProviderDetailsActivity.EXTRA_SUPPLIER_ID, supplierId)
            startActivity(i)
        }
        b.recyclerFavorites.layoutManager = LinearLayoutManager(requireContext())
        b.recyclerFavorites.adapter = favAdapter

        msgAdapter = ConversationsAdapter { c ->
            val i = Intent(requireContext(), ChatActivity::class.java)
            i.putExtra("otherUserId", c.otherUserId)
            i.putExtra("otherUserName", c.otherUserName)
            startActivity(i)
        }
        b.recyclerMessages.layoutManager = LinearLayoutManager(requireContext())
        b.recyclerMessages.adapter = msgAdapter

        b.textFavSeeAll.setOnClickListener { openFavorites() }
        b.textMsgSeeAll.setOnClickListener { openMessages() }
        b.cardTips.setOnClickListener { openTips() }

        val uid = auth.currentUser?.uid ?: return
        loadFavorites(uid)
        loadRecentMessages(uid)
        loadWeddingDate(uid)
        loadUserTip(uid)
    }

    private fun openFavorites() {
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment_content_main, FavoritesFragment(), "favorites")
            .commit()
        requireActivity().title = getString(R.string.favorites)
    }

    private fun openMessages() {
        startActivity(
            Intent(
                requireContext(),
                com.example.letsgetweddi.ui.chat.ConversationsActivity::class.java
            )
        )
    }

    private fun openTips() {
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(
                R.id.nav_host_fragment_content_main,
                TipsAndChecklistFragment(),
                "tips_checklist"
            )
            .commit()
        requireActivity().title = getString(R.string.menu_tips_checklist)
    }

    private fun loadFavorites(uid: String) {
        db.child(DbPaths.FAVORITES).child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val ids = snapshot.children.mapNotNull { it.key }
                    if (ids.isEmpty()) {
                        favAdapter.submit(emptyList())
                        b.textFavEmpty.visibility = View.VISIBLE
                        return
                    }
                    fetchSuppliers(ids) { list ->
                        favAdapter.submit(list)
                        b.textFavEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    favAdapter.submit(emptyList())
                    b.textFavEmpty.visibility = View.VISIBLE
                }
            })
    }

    private fun fetchSuppliers(ids: List<String>, onReady: (List<HomeFavoriteItem>) -> Unit) {
        if (ids.isEmpty()) {
            onReady(emptyList()); return
        }
        val storage = FirebaseStorage.getInstance()
        val results = mutableListOf<HomeFavoriteItem>()
        var remaining = ids.size

        fun addResult(item: HomeFavoriteItem) {
            results.add(item)
            remaining--
            if (remaining == 0) onReady(results)
        }

        ids.forEach { sid ->
            db.child(DbPaths.SUPPLIERS).child(sid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(s: DataSnapshot) {
                        val name = s.child("name").getValue(String::class.java) ?: sid
                        val category = s.child("category").getValue(String::class.java)
                            ?: s.child("type").getValue(String::class.java)

                        val candidates = listOfNotNull(
                            s.child("coverImage").getValue(String::class.java),
                            s.child("imageUrl").getValue(String::class.java),
                            s.child("logoUrl").getValue(String::class.java),
                            s.child("photoUrl").getValue(String::class.java),
                            s.child("image").getValue(String::class.java),
                            s.child("avatar").getValue(String::class.java),
                            s.child("gallery").child("0").getValue(String::class.java)
                        )

                        val first = candidates.firstOrNull { it.isNotBlank() }

                        if (first != null && first.startsWith("gs://")) {
                            try {
                                storage.getReferenceFromUrl(first).downloadUrl
                                    .addOnSuccessListener { uri ->
                                        addResult(
                                            HomeFavoriteItem(
                                                supplierId = sid,
                                                supplierName = name,
                                                category = category,
                                                imageUrl = uri.toString()
                                            )
                                        )
                                    }
                                    .addOnFailureListener {
                                        addResult(
                                            HomeFavoriteItem(
                                                supplierId = sid,
                                                supplierName = name,
                                                category = category,
                                                imageUrl = null
                                            )
                                        )
                                    }
                            } catch (e: Exception) {
                                addResult(
                                    HomeFavoriteItem(
                                        supplierId = sid,
                                        supplierName = name,
                                        category = category,
                                        imageUrl = null
                                    )
                                )
                            }
                        } else {
                            addResult(
                                HomeFavoriteItem(
                                    supplierId = sid,
                                    supplierName = name,
                                    category = category,
                                    imageUrl = first
                                )
                            )
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        addResult(HomeFavoriteItem(supplierId = sid, supplierName = sid))
                    }
                })
        }
    }

    private fun loadRecentMessages(uid: String) {
        db.child("inbox").child(uid)
            .orderByChild("lastTs")
            .limitToLast(10)
            .addListenerForSingleValueEvent(object : ValueEventListener {
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
                    msgAdapter.submitList(list)
                    b.textMsgEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                }

                override fun onCancelled(error: DatabaseError) {
                    msgAdapter.submitList(emptyList())
                    b.textMsgEmpty.visibility = View.VISIBLE
                }
            })
    }

    private fun loadWeddingDate(uid: String) {
        db.child("users").child(uid).child("weddingDate")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(s: DataSnapshot) {
                    val millis = s.getValue(Long::class.java) ?: 0L
                    if (millis <= 0L) {
                        b.textCountdownHeader.text = "Countdown to the big day!"
                        b.textCountdown.text = "Set your wedding date"
                        return
                    }
                    val now = System.currentTimeMillis()
                    val diff = millis - now
                    val days = ceil(diff.toDouble() / TimeUnit.DAYS.toMillis(1).toDouble()).toLong()
                    if (days >= 0) {
                        b.textCountdownHeader.text = "Countdown to the big day!"
                        b.textCountdown.text = "$days days to go"
                    } else {
                        b.textCountdownHeader.text = "It was great!"
                        b.textCountdown.text = "${-days} days ago"
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    b.textCountdownHeader.text = "Countdown to the big day!"
                    b.textCountdown.text = "Set your wedding date"
                }
            })
    }

    private fun loadUserTip(uid: String) {
        val ref = FirebaseDatabase.getInstance().reference.child("tips").child("global")
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tips = mutableListOf<String>()
                for (c in snapshot.children) {
                    val v = c.value
                    when (v) {
                        is String -> tips.add(v)
                        is Map<*, *> -> {
                            val title = (v["title"] as? String).orEmpty()
                            val text = (v["text"] as? String)
                                ?: (v["content"] as? String)
                                ?: ""
                            val body = if (title.isNotBlank()) "$title\n$text" else text
                            if (body.isNotBlank()) tips.add(body)
                        }
                    }
                }
                if (tips.isEmpty()) {
                    b.textTipBody.text = "Tips unavailable right now."
                    return
                }
                val day = LocalDate.now().dayOfYear
                val seed = day + (uid.hashCode() and 0x7fffffff)
                val idx = seed % tips.size
                b.textTipBody.text = tips[idx]
            }

            override fun onCancelled(error: DatabaseError) {
                b.textTipBody.text = "Tips unavailable right now."
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
