package com.example.letsgetweddi.ui.providers

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.letsgetweddi.R
import com.example.letsgetweddi.adapters.ReviewAdapter
import com.example.letsgetweddi.data.DbPaths
import com.example.letsgetweddi.data.FirebaseRefs
import com.example.letsgetweddi.databinding.ActivityProviderDetailsBinding
import com.example.letsgetweddi.model.Review
import com.example.letsgetweddi.ui.chat.ChatActivity
import com.example.letsgetweddi.ui.gallery.GalleryFragment
import com.example.letsgetweddi.ui.supplier.SupplierDashboardActivity
import com.example.letsgetweddi.utils.RoleManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import com.kizitonwose.calendar.view.CalendarView
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.ViewContainer
import com.squareup.picasso.Picasso
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

class ProviderDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProviderDetailsBinding

    private lateinit var reviewsRef: DatabaseReference
    private lateinit var reviewAdapter: ReviewAdapter
    private val reviewsList = mutableListOf<Review>()

    private var supplierId: String = ""
    private var supplierPhone: String = ""
    private var supplierName: String = ""
    private var supplierDescription: String = ""
    private var supplierLocation: String = ""
    private var supplierCategory: String = ""
    private var supplierImageUrl: String = ""

    private var isFavorite: Boolean = false
    private var currentRole: String = "client"
    private var currentUserSupplierId: String? = null

    private val isoFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
    private var availableDates: Set<LocalDate> = emptySet()

    // ---------- Calendar view container ----------
    private inner class DayViewContainer(view: View) : ViewContainer(view) {
        val text: TextView = view.findViewById(R.id.calendarDayText)
        val dot: View = view.findViewById(R.id.dot)
        lateinit var date: LocalDate
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProviderDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supplierId = intent.getStringExtra("id") ?: intent.getStringExtra("supplierId") ?: return

        loadSupplierDetails()
        setupCalendarView()
        loadAvailability()
        setupButtons()
        loadReviews()
        preloadFavoriteState()
        preloadRoleAndOwnerUi()
    }

    private fun loadSupplierDetails() {
        FirebaseRefs.ref(DbPaths.supplier(supplierId)).get().addOnSuccessListener { snapshot ->
            supplierName = snapshot.child("name").value?.toString() ?: ""
            supplierDescription = snapshot.child("description").value?.toString() ?: ""
            supplierLocation = snapshot.child("location").value?.toString() ?: ""
            supplierCategory = snapshot.child("category").value?.toString() ?: ""
            supplierImageUrl = snapshot.child("imageUrl").value?.toString() ?: ""
            supplierPhone = snapshot.child("phone").value?.toString() ?: ""

            binding.textName.text = supplierName
            binding.textDescription.text = supplierDescription
            binding.textLocation.text = supplierLocation

            if (supplierImageUrl.isNotEmpty()) {
                Picasso.get().load(supplierImageUrl).into(binding.imageProvider)
            } else {
                binding.imageProvider.setImageResource(android.R.drawable.ic_menu_gallery)
            }

            supportFragmentManager.beginTransaction()
                .replace(R.id.galleryContainer, GalleryFragment.newInstance(supplierId))
                .commit()
        }
    }

    private fun setupCalendarView() {
        val cv: CalendarView = binding.calendarAvailability
        val currentMonth = YearMonth.now()
        val firstDayOfWeek = firstDayOfWeekFromLocale()

        cv.dayViewResource = R.layout.item_calendar_day

        cv.dayBinder = object : MonthDayBinder<DayViewContainer> {
            override fun create(view: View): DayViewContainer = DayViewContainer(view)

            override fun bind(container: DayViewContainer, day: CalendarDay) {
                val date = day.date
                container.date = date
                container.text.text = date.dayOfMonth.toString()

                val inMonth = day.position == DayPosition.MonthDate
                container.text.alpha = if (inMonth) 1f else 0.35f

                val isAvailable = availableDates.contains(date)
                container.dot.visibility = if (isAvailable && inMonth) View.VISIBLE else View.GONE
            }
        }

        cv.setup(
            startMonth = currentMonth.minusMonths(12),
            endMonth   = currentMonth.plusMonths(12),
            firstDayOfWeek = firstDayOfWeek
        )
        cv.scrollToMonth(currentMonth)
    }

    private fun loadAvailability() {
        val ref = FirebaseRefs.availability(supplierId)

        binding.progressBar.visibility = View.VISIBLE
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<LocalDate>()
                for (child in snapshot.children) {
                    val key = child.key ?: continue
                    try { list.add(LocalDate.parse(key, isoFormatter)) } catch (_: Exception) {}
                }
                availableDates = list.toSet()
                binding.calendarAvailability.notifyCalendarChanged()
                binding.progressBar.visibility = View.GONE
            }
            override fun onCancelled(error: DatabaseError) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(
                    this@ProviderDetailsActivity,
                    "Failed to load availability",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun setupButtons() {
        binding.buttonChat.setOnClickListener {
            startActivity(Intent(this, ChatActivity::class.java).putExtra("supplierId", supplierId))
        }

        binding.buttonContact.setOnClickListener {
            if (supplierPhone.isNotEmpty()) {
                startActivity(Intent(Intent.ACTION_VIEW, "https://wa.me/$supplierPhone".toUri()))
            } else {
                Toast.makeText(this, "No phone number available", Toast.LENGTH_SHORT).show()
            }
        }

        binding.buttonFavorite.setOnClickListener {
            val user = FirebaseAuth.getInstance().currentUser ?: return@setOnClickListener
            val favRef = FirebaseRefs.favorite(user.uid, supplierId)

            if (isFavorite) {
                favRef.removeValue().addOnSuccessListener {
                    isFavorite = false
                    updateFavoriteButton()
                    Toast.makeText(this, "Removed from favorites", Toast.LENGTH_SHORT).show()
                }
            } else {
                val supplierMap = mapOf(
                    "id" to supplierId,
                    "name" to supplierName,
                    "description" to supplierDescription,
                    "location" to supplierLocation,
                    "imageUrl" to supplierImageUrl,
                    "phone" to supplierPhone,
                    "category" to supplierCategory
                )
                favRef.setValue(supplierMap).addOnSuccessListener {
                    isFavorite = true
                    updateFavoriteButton()
                    Toast.makeText(this, "Added to favorites", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.buttonEditSupplier.setOnClickListener {
            startActivity(Intent(this, SupplierDashboardActivity::class.java))
        }
    }

    private fun preloadFavoriteState() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        FirebaseRefs.favorite(user.uid, supplierId).get().addOnSuccessListener { snapshot ->
            isFavorite = snapshot.exists()
            updateFavoriteButton()
        }
    }

    private fun updateFavoriteButton() {
        if (isFavorite) {
            binding.buttonFavorite.text = "Remove from favorites"
            binding.buttonFavorite.icon = ContextCompat.getDrawable(this, R.drawable.ic_favorite_24)
            binding.buttonFavorite.contentDescription = "Remove from favorites"
        } else {
            binding.buttonFavorite.text = "Add to favorites"
            binding.buttonFavorite.icon = ContextCompat.getDrawable(this, R.drawable.ic_favorite_border_24)
            binding.buttonFavorite.contentDescription = "Add to favorites"
        }
    }

    private fun preloadRoleAndOwnerUi() {
        RoleManager.load(object : RoleManager.Callback {
            override fun onRoleLoaded(role: String, supplierId: String?) {
                currentRole = role
                currentUserSupplierId = supplierId
                applyRoleUi()
            }
            override fun onNoUser() {
                currentRole = "client"
                currentUserSupplierId = null
                applyRoleUi()
            }
        })
    }

    private fun applyRoleUi() {
        val isOwner = currentRole == "supplier" && currentUserSupplierId == supplierId
        binding.buttonFavorite.isEnabled = currentRole == "client" && !isOwner
        binding.buttonSubmitReview.isEnabled = currentRole == "client" && !isOwner
        binding.buttonEditSupplier.visibility = if (isOwner) View.VISIBLE else View.GONE
    }

    private fun loadReviews() {
        reviewsRef = FirebaseRefs.reviews(supplierId)
        reviewAdapter = ReviewAdapter(reviewsList)
        binding.recyclerReviews.layoutManager = LinearLayoutManager(this)
        binding.recyclerReviews.adapter = reviewAdapter

        reviewsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                reviewsList.clear()
                for (child in snapshot.children) {
                    val review = child.getValue(Review::class.java)
                    if (review != null) reviewsList.add(review)
                }
                reviewAdapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        binding.buttonSubmitReview.setOnClickListener {
            val rating: Float = binding.ratingBarNewReview.rating
            val comment = binding.editReviewComment.text.toString().trim()
            val user = FirebaseAuth.getInstance().currentUser

            if (user != null && rating > 0f) {
                val reviewId = reviewsRef.push().key ?: System.currentTimeMillis().toString()
                val newReview = Review(
                    userId = user.uid,
                    name = user.displayName ?: "Anonymous",
                    rating = rating,
                    comment = comment,
                    timestamp = System.currentTimeMillis()
                )
                reviewsRef.child(reviewId).setValue(newReview)
                binding.editReviewComment.setText("")
                binding.ratingBarNewReview.rating = 0f
            }
        }
    }
}
