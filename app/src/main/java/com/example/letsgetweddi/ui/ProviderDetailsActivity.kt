package com.example.letsgetweddi.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.letsgetweddi.R
import com.example.letsgetweddi.adapters.ReviewAdapter
import com.example.letsgetweddi.data.DbPaths
import com.example.letsgetweddi.data.FirebaseRefs
import com.example.letsgetweddi.databinding.ActivityProviderDetailsBinding
import com.example.letsgetweddi.model.Review
import com.example.letsgetweddi.model.Supplier
import com.example.letsgetweddi.ui.gallery.GalleryFragment
import com.example.letsgetweddi.ui.supplier.AvailabilityActivity
import com.example.letsgetweddi.utils.RoleManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso

class ProviderDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProviderDetailsBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseDatabase.getInstance() }
    private var supplierId: String? = null
    private var supplier: Supplier? = null
    private var categoryId: String? = null
    private val reviews: MutableList<Review> = mutableListOf()
    private lateinit var reviewsAdapter: ReviewAdapter

    private var favRef: DatabaseReference? = null
    private var favListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProviderDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supplierId = intent.getStringExtra(EXTRA_SUPPLIER_ID)
        categoryId = supplier?.categoryId

        setupToolbar()
        setupLists()
        wireActions()

        loadSupplier()
        observeFavorite()
        loadReviewsMultiPath()
        loadAvailabilityHint()
        mountInlineGalleryIfPossible()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupLists() {
        reviewsAdapter = ReviewAdapter(reviews)
        binding.recyclerReviews.layoutManager = LinearLayoutManager(this)
        binding.recyclerReviews.adapter = reviewsAdapter
    }

    private fun wireActions() {
        binding.buttonWhatsApp.setOnClickListener {
            val phone = supplier?.phone ?: return@setOnClickListener
            val uri = Uri.parse("https://wa.me/${phone.replace("+", "")}")
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }

        binding.buttonChat.setOnClickListener {
            val peer = supplierId ?: return@setOnClickListener
            val intent = Intent(this, com.example.letsgetweddi.ui.chat.ChatActivity::class.java)
                .putExtra("otherUserId", peer)
                .putExtra("otherUserName", supplier?.name ?: "")
            startActivity(intent)
        }

        binding.buttonFav.setOnClickListener { toggleFavorite() }

        binding.buttonEdit.setOnClickListener {
            val id = supplierId ?: return@setOnClickListener
            startActivity(
                Intent(this, com.example.letsgetweddi.ui.supplier.SupplierEditActivity::class.java)
                    .putExtra(EXTRA_SUPPLIER_ID, id)
            )
        }

        binding.buttonManageGallery.setOnClickListener {
            val id = supplierId ?: return@setOnClickListener
            startActivity(
                Intent(this, com.example.letsgetweddi.ui.gallery.GalleryViewActivity::class.java)
                    .putExtra(EXTRA_SUPPLIER_ID, supplier?.id)
                    .putExtra("categoryId", supplier?.categoryId)
            )
        }

        binding.buttonManageAvailability.setOnClickListener {
            val id = supplierId ?: return@setOnClickListener
            startActivity(
                Intent(this, AvailabilityActivity::class.java)
                    .putExtra(EXTRA_SUPPLIER_ID, id)
            )
        }
    }

    private fun loadSupplier() {
        val id = supplierId ?: return
        val primary = db.getReference(DbPaths.supplier(id))
        primary.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    supplier = Supplier.fromSnapshot(snapshot)
                    render()
                } else {
                    db.getReference("suppliers/$id")
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(s2: DataSnapshot) {
                                supplier = Supplier.fromSnapshot(s2)
                                render()
                            }

                            override fun onCancelled(error: DatabaseError) {}
                        })
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun render() {
        val s = supplier ?: return
        title = s.name ?: getString(R.string.supplier_name)

        binding.textName.text = s.name.orEmpty()
        binding.textLocation.text = s.location.orEmpty()
        binding.textDescription.text = s.description.orEmpty()

        // ensure categoryId is set so gallery can mount
        categoryId = s.categoryId

        loadSupplierCover(binding.imageHeader, s.id, s.imageUrl)

        RoleManager.isSupplier(this) { isSupplier, mySupplierId ->
            val canEdit = isSupplier && !mySupplierId.isNullOrBlank() && mySupplierId == s.id
            binding.manageRow.visibility = if (canEdit) View.VISIBLE else View.GONE
        }

        binding.buttonFav.visibility = View.VISIBLE
        binding.buttonChat.visibility = View.VISIBLE
        binding.buttonWhatsApp.visibility = View.VISIBLE

        mountInlineGalleryIfPossible()
        loadAvailabilityHint()
    }

    private fun observeFavorite() {
        val sId = supplierId ?: return
        val uid = auth.currentUser?.uid ?: return
        favListener?.let { favRef?.removeEventListener(it) }
        favRef = FirebaseRefs.favorite(uid, sId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                updateFavIcon(snapshot.exists())
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        favRef!!.addValueEventListener(listener)
        favListener = listener
        updateFavIcon(false)
    }

    private fun updateFavIcon(isFav: Boolean) {
        val btn = binding.buttonFav as ImageButton
        btn.setImageResource(if (isFav) R.drawable.ic_favorite else R.drawable.ic_favorite_border)
        btn.contentDescription =
            getString(if (isFav) R.string.remove_from_favorites else R.string.add_to_favorites)
    }

    private fun toggleFavorite() {
        val sId = supplierId ?: return
        val uid = auth.currentUser?.uid ?: return
        val ref = FirebaseRefs.favorite(uid, sId)
        ref.get().addOnSuccessListener { snap ->
            if (snap.exists()) {
                ref.removeValue()
            } else {
                val s = supplier ?: return@addOnSuccessListener
                ref.setValue(s)
            }
        }
    }

    private fun loadAvailabilityHint() {
        val sId = supplierId ?: return
        FirebaseRefs.availability(sId).get()
            .addOnSuccessListener { /* optional hint UI - none here */ }
            .addOnFailureListener { /* optional */ }
    }

    private fun mountInlineGalleryIfPossible() {
        val sid = supplierId ?: return
        val cid = categoryId ?: return
        if (supportFragmentManager.findFragmentById(R.id.galleryContainer) == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.galleryContainer, GalleryFragment.newInstance(sid, cid))
                .commitAllowingStateLoss()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        favListener?.let { favRef?.removeEventListener(it) }
        favListener = null
        favRef = null
    }

    private fun loadSupplierCover(
        target: android.widget.ImageView,
        sId: String?,
        urlOrPath: String?
    ) {
        val v = (urlOrPath ?: "").trim()
        if (v.startsWith("http", true)) {
            Picasso.get().load(v).fit().centerCrop().into(target)
            return
        }
        val id = sId ?: return
        val storage = FirebaseStorage.getInstance().reference
        val candidates = listOf(
            v,
            "suppliers/$id/cover.jpg",
            "suppliers/$id/cover.jpeg",
            "suppliers/$id/cover.png",
            "suppliers/$id/cover.webp",
            "suppliers/$id/cover.avif",
            "suppliers/$id/cover dj.avif"
        ).filter { it.isNotBlank() }

        fun tryIndex(i: Int) {
            if (i >= candidates.size) {
                target.setImageDrawable(null)
                return
            }
            val path = candidates[i]
            if (path.startsWith("http", true)) {
                Picasso.get().load(path).fit().centerCrop().into(target)
            } else {
                storage.child(path).downloadUrl
                    .addOnSuccessListener { uri ->
                        Picasso.get().load(uri).fit().centerCrop().into(target)
                    }
                    .addOnFailureListener { tryIndex(i + 1) }
            }
        }
        tryIndex(0)
    }

    private fun loadReviewsMultiPath() {
        val sId = supplierId ?: return

        val paths = listOf(
            FirebaseRefs.reviews(sId),            // reviews/<supplierId>
            db.getReference("reviews/$sId"),
            db.getReference("suppliers/$sId/reviews"),
            db.getReference("Suppliers/$sId/reviews")
        )

        val bag = mutableListOf<Review>()
        var remaining = paths.size

        fun finish() {
            reviews.clear()
            reviews.addAll(bag)

            val rated = bag.filter { it.rating > 0f }
            val sum = rated.sumOf { it.rating.toDouble() }.toFloat()
            val count = rated.size
            val avg = if (count > 0) sum / count else 0f

            binding.textRatingCount.text = "($count)"
            binding.textRatingAvg.text = String.format("%.1f", avg)
            binding.ratingBarSummary.rating = avg

            resolveReviewerNames(reviews) {
                reviewsAdapter.notifyDataSetChanged()
            }
        }

        fun readOnce(ref: DatabaseReference, after: () -> Unit) {
            ref.get()
                .addOnSuccessListener { snap ->
                    for (c in snap.children) {
                        Review.fromSnapshot(c)?.let { bag += it }
                            ?: c.getValue(Review::class.java)?.let { bag += it }
                    }
                    after()
                }
                .addOnFailureListener { after() }
        }

        paths.forEach { ref ->
            readOnce(ref) {
                remaining -= 1
                if (remaining == 0) finish()
            }
        }
    }

    private fun resolveReviewerNames(items: MutableList<Review>, onDone: () -> Unit) {
        val allUids = items.mapNotNull { it.userId?.trim() }.filter { it.isNotEmpty() }.distinct()
        if (allUids.isEmpty()) {
            onDone(); return
        }

        var remaining = allUids.size
        val names = HashMap<String, String>()

        fun takeName(node: DataSnapshot): String? {
            val candidates = listOf("displayName", "name", "fullName", "firstName", "lastName")
            for (k in candidates) {
                val v = node.child(k).value
                if (v is String && v.isNotBlank()) return v
            }
            val first = node.child("firstName").getValue(String::class.java).orEmpty()
            val last = node.child("lastName").getValue(String::class.java).orEmpty()
            val both = listOf(first, last).filter { it.isNotBlank() }.joinToString(" ")
            return both.ifBlank { null }
        }

        fun doneOne() {
            remaining -= 1
            if (remaining == 0) {
                items.forEachIndexed { idx, r ->
                    val uid = r.userId
                    if (!uid.isNullOrBlank() && r.name.isNullOrBlank()) {
                        names[uid]?.let { nm ->
                            items[idx] = r.copy(name = nm)
                        }
                    }
                }
                onDone()
            }
        }

        allUids.forEach { uid ->
            // Try lowercase "users" first (your DB), then fallback to "Users"
            db.getReference("users/$uid").get()
                .addOnSuccessListener { snap ->
                    val nm = takeName(snap) ?: run {
                        // fallback to "Users"
                        null
                    }
                    if (nm != null) {
                        names[uid] = nm
                        doneOne()
                    } else {
                        db.getReference(DbPaths.user(uid)).get()
                            .addOnSuccessListener { s2 ->
                                takeName(s2)?.let { names[uid] = it }
                                doneOne()
                            }
                            .addOnFailureListener { doneOne() }
                    }
                }
                .addOnFailureListener {
                    db.getReference(DbPaths.user(uid)).get()
                        .addOnSuccessListener { s2 ->
                            takeName(s2)?.let { names[uid] = it }
                            doneOne()
                        }
                        .addOnFailureListener { doneOne() }
                }
        }
    }

    companion object {
        const val EXTRA_SUPPLIER_ID = "supplierId"
    }
}
