package com.example.letsgetweddi.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.letsgetweddi.R
import com.example.letsgetweddi.adapters.ReviewAdapter
import com.example.letsgetweddi.data.DbPaths
import com.example.letsgetweddi.data.FirebaseRefs
import com.example.letsgetweddi.model.Supplier
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.squareup.picasso.Picasso

class ProviderDetailsActivity : AppCompatActivity() {

    private lateinit var imageHeader: ImageView
    private lateinit var textName: TextView
    private lateinit var textLocation: TextView
    private lateinit var textAvailability: TextView
    private lateinit var textRatingAvg: TextView
    private lateinit var textRatingCount: TextView
    private lateinit var recyclerReviews: RecyclerView
    private lateinit var buttonFav: ImageButton
    private lateinit var buttonWhatsApp: MaterialButton
    private lateinit var buttonChat: MaterialButton

    private lateinit var reviewsAdapter: ReviewAdapter
    private val reviews = mutableListOf<Pair<String, String>>()

    private var supplierId: String = ""
    private var currentSupplier: Supplier? = null
    private var isFavorite = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_provider_details)

        supplierId = intent.getStringExtra("supplierId").orEmpty()

        imageHeader = findViewById(R.id.imageHeader)
        textName = findViewById(R.id.textName)
        textLocation = findViewById(R.id.textLocation)
        textAvailability = findViewById(R.id.textAvailability)
        textRatingAvg = findViewById(R.id.textRatingAvg)
        textRatingCount = findViewById(R.id.textRatingCount)
        recyclerReviews = findViewById(R.id.recyclerReviews)
        buttonFav = findViewById(R.id.buttonFav)
        buttonWhatsApp = findViewById(R.id.buttonWhatsApp)
        buttonChat = findViewById(R.id.buttonChat)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = getString(R.string.app_name)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        findViewById<ImageButton>(R.id.toolbarNavBack)?.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        reviewsAdapter = ReviewAdapter(reviews)
        recyclerReviews.layoutManager = LinearLayoutManager(this)
        recyclerReviews.adapter = reviewsAdapter

        loadSupplier()
        observeFavoriteState()
        loadAvailability()
        loadReviews()
        mountGallery()

        buttonFav.setOnClickListener { toggleFavorite() }
        buttonWhatsApp.setOnClickListener { openWhatsApp() }
        buttonChat.setOnClickListener { openChat() }
    }

    private fun loadSupplier() {
        val ref = FirebaseDatabase.getInstance().getReference(DbPaths.supplier(supplierId))
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                val supplier = s.getValue(Supplier::class.java)?.copy(
                    id = s.child("id").getValue(String::class.java) ?: s.key
                )
                currentSupplier = supplier
                textName.text = supplier?.name ?: ""
                textLocation.text = supplier?.location ?: ""
                val url = supplier?.imageUrl.orEmpty()
                if (url.isNotBlank()) {
                    Picasso.get()
                        .load(url)
                        .placeholder(R.drawable.rounded_card_placeholder)
                        .fit()
                        .centerCrop()
                        .into(imageHeader)
                } else {
                    imageHeader.setImageResource(R.drawable.rounded_card_placeholder)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun observeFavoriteState() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val favRef = FirebaseRefs.favorites(user.uid).child(supplierId)
        favRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                isFavorite = s.exists()
                buttonFav.setImageResource(if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun toggleFavorite() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val ref = FirebaseRefs.favorites(user.uid).child(supplierId)
        val supplier = currentSupplier ?: return
        if (isFavorite) ref.removeValue() else ref.setValue(supplier)
    }

    private fun loadAvailability() {
        val ref = FirebaseDatabase.getInstance().getReference(DbPaths.SUPPLIERS_AVAILABILITY)
            .child(supplierId)
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                val dates = mutableListOf<String>()
                for (c in s.children) c.getValue(String::class.java)?.let { dates.add(it) }
                textAvailability.text =
                    if (dates.isEmpty()) getString(R.string.no_availability)
                    else getString(R.string.availability_prefix, dates.joinToString(", "))
            }
            override fun onCancelled(error: DatabaseError) {
                textAvailability.text = getString(R.string.no_availability)
            }
        })
    }

    private fun loadReviews() {
        val ref = FirebaseDatabase.getInstance().getReference(DbPaths.SUPPLIER_REVIEWS)
            .child(supplierId)
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                reviews.clear()
                var sum = 0.0
                var count = 0
                for (c in s.children) {
                    val author = c.child("author").getValue(String::class.java) ?: ""
                    val content = c.child("content").getValue(String::class.java) ?: ""
                    val rating = c.child("rating").getValue(Double::class.java) ?: 0.0
                    if (author.isNotBlank() || content.isNotBlank()) reviews.add(author to content)
                    if (rating > 0) { sum += rating; count++ }
                }
                val avg = if (count > 0) kotlin.math.round((sum / count) * 10) / 10.0 else 0.0
                textRatingAvg.text = avg.toString()
                textRatingCount.text = getString(R.string.rating_count, count)
                reviewsAdapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun mountGallery() {
        val frag = com.example.letsgetweddi.ui.gallery.GalleryFragment.newInstance(supplierId)
        supportFragmentManager.beginTransaction()
            .replace(R.id.galleryContainer, frag)
            .commitAllowingStateLoss()
    }

    private fun openWhatsApp() {
        val phoneRaw = currentSupplier?.phone.orEmpty()
        if (phoneRaw.isBlank()) return
        val digits = phoneRaw.filter { it.isDigit() || it == '+' }
        val uri = Uri.parse("https://wa.me/${digits.replace("+", "")}")
        startActivity(Intent(Intent.ACTION_VIEW, uri))
    }

    private fun openChat() {
        val s = currentSupplier ?: return
        val i = Intent(this, com.example.letsgetweddi.ui.chat.ChatActivity::class.java)
        i.putExtra("supplierId", s.id ?: supplierId)
        i.putExtra("supplierName", s.name ?: "")
        startActivity(i)
    }
}
