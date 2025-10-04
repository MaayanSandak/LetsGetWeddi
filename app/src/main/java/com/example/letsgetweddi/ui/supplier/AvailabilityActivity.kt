package com.example.letsgetweddi.ui.supplier

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.letsgetweddi.R
import com.example.letsgetweddi.data.FirebaseRefs
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.OutDateStyle
import com.kizitonwose.calendar.view.CalendarView
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.ViewContainer
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

class AvailabilityActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var calendarView: CalendarView
    private lateinit var textEmpty: TextView
    private lateinit var progressBar: ProgressBar


    private lateinit var monthTitle: TextView
    private lateinit var btnPrev: View
    private lateinit var btnNext: View

    private var supplierId: String? = null

    private val availableDates = HashSet<LocalDate>()
    private val busyDates = HashSet<LocalDate>()

    private val fmt: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
    private val headerFmt: DateTimeFormatter =
        DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_availability)

        toolbar = findViewById(R.id.toolbar)
        calendarView = findViewById(R.id.calendarView)
        textEmpty = findViewById(R.id.textEmpty)
        progressBar = findViewById(R.id.progressBar)


        monthTitle = findViewById(R.id.textMonthTitle)
        btnPrev = findViewById(R.id.buttonMonthPrev)
        btnNext = findViewById(R.id.buttonMonthNext)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Availability"
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        supplierId = resolveSupplierId(intent?.data, intent?.getStringExtra("supplierId"))
        if (supplierId.isNullOrBlank()) {
            showLoading(false)
            textEmpty.visibility = View.VISIBLE
            return
        }

        setupCalendarSkeleton()
        loadAvailability(supplierId!!)
    }

    private fun resolveSupplierId(data: Uri?, extra: String?): String? {
        if (!extra.isNullOrBlank()) return extra
        if (data != null && data.scheme == "letsgetweddi") {
            val segs = data.pathSegments
            if (segs.size >= 2 && segs[0].equals("availability", ignoreCase = true)) {
                return segs[1]
            }
        }
        return null
    }

    private fun setupCalendarSkeleton() {
        val currentMonth = YearMonth.now()
        val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek

        calendarView.dayViewResource = R.layout.item_calendar_day
        calendarView.setup(
            currentMonth.minusMonths(12),
            currentMonth.plusMonths(12),
            firstDayOfWeek
        )
        calendarView.scrollToMonth(currentMonth)
        calendarView.outDateStyle = OutDateStyle.EndOfGrid
        monthTitle.text = headerFmt.format(currentMonth)
        calendarView.monthScrollListener = { month ->
            monthTitle.text = headerFmt.format(month.yearMonth)
        }


        btnPrev.setOnClickListener {
            val ym = calendarView.findFirstVisibleMonth()?.yearMonth ?: return@setOnClickListener
            calendarView.smoothScrollToMonth(ym.minusMonths(1))
        }
        btnNext.setOnClickListener {
            val ym = calendarView.findFirstVisibleMonth()?.yearMonth ?: return@setOnClickListener
            calendarView.smoothScrollToMonth(ym.plusMonths(1))
        }

        class DayContainer(view: View) : ViewContainer(view) {
            val text: TextView = view.findViewById(R.id.calendarDayText)
            val dot: View = view.findViewById(R.id.dot)
        }

        calendarView.dayBinder = object : MonthDayBinder<DayContainer> {
            override fun create(view: View): DayContainer = DayContainer(view)

            override fun bind(container: DayContainer, data: CalendarDay) {
                container.text.text = data.date.dayOfMonth.toString()

                if (data.position != DayPosition.MonthDate) {
                    container.text.alpha = 0.3f
                    container.dot.visibility = View.GONE
                    return
                } else {
                    container.text.alpha = 1f
                }

                val isWorkday = data.date.dayOfWeek != DayOfWeek.SATURDAY

                when {
                    isWorkday && busyDates.contains(data.date) -> {
                        container.dot.visibility = View.VISIBLE
                        container.dot.setBackgroundColor(0xFFC62828.toInt()) // red
                    }

                    else -> {
                        container.dot.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun loadAvailability(id: String) {
        showLoading(true)
        FirebaseRefs.availability(id).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                availableDates.clear()
                busyDates.clear()

                for (child in snapshot.children) {
                    val key = child.key ?: continue
                    val v = child.getValue(Boolean::class.java) ?: continue
                    runCatching { LocalDate.parse(key, fmt) }
                        .getOrNull()
                        ?.let { date ->
                            if (v) availableDates.add(date) else busyDates.add(date)
                        }
                }

                showLoading(false)
                textEmpty.visibility =
                    if (availableDates.isEmpty() && busyDates.isEmpty()) View.VISIBLE else View.GONE

                calendarView.notifyCalendarChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                showLoading(false)
                textEmpty.visibility = View.VISIBLE
            }
        })
    }

    private fun showLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        calendarView.visibility = View.VISIBLE
        textEmpty.visibility = View.GONE
    }
}
