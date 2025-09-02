package com.example.diabetesmanager
import androidx.core.widget.NestedScrollView
import android.text.TextWatcher
import android.text.Editable
import android.os.Handler
import android.os.Looper
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.view.ViewGroup
import android.view.Gravity
import android.content.Context
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
data class MonthlyTableRow(
    val day: Int,
    val date: String,
    val fastingValue: String,
    val morningValue: String,
    val afternoonValue: String,
    val eveningValue: String
)

class MonthlyTableAdapter(
    private val context: Context,
    private val onCellClick: (date: String, timeSlot: String, currentValue: String) -> Unit
) : RecyclerView.Adapter<MonthlyTableAdapter.ViewHolder>() {

    private var rows = listOf<MonthlyTableRow>()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dayText: TextView = view.findViewById(R.id.dayText)
        val fastingCell: EditText = view.findViewById(R.id.fastingCell)
        val morningCell: EditText = view.findViewById(R.id.morningCell)
        val afternoonCell: EditText = view.findViewById(R.id.afternoonCell)
        val eveningCell: EditText = view.findViewById(R.id.eveningCell)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.monthly_table_row_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val row = rows[position]

        // Set day text
        holder.dayText.text = row.day.toString()

        // Bind glucose cells
        bindGlucoseCell(holder.fastingCell, row.fastingValue, "Fasting", row.date)
        bindGlucoseCell(holder.morningCell, row.morningValue, "Morning", row.date)
        bindGlucoseCell(holder.afternoonCell, row.afternoonValue, "Afternoon", row.date)
        bindGlucoseCell(holder.eveningCell, row.eveningValue, "Evening", row.date)
    }

    private fun bindGlucoseCell(cell: EditText, value: String, timeSlot: String, date: String) {
        // Set value
        cell.setText(if (value == "-") "" else value)

        // Set color based on value and time slot
        if (value != "-" && value.isNotEmpty()) {
            try {
                val glucoseValue = value.toFloat()
                setGlucoseTextColor(cell, glucoseValue, timeSlot)
            } catch (e: NumberFormatException) {
                cell.setTextColor(Color.BLACK)
            }
        } else {
            cell.setTextColor(Color.BLACK)
        }

        // Set click listener to open edit dialog
        cell.setOnClickListener {
            onCellClick(date, timeSlot, value)
        }
    }

    private fun setGlucoseTextColor(editText: EditText, value: Float, timeSlot: String) {
        when (timeSlot) {
            "Fasting" -> {
                when {
                    value < 80 -> editText.setTextColor(Color.RED)
                    value in 80.0..120.0 -> editText.setTextColor(Color.parseColor("#4CAF50")) // Green
                    value in 121.0..140.0 -> editText.setTextColor(Color.parseColor("#FF9800")) // Orange
                    value > 140 -> editText.setTextColor(Color.RED)
                    else -> editText.setTextColor(Color.BLACK)
                }
            }
            "Morning", "Afternoon", "Evening" -> {
                when {
                    value < 100 -> editText.setTextColor(Color.RED)
                    value in 100.0..160.0 -> editText.setTextColor(Color.parseColor("#4CAF50")) // Green
                    value in 161.0..180.0 -> editText.setTextColor(Color.parseColor("#FF9800")) // Orange
                    value > 180 -> editText.setTextColor(Color.RED)
                    else -> editText.setTextColor(Color.BLACK)
                }
            }
            else -> editText.setTextColor(Color.BLACK)
        }
    }

    override fun getItemCount() = rows.size

    fun updateData(newRows: List<MonthlyTableRow>) {
        rows = newRows
        notifyDataSetChanged()
    }
}


class MainActivity : AppCompatActivity() {
    // Database
    // Add this variable to track manual date selections
    // Add this variable to your MainActivity class
    private lateinit var mainScrollView: NestedScrollView
    private lateinit var monthlyRecyclerView: RecyclerView
    private lateinit var monthlyTableAdapter: MonthlyTableAdapter
    private var isManualDateSelection = false

    private lateinit var inlineFormScrollView: ScrollView
    // Add these new variables for the inline form
    private lateinit var inlineGlucoseForm: LinearLayout
    private lateinit var selectedDateHeader: TextView
    private lateinit var fastingInput: EditText
    private lateinit var morningInput: EditText
    private lateinit var afternoonInput: EditText
    private lateinit var eveningInput: EditText
    private lateinit var saveFormButton: Button
    private lateinit var clearFormButton: Button

    private var currentSelectedDate: String = ""

    // Add these new variables at the top with your other variables
    private lateinit var horizontalDatePickerContainer: LinearLayout
    private lateinit var prevMonthButtonCalendar: Button
    private lateinit var nextMonthButtonCalendar: Button
    private lateinit var currentMonthTextCalendar: TextView
    private lateinit var currentYearTextCalendar: TextView
    private lateinit var horizontalDateScroll: HorizontalScrollView
    private lateinit var horizontalDateContainer: LinearLayout

    private var currentCalendarYear = Calendar.getInstance().get(Calendar.YEAR)
    private var currentCalendarMonth = Calendar.getInstance().get(Calendar.MONTH)
    private lateinit var currentYearText: TextView
    private lateinit var database: GlucoseDatabase
    private lateinit var dao: GlucoseDao

    private lateinit var statusText: TextView
    private lateinit var viewMonthlyHistoryButton: Button
    private lateinit var monthlyHistoryContainer: LinearLayout
    private lateinit var yearSpinner: Spinner
    private lateinit var prevMonthButton: Button
    private lateinit var nextMonthButton: Button
    private lateinit var currentMonthText: TextView

    private var selectedDate: String = ""
    private val readings = mutableListOf<GlucoseReading>()
    private var isMonthlyViewVisible = false
    private var currentDisplayYear = 2025
    private var currentDisplayMonth = Calendar.getInstance().get(Calendar.MONTH)

    private val months = arrayOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize database FIRST
        database = GlucoseDatabase.getDatabase(this)
        dao = database.glucoseDao()

        initializeViews()
        setupHorizontalDatePicker()
        setupInlineForm()
        setupRealTimeColorUpdates()
        setupButtons()
        setupYearSpinner()
        setupMonthNavigation() // ✅ ENSURE THIS IS CALLED
        setupRecyclerView()
        loadDataFromDatabase()
        // Set initial button text
        viewMonthlyHistoryButton.text = "Monthly Logs"

        // Initialize calendar variables
        val today = Calendar.getInstance()
        currentCalendarMonth = today.get(Calendar.MONTH)
        currentCalendarYear = today.get(Calendar.YEAR)

        // Rest of initialization...

        updateDisplay()
    }

    private fun setupRecyclerView() {
        monthlyRecyclerView = findViewById(R.id.monthlyRecyclerView)

        // Setup RecyclerView with LinearLayoutManager for smooth scrolling
        val layoutManager = LinearLayoutManager(this)
        monthlyRecyclerView.layoutManager = layoutManager
        monthlyRecyclerView.setHasFixedSize(true) // Performance optimization

        // Initialize adapter with cell click handler
        monthlyTableAdapter = MonthlyTableAdapter(this) { date, timeSlot, currentValue ->
            handleCellClick(date, timeSlot, currentValue)
        }

        monthlyRecyclerView.adapter = monthlyTableAdapter
    }

    private fun handleCellClick(date: String, timeSlot: String, currentValue: String) {
        val existingReading = readings.find { it.date == date && it.timeSlot == timeSlot }

        if (existingReading != null) {
            // Edit existing reading
            showEditDialog(existingReading, dbDateToDisplayDate(date))
        } else {
            // Create new reading
            val newReading = GlucoseReading(
                id = 0,
                date = date,
                timeSlot = timeSlot,
                glucoseLevel = 0f,
                timestamp = System.currentTimeMillis()
            )
            showEditDialog(newReading, dbDateToDisplayDate(date))
        }
    }


    // Add this helper method
    private fun getCurrentDateString(): String {
        val today = Calendar.getInstance()
        return String.format("%02d-%02d-%04d",
            today.get(Calendar.DAY_OF_MONTH),
            today.get(Calendar.MONTH) + 1,
            today.get(Calendar.YEAR))
    }

    // Cache frequently used views
    private var cachedRows = mutableMapOf<Int, LinearLayout>()

    // Optimize cell creation by reusing views when possible
    private fun createOptimizedTableCell(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            gravity = Gravity.CENTER
            setPadding(8, 12, 8, 12)
            textSize = 13f
            setBackgroundColor(Color.parseColor("#F5F5F5"))
            setTextColor(Color.BLACK)
            setTypeface(typeface, android.graphics.Typeface.BOLD)

            // Pre-create border drawable
            background = createBorderDrawable()
        }
    }

    private fun createBorderDrawable(): GradientDrawable {
        return GradientDrawable().apply {
            setStroke(1, Color.GRAY)
            setColor(Color.parseColor("#F5F5F5"))
        }
    }

    private fun autoLoadCurrentDate() {
        val today = Calendar.getInstance()
        val currentDay = today.get(Calendar.DAY_OF_MONTH)
        val currentMonth = today.get(Calendar.MONTH)
        val currentYear = today.get(Calendar.YEAR)

        // Only auto-load if we're viewing current month AND no manual selection
        if (currentCalendarMonth == currentMonth && currentCalendarYear == currentYear && !isManualDateSelection) {
            val todayString = String.format("%02d-%02d-%04d", currentDay, currentMonth + 1, currentYear)

            loadDateIntoForm(todayString)
            updateSelectedDate(todayString)

            // Find and highlight today's button
            horizontalDateContainer.post {
                for (i in 0 until horizontalDateContainer.childCount) {
                    val button = horizontalDateContainer.getChildAt(i) as? Button
                    if (button != null && button.text.toString().toInt() == currentDay) {
                        highlightSelectedButton(button)
                        centerDateButton(button)
                        break
                    }
                }
            }
        }

        // Reset flag after auto-load check
        isManualDateSelection = false
    }

    private fun setGlucoseTextColor(editText: EditText, value: Float, timeSlot: String) {
        when (timeSlot) {
            "Fasting" -> {
                when {
                    value < 80 -> editText.setTextColor(Color.RED)
                    value in 80.0..120.0 -> editText.setTextColor(Color.parseColor("#4CAF50")) // Green
                    value in 121.0..140.0 -> editText.setTextColor(Color.parseColor("#FF9800")) // Orange
                    value > 140 -> editText.setTextColor(Color.RED)
                    else -> editText.setTextColor(Color.BLACK)
                }
            }
            "Morning", "Afternoon", "Evening" -> {
                when {
                    value < 100 -> editText.setTextColor(Color.RED)
                    value in 100.0..160.0 -> editText.setTextColor(Color.parseColor("#4CAF50")) // Green
                    value in 161.0..180.0 -> editText.setTextColor(Color.parseColor("#FF9800")) // Orange
                    value > 180 -> editText.setTextColor(Color.RED)
                    else -> editText.setTextColor(Color.BLACK)
                }
            }
            else -> editText.setTextColor(Color.BLACK)
        }
    }

    private fun todayString(): String =
        SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())

    private fun loadDataFromDatabase() {
        lifecycleScope.launch(Dispatchers.IO) {
            val dbReadings = dao.getAllReadings()
            readings.clear()
            readings.addAll(dbReadings.map { it.toGlucoseReading() })

            withContext(Dispatchers.Main) {
                if (isMonthlyViewVisible) updateMonthlyDisplay()
                updateDisplay()                         // refresh charts/lists
                loadDateIntoForm(todayString())         // ← auto-fills TODAY
            }
        }
    }


    private fun initializeViews() {
        mainScrollView = findViewById(R.id.mainScrollView)
        //calendarView = findViewById(R.id.calendarView)
        statusText = findViewById(R.id.statusText)
        //addButton = findViewById(R.id.addReadingButton)
        viewMonthlyHistoryButton = findViewById(R.id.viewMonthlyHistoryButton)
        monthlyHistoryContainer = findViewById(R.id.monthlyHistoryContainer)
        //monthlyTableContainer = findViewById(R.id.monthlyTableContainer)
        yearSpinner = findViewById(R.id.yearSpinner)
        prevMonthButton = findViewById(R.id.prevMonthButton)
        nextMonthButton = findViewById(R.id.nextMonthButton)
        currentMonthText = findViewById(R.id.currentMonthText)
        currentYearText = findViewById(R.id.currentYearText)  // Add this line

        // Add new horizontal date picker views
        horizontalDatePickerContainer = findViewById(R.id.horizontalDatePickerContainer)
        prevMonthButtonCalendar = findViewById(R.id.prevMonthButtonCalendar)
        nextMonthButtonCalendar = findViewById(R.id.nextMonthButtonCalendar)
        currentMonthTextCalendar = findViewById(R.id.currentMonthTextCalendar)
        currentYearTextCalendar = findViewById(R.id.currentYearTextCalendar)
        horizontalDateScroll = findViewById(R.id.horizontalDateScroll)
        horizontalDateContainer = findViewById(R.id.horizontalDateContainer)
        //inlineFormScrollView = findViewById(R.id.inlineFormScrollView)
        // ADD THESE NEW LINES for inline form
        inlineGlucoseForm = findViewById(R.id.inlineGlucoseForm)
        selectedDateHeader = findViewById(R.id.selectedDateHeader)
        fastingInput = findViewById(R.id.fastingInput)
        morningInput = findViewById(R.id.morningInput)
        afternoonInput = findViewById(R.id.afternoonInput)
        eveningInput = findViewById(R.id.eveningInput)
        saveFormButton = findViewById(R.id.saveFormButton)
        clearFormButton = findViewById(R.id.clearFormButton)
    }

    private fun setupInlineForm() {
        saveFormButton.setOnClickListener {
            saveInlineFormData()
        }

        clearFormButton.setOnClickListener {
            clearInlineForm()
        }
    }

    private fun loadDateIntoForm(dateString: String) {
        currentSelectedDate = dateString

        val today = Calendar.getInstance()
        val todayString = String.format("%02d-%02d-%04d",
            today.get(Calendar.DAY_OF_MONTH),
            today.get(Calendar.MONTH) + 1,
            today.get(Calendar.YEAR))

        if (dateString == todayString) {
            selectedDateHeader.text = "Today's Readings ($dateString)"
            selectedDateHeader.setBackgroundColor(Color.parseColor("#4CAF50"))
        } else {
            selectedDateHeader.text = "Readings for $dateString"
            selectedDateHeader.setBackgroundColor(Color.parseColor("#E8F5E8"))
        }

        clearInlineForm()

        val dbDate = displayDateToDbDate(dateString)
        val dayReadings = readings.filter { it.date == dbDate }

        dayReadings.forEach { reading ->
            when (reading.timeSlot) {
                "Fasting" -> {
                    val glucoseValue = reading.glucoseLevel.toInt().toString()
                    fastingInput.setText(glucoseValue)
                    fastingInput.setBackgroundColor(Color.parseColor("#E8F5E8"))
                    setGlucoseTextColor(fastingInput, reading.glucoseLevel, "Fasting")
                }
                "Morning" -> {
                    val glucoseValue = reading.glucoseLevel.toInt().toString()
                    morningInput.setText(glucoseValue)
                    morningInput.setBackgroundColor(Color.parseColor("#E8F5E8"))
                    setGlucoseTextColor(morningInput, reading.glucoseLevel, "Morning")
                }
                "Afternoon" -> {
                    val glucoseValue = reading.glucoseLevel.toInt().toString()
                    afternoonInput.setText(glucoseValue)
                    afternoonInput.setBackgroundColor(Color.parseColor("#E8F5E8"))
                    setGlucoseTextColor(afternoonInput, reading.glucoseLevel, "Afternoon")
                }
                "Evening" -> {
                    val glucoseValue = reading.glucoseLevel.toInt().toString()
                    eveningInput.setText(glucoseValue)
                    eveningInput.setBackgroundColor(Color.parseColor("#E8F5E8"))
                    setGlucoseTextColor(eveningInput, reading.glucoseLevel, "Evening")
                }
            }
        }
    }

    private fun setupRealTimeColorUpdates() {
        // Fasting input
        fastingInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString()?.trim()
                if (!text.isNullOrEmpty()) {
                    try {
                        val value = text.toFloat()
                        if (value in 20.0..600.0) {
                            setGlucoseTextColor(fastingInput, value, "Fasting")
                        } else {
                            fastingInput.setTextColor(Color.BLACK)
                        }
                    } catch (e: NumberFormatException) {
                        fastingInput.setTextColor(Color.BLACK)
                    }
                } else {
                    fastingInput.setTextColor(Color.BLACK)
                }
            }
        })

        // Morning input
        morningInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString()?.trim()
                if (!text.isNullOrEmpty()) {
                    try {
                        val value = text.toFloat()
                        if (value in 20.0..600.0) {
                            setGlucoseTextColor(morningInput, value, "Morning")
                        } else {
                            morningInput.setTextColor(Color.BLACK)
                        }
                    } catch (e: NumberFormatException) {
                        morningInput.setTextColor(Color.BLACK)
                    }
                } else {
                    morningInput.setTextColor(Color.BLACK)
                }
            }
        })

        // Afternoon input
        afternoonInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString()?.trim()
                if (!text.isNullOrEmpty()) {
                    try {
                        val value = text.toFloat()
                        if (value in 20.0..600.0) {
                            setGlucoseTextColor(afternoonInput, value, "Afternoon")
                        } else {
                            afternoonInput.setTextColor(Color.BLACK)
                        }
                    } catch (e: NumberFormatException) {
                        afternoonInput.setTextColor(Color.BLACK)
                    }
                } else {
                    afternoonInput.setTextColor(Color.BLACK)
                }
            }
        })

        // Evening input
        eveningInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString()?.trim()
                if (!text.isNullOrEmpty()) {
                    try {
                        val value = text.toFloat()
                        if (value in 20.0..600.0) {
                            setGlucoseTextColor(eveningInput, value, "Evening")
                        } else {
                            eveningInput.setTextColor(Color.BLACK)
                        }
                    } catch (e: NumberFormatException) {
                        eveningInput.setTextColor(Color.BLACK)
                    }
                } else {
                    eveningInput.setTextColor(Color.BLACK)
                }
            }
        })
    }



    private fun clearInlineForm() {
        val inputs = listOf(fastingInput, morningInput, afternoonInput, eveningInput)

        inputs.forEach { input ->
            input.setText("")
            input.setTextColor(Color.BLACK) // Reset to default color
            input.setBackgroundResource(android.R.drawable.edit_text)
        }
    }


    private fun saveInlineFormData() {
        if (currentSelectedDate.isEmpty()) return

        lifecycleScope.launch {
            var hasChanges = false
            var allValid = true

            val inputs = mapOf(
                "Fasting" to fastingInput,
                "Morning" to morningInput,
                "Afternoon" to afternoonInput,
                "Evening" to eveningInput
            )

            for ((slot, input) in inputs) {
                val glucoseText = input.text.toString().trim()
                if (glucoseText.isNotEmpty()) {
                    val glucoseVal = try {
                        glucoseText.toFloat()
                    } catch (e: NumberFormatException) {
                        null
                    }

                    if (glucoseVal == null || glucoseVal < 20 || glucoseVal > 600) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, "Invalid $slot glucose (20-600)", Toast.LENGTH_SHORT).show()
                        }
                        allValid = false
                        break
                    } else {
                        hasChanges = true
                        saveGlucoseReadingSilent(currentSelectedDate, slot, glucoseVal)
                    }
                }
            }

            withContext(Dispatchers.Main) {
                if (allValid) {
                    if (hasChanges) {
                        Toast.makeText(this@MainActivity, "Readings saved successfully!", Toast.LENGTH_SHORT).show()
                        if (isMonthlyViewVisible) updateMonthlyDisplay()
                        updateDisplay()
                        // FIXED: Don't scroll away from manually selected date
                        populateHorizontalDates() // Only refresh colors, don't auto-scroll
                    } else {
                        Toast.makeText(this@MainActivity, "No changes made.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }



    // Add these new methods for the horizontal date picker
    private fun setupHorizontalDatePicker() {
        prevMonthButtonCalendar.setOnClickListener {
            navigateCalendarMonth(-1)
        }

        nextMonthButtonCalendar.setOnClickListener {
            navigateCalendarMonth(1)
        }

        updateHorizontalDatePicker()
    }

    private fun navigateCalendarMonth(direction: Int) {
        currentCalendarMonth += direction

        if (currentCalendarMonth < 0) {
            currentCalendarMonth = 11
            currentCalendarYear--
        } else if (currentCalendarMonth > 11) {
            currentCalendarMonth = 0
            currentCalendarYear++
        }

        updateHorizontalDatePicker()

        // Auto-load current date if we switched to the current month/year
        val today = Calendar.getInstance()
        val currentMonth = today.get(Calendar.MONTH)
        val currentYear = today.get(Calendar.YEAR)

        if (currentCalendarMonth == currentMonth && currentCalendarYear == currentYear) {
            // We're viewing current month - auto-load today's data
            horizontalDateContainer.postDelayed({
                autoLoadCurrentDate()
            }, 300)
        } else {
            // We're viewing a different month - clear form and show month header
            clearInlineForm()
            selectedDateHeader.text = "Select a date"
            currentSelectedDate = ""
        }
    }

    private fun setupAutoRefresh() {
        // Check every minute if date has changed (for apps left open overnight)
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                val today = Calendar.getInstance()
                val currentMonth = today.get(Calendar.MONTH)
                val currentYear = today.get(Calendar.YEAR)

                // If we're viewing current month and it's a new day, refresh
                if (currentCalendarMonth == currentMonth && currentCalendarYear == currentYear) {
                    autoLoadCurrentDate()
                }

                // Schedule next check in 1 minute
                handler.postDelayed(this, 60000) // 60 seconds
            }
        }

        // Start the periodic check
        handler.postDelayed(runnable, 60000)
    }



    private fun updateHorizontalDatePicker() {
        currentMonthTextCalendar.text = months[currentCalendarMonth]
        currentYearTextCalendar.text = currentCalendarYear.toString()

        populateHorizontalDates()
    }

    private fun populateHorizontalDates() {
        horizontalDateContainer.removeAllViews()

        val calendar = Calendar.getInstance()
        calendar.set(currentCalendarYear, currentCalendarMonth, 1)
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Get current date for comparison
        val today = Calendar.getInstance()
        val todayYear = today.get(Calendar.YEAR)
        val todayMonth = today.get(Calendar.MONTH)
        val todayDay = today.get(Calendar.DAY_OF_MONTH)

        for (day in 1..daysInMonth) {
            val dateButton = createDateButton(day)

            // Check if this date is in the future
            val isFutureDate = when {
                currentCalendarYear > todayYear -> true
                currentCalendarYear == todayYear && currentCalendarMonth > todayMonth -> true
                currentCalendarYear == todayYear && currentCalendarMonth == todayMonth && day > todayDay -> true
                else -> false
            }

            if (isFutureDate) {
                // Disable future dates
                dateButton.isEnabled = false
                dateButton.alpha = 0.3f
            } else {
                // Enable past and current dates
                dateButton.isEnabled = true
                dateButton.alpha = 1.0f

                // Manual click handler (for switching between dates)
                dateButton.setOnClickListener {
                    val selectedDateString = String.format("%02d-%02d-%04d", day, currentCalendarMonth + 1, currentCalendarYear)

                    // Set flag to indicate manual selection
                    isManualDateSelection = true

                    loadDateIntoForm(selectedDateString)
                    updateSelectedDate(selectedDateString)
                    centerDateButton(dateButton)
                    highlightSelectedButton(dateButton)
                }
            }

            horizontalDateContainer.addView(dateButton)
        }

        // AUTOMATICALLY load current date data after all buttons are created
        horizontalDateContainer.post {
            autoLoadCurrentDate()
        }
    }


    private fun highlightSelectedButton(selectedButton: Button) {
        // Reset all buttons to default style
        for (i in 0 until horizontalDateContainer.childCount) {
            val button = horizontalDateContainer.getChildAt(i) as Button
            if (button.isEnabled) {
                // Check if this date has readings for color
                val day = button.text.toString().toInt()
                val dbDate = String.format("%04d-%02d-%02d", currentCalendarYear, currentCalendarMonth + 1, day)
                val hasReadings = readings.any { it.date == dbDate }

                val drawable = GradientDrawable()
                if (hasReadings) {
                    drawable.setColor(Color.parseColor("#4CAF50")) // Green for data
                    button.setTextColor(Color.WHITE)
                } else {
                    drawable.setColor(Color.parseColor("#E0E0E0")) // Gray for no data
                    drawable.setStroke(2, Color.parseColor("#CCCCCC"))
                    button.setTextColor(Color.BLACK)
                }
                drawable.cornerRadius = 30f
                button.background = drawable
            }
        }

        // Highlight the selected button with blue border
        val selectedDrawable = GradientDrawable()
        val day = selectedButton.text.toString().toInt()
        val dbDate = String.format("%04d-%02d-%02d", currentCalendarYear, currentCalendarMonth + 1, day)
        val hasReadings = readings.any { it.date == dbDate }

        if (hasReadings) {
            selectedDrawable.setColor(Color.parseColor("#4CAF50")) // Keep green background
            selectedButton.setTextColor(Color.WHITE)
        } else {
            selectedDrawable.setColor(Color.parseColor("#E0E0E0")) // Keep gray background
            selectedButton.setTextColor(Color.BLACK)
        }

        // Add thick blue border to show selection
        selectedDrawable.setStroke(6, Color.parseColor("#2196F3"))
        selectedDrawable.cornerRadius = 30f
        selectedButton.background = selectedDrawable
    }


    private fun createDateButton(day: Int): Button {
        val button = Button(this)
        val size = (60 * resources.displayMetrics.density).toInt()

        val params = LinearLayout.LayoutParams(size, size)
        params.setMargins(8, 8, 8, 8)
        button.layoutParams = params

        button.text = day.toString()
        button.textSize = 14f
        button.setTypeface(null, android.graphics.Typeface.BOLD)

        // Check if this date has readings
        val dbDate = String.format("%04d-%02d-%02d", currentCalendarYear, currentCalendarMonth + 1, day)
        val hasReadings = readings.any { it.date == dbDate }

        if (hasReadings) {
            // Date with readings - green background
            val drawable = GradientDrawable()
            drawable.setColor(Color.parseColor("#4CAF50"))
            drawable.cornerRadius = 30f
            button.background = drawable
            button.setTextColor(Color.WHITE)
        } else {
            // Date without readings - light background
            val drawable = GradientDrawable()
            drawable.setColor(Color.parseColor("#E0E0E0"))
            drawable.cornerRadius = 30f
            drawable.setStroke(2, Color.parseColor("#CCCCCC"))
            button.background = drawable
            button.setTextColor(Color.BLACK)
        }

        return button
    }

    private fun updateSelectedDate(dateString: String) {
        selectedDate = dateString
        updateDisplay()
    }

    /*
    private fun setupCalendar() {
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance()
            calendar.set(year, month, dayOfMonth)
            updateSelectedDate(calendar.timeInMillis)
        }
    }
     */

    private fun setupButtons() {

        viewMonthlyHistoryButton.setOnClickListener {
            toggleMonthlyView()
        }

        // Add long press for advanced options
        viewMonthlyHistoryButton.setOnLongClickListener {
            showAdvancedOptionsDialog()
            true
        }
    }

    private fun showAdvancedOptionsDialog() {
        val options = arrayOf(
            "📊 View Statistics",
            "📤 Export to CSV",
            "🗑️ Clear All Data"
        )

        AlertDialog.Builder(this)
            .setTitle("Advanced Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showStatisticsDialog()
                    1 -> Toast.makeText(this, "CSV Export coming soon!", Toast.LENGTH_SHORT).show()
                    2 -> showClearDataConfirmation()
                }
            }
            .show()
    }

    private fun showStatisticsDialog() {
        lifecycleScope.launch {
            val totalReadings = dao.getTotalReadingsCount()

            // Calculate normal readings based on new ranges
            val fastingNormalCount = readings.count {
                it.timeSlot == "Fasting" && it.glucoseLevel in 80f..120f
            }
            val otherNormalCount = readings.count {
                it.timeSlot != "Fasting" && it.glucoseLevel in 100f..160f
            }
            val totalNormalReadings = fastingNormalCount + otherNormalCount

            // Get last 30 days average (use database format for queries)
            val calendar = Calendar.getInstance()
            val endDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
            calendar.add(Calendar.DAY_OF_YEAR, -30)
            val startDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

            val avgGlucose = dao.getAverageGlucose(startDate, endDate) ?: 0f
            val normalPercentage = if (totalReadings > 0) (totalNormalReadings * 100f / totalReadings) else 0f

            // Calculate separate percentages for fasting and other readings
            val totalFastingReadings = readings.count { it.timeSlot == "Fasting" }
            val totalOtherReadings = readings.count { it.timeSlot != "Fasting" }

            val fastingPercentage = if (totalFastingReadings > 0) (fastingNormalCount * 100f / totalFastingReadings) else 0f
            val otherPercentage = if (totalOtherReadings > 0) (otherNormalCount * 100f / totalOtherReadings) else 0f

            runOnUiThread {
                val message = """
                📊 Your Diabetes Statistics:
                
                📈 Overall Summary:
                Total Readings: $totalReadings
                Normal Readings: $totalNormalReadings (${normalPercentage.toInt()}%)
                
                🍽️ Breakdown by Time:
                • Fasting Normal: $fastingNormalCount/${totalFastingReadings} (${fastingPercentage.toInt()}%)
                • Post-meal Normal: $otherNormalCount/${totalOtherReadings} (${otherPercentage.toInt()}%)
                
                📅 Last 30 Days:
                Average Glucose: ${avgGlucose.toInt()} mg/dL
                
                🎯 Target Ranges:
                • Fasting: 80-120 mg/dL
                • Post-meal: 100-160 mg/dL
                
                💪 Your Control: ${when {
                    normalPercentage >= 80 -> "Excellent! 🌟"
                    normalPercentage >= 70 -> "Very Good 👍"
                    normalPercentage >= 60 -> "Good 😊"
                    normalPercentage >= 50 -> "Fair 😐"
                    else -> "Needs Improvement 📈"
                }}
            """.trimIndent()

                AlertDialog.Builder(this@MainActivity)
                    .setTitle("📈 Health Statistics")
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }


    private fun showClearDataConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("⚠️ Clear All Data")
            .setMessage("This will permanently delete ALL your glucose readings. This action cannot be undone.\n\nAre you sure?")
            .setPositiveButton("DELETE ALL") { _, _ ->
                lifecycleScope.launch {
                    dao.deleteAllReadings()
                    readings.clear()
                    runOnUiThread {
                        updateDisplay()
                        if (isMonthlyViewVisible) {
                            updateMonthlyDisplay()
                        }
                        Toast.makeText(this@MainActivity, "All data cleared", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupYearSpinner() {
        val years = (2020..2050).map { it.toString() }.toTypedArray()

        // Use simple ArrayAdapter without custom overrides to avoid errors
        val yearAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, years)
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        yearSpinner.adapter = yearAdapter
        yearSpinner.setSelection(years.indexOf(currentDisplayYear.toString()))

        yearSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Style the selected view to make it more visible
                (view as? TextView)?.let { textView ->
                    textView.setTextColor(Color.BLACK)
                    textView.textSize = 14f
                    textView.gravity = android.view.Gravity.CENTER
                }

                currentDisplayYear = years[position].toInt()
                if (isMonthlyViewVisible) {
                    updateMonthlyDisplay()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }


    private fun setupMonthNavigation() {
        // Previous month button
        prevMonthButton.setOnClickListener {
            try {
                currentCalendarMonth--
                if (currentCalendarMonth < 0) {
                    currentCalendarMonth = 11
                    currentCalendarYear--
                }
                updateMonthlyDisplay()
                //Toast.makeText(this, "Switched to ${months[currentCalendarMonth]} $currentCalendarYear", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Error navigating to previous month", Toast.LENGTH_SHORT).show()
            }
        }

        // Next month button
        nextMonthButton.setOnClickListener {
            try {
                currentCalendarMonth++
                if (currentCalendarMonth > 11) {
                    currentCalendarMonth = 0
                    currentCalendarYear++
                }
                updateMonthlyDisplay()
                //Toast.makeText(this, "Switched to ${months[currentCalendarMonth]} $currentCalendarYear", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Error navigating to next month", Toast.LENGTH_SHORT).show()
            }
        }

        // Year spinner change listener
        yearSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedYear = yearSpinner.selectedItem.toString().toIntOrNull()
                if (selectedYear != null && selectedYear != currentCalendarYear) {
                    currentCalendarYear = selectedYear
                    updateMonthlyDisplay()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }


    private fun navigateMonth(direction: Int) {
        currentDisplayMonth += direction

        if (currentDisplayMonth < 0) {
            currentDisplayMonth = 11
            currentDisplayYear--
            updateYearSpinner()
        } else if (currentDisplayMonth > 11) {
            currentDisplayMonth = 0
            currentDisplayYear++
            updateYearSpinner()
        }

        updateMonthlyDisplay()
    }

    private fun updateYearSpinner() {
        val years = (2020..2050).map { it.toString() }.toTypedArray()
        yearSpinner.setSelection(years.indexOf(currentDisplayYear.toString()))
    }

    private fun toggleMonthlyView() {
        try {
            isMonthlyViewVisible = !isMonthlyViewVisible

            if (isMonthlyViewVisible) {
                // Show monthly table view
                monthlyHistoryContainer.visibility = View.VISIBLE
                horizontalDatePickerContainer.visibility = View.GONE
                viewMonthlyHistoryButton.text = "Enter Readings"
                updateMonthlyDisplay()
            } else {
                // Show horizontal date picker view
                monthlyHistoryContainer.visibility = View.GONE
                horizontalDatePickerContainer.visibility = View.VISIBLE
                viewMonthlyHistoryButton.text = "Monthly Logs"
                updateHorizontalDatePicker()
                // Auto-scroll to current date when switching back
                horizontalDateScroll.postDelayed({
                    autoLoadCurrentDate()
                }, 300)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error switching views: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateMonthlyDisplay() {
        // Update month/year text
        currentMonthText.text = months[currentCalendarMonth]
        currentYearText.text = currentCalendarYear.toString()

        // Generate data for RecyclerView
        lifecycleScope.launch(Dispatchers.Default) {
            val calendar = Calendar.getInstance()
            calendar.set(currentCalendarYear, currentCalendarMonth, 1)
            val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

            // Create readings map for efficient lookup
            val monthReadings = readings.filter {
                it.date.startsWith("$currentCalendarYear-${String.format("%02d", currentCalendarMonth + 1)}")
            }.associateBy { "${it.date}-${it.timeSlot}" }

            // Build rows data
            val rows = mutableListOf<MonthlyTableRow>()

            for (day in 1..daysInMonth) {
                val dbDate = String.format("%04d-%02d-%02d", currentCalendarYear, currentCalendarMonth + 1, day)

                val fastingReading = monthReadings["$dbDate-Fasting"]
                val morningReading = monthReadings["$dbDate-Morning"]
                val afternoonReading = monthReadings["$dbDate-Afternoon"]
                val eveningReading = monthReadings["$dbDate-Evening"]

                val row = MonthlyTableRow(
                    day = day,
                    date = dbDate,
                    fastingValue = fastingReading?.glucoseLevel?.toInt()?.toString() ?: "-",
                    morningValue = morningReading?.glucoseLevel?.toInt()?.toString() ?: "-",
                    afternoonValue = afternoonReading?.glucoseLevel?.toInt()?.toString() ?: "-",
                    eveningValue = eveningReading?.glucoseLevel?.toInt()?.toString() ?: "-"
                )

                rows.add(row)
            }

            // Update adapter on main thread
            withContext(Dispatchers.Main) {
                monthlyTableAdapter.updateData(rows)
            }
        }
    }




    private fun populateMonthlyTable() {
        //monthlyTableContainer.removeAllViews()

        val calendar = Calendar.getInstance()
        calendar.set(currentDisplayYear, currentDisplayMonth, 1)
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        for (day in 1..daysInMonth) {
            // Create database format date for querying
            val dbDateString = String.format("%04d-%02d-%02d", currentDisplayYear, currentDisplayMonth + 1, day)

            // Create display format date for showing
            val displayDateString = String.format("%02d-%02d-%04d", day, currentDisplayMonth + 1, currentDisplayYear)

            val dayReadings = readings.filter { it.date == dbDateString }

            //val row = createMonthlyTableRow(day)
            //monthlyTableContainer.addView(row)
        }
    }

    private fun centerDateButton(dateButton: View) {
        horizontalDateScroll.post {
            val scrollX = dateButton.left - (horizontalDateScroll.width / 2) + (dateButton.width / 2)
            // Ensure we don't scroll past the boundaries
            val maxScroll = horizontalDateContainer.width - horizontalDateScroll.width
            val finalScrollX = when {
                scrollX < 0 -> 0
                scrollX > maxScroll -> maxScroll
                else -> scrollX
            }
            horizontalDateScroll.smoothScrollTo(finalScrollX, 0)
        }
    }

    private fun createTableCell(text: String): TextView {
        val cell = TextView(this)
        cell.text = text
        cell.gravity = Gravity.CENTER
        cell.setPadding(8, 8, 8, 8) // FIXED: Use setPadding() instead of .padding
        cell.textSize = 11f
        cell.setBackgroundColor(Color.WHITE)
        cell.setTextColor(Color.BLACK)

        // Add border
        val border = GradientDrawable()
        border.setStroke(1, Color.GRAY)
        border.setColor(Color.WHITE)
        cell.background = border

        return cell
    }

    private suspend fun saveGlucoseReadingSilent(date: String, timeSlot: String, glucoseLevel: Float) {
        if (glucoseLevel < 20 || glucoseLevel > 600) {
            return
        }

        val dbDate = displayDateToDbDate(date)
        val existingReading = readings.find { it.date == dbDate && it.timeSlot == timeSlot }

        if (existingReading != null) {
            // Update existing reading - but call silent version
            updateGlucoseReadingSilent(existingReading, glucoseLevel)
        } else {
            // Insert new reading
            val reading = GlucoseReading(
                date = dbDate,
                timeSlot = timeSlot,
                glucoseLevel = glucoseLevel,
                timestamp = System.currentTimeMillis()
            )

            val newId = dao.insertReading(reading.toEntity())
            val savedReading = reading.copy(id = newId)
            readings.add(savedReading)

            // Update UI without toast
            withContext(Dispatchers.Main) {
                if (isMonthlyViewVisible) {
                    updateMonthlyDisplay()
                }
                updateDisplay()
            }
        }
    }

    private suspend fun updateGlucoseReadingSilent(
        oldReading: GlucoseReading,
        newGlucoseLevel: Float,
    ) {
        val updatedReading = oldReading.copy(
            glucoseLevel = newGlucoseLevel,
        )

        dao.updateReading(updatedReading.toEntity())

        // Update local list
        val index = readings.indexOfFirst { it.id == oldReading.id }
        if (index != -1) {
            readings[index] = updatedReading
        }

        withContext(Dispatchers.Main) {
            if (isMonthlyViewVisible) {
                updateMonthlyDisplay()
            }
            updateDisplay()
            // No toast here - handled by caller
        }
    }


    private fun showEditDialog(reading: GlucoseReading, dateString: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Edit ${reading.timeSlot} Reading")
        builder.setMessage("Date: $dateString\nCurrent: ${reading.glucoseLevel} mg/dL")

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

        val glucoseLabel = TextView(this)
        glucoseLabel.text = "Glucose Level (mg/dL):"
        glucoseLabel.textSize = 16f
        layout.addView(glucoseLabel)

        val glucoseInput = EditText(this)
        glucoseInput.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        glucoseInput.setText(if (reading.glucoseLevel > 0) reading.glucoseLevel.toInt().toString() else "")
        layout.addView(glucoseInput)

        builder.setView(layout)

        builder.setPositiveButton("Update") { _, _ ->
            val glucoseText = glucoseInput.text.toString()

            if (glucoseText.isNotEmpty()) {
                try {
                    val glucoseLevel = glucoseText.toFloat()
                    if (glucoseLevel >= 20 && glucoseLevel <= 600) {
                        // ✅ FIXED: Only pass 2 arguments (removed notes)
                        updateGlucoseReading(reading, glucoseLevel)
                    } else {
                        Toast.makeText(this, "Invalid glucose level (20-600 mg/dL)", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: NumberFormatException) {
                    Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show()
                }
            }
        }

        builder.setNeutralButton("Delete") { _, _ ->
            showDeleteConfirmation(reading)
        }

        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun createEditableGlucoseCell(value: String, timeSlot: String, date: String, day: Int): EditText {
        val editText = EditText(this)
        editText.setText(if (value == "-") "" else value)
        editText.gravity = Gravity.CENTER
        editText.setPadding(8, 12, 8, 12)
        editText.textSize = 13f
        editText.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        editText.hint = "-"
        editText.isFocusable = false
        editText.isFocusableInTouchMode = false
        editText.isCursorVisible = false

        // Set initial color based on value and time slot
        if (value != "-" && value.isNotEmpty()) {
            try {
                val glucoseValue = value.toFloat()
                setGlucoseTextColor(editText, glucoseValue, timeSlot)
            } catch (e: NumberFormatException) {
                editText.setTextColor(Color.BLACK)
            }
        } else {
            editText.setTextColor(Color.BLACK)
        }

        // Add border
        val border = GradientDrawable()
        border.setStroke(1, Color.GRAY)
        border.setColor(Color.WHITE)
        editText.background = border

        // Make cell clickable - opens edit dialog
        editText.setOnClickListener {
            val existingReading = readings.find { it.date == date && it.timeSlot == timeSlot }

            if (existingReading != null) {
                // Edit existing reading
                showEditDialog(existingReading, dbDateToDisplayDate(date))
            } else {
                // Create new reading
                val newReading = GlucoseReading(
                    id = 0,
                    date = date,
                    timeSlot = timeSlot,
                    glucoseLevel = 0f,
                    timestamp = System.currentTimeMillis()
                )
                showEditDialog(newReading, dbDateToDisplayDate(date))
            }
        }

        return editText
    }



    private fun showDeleteConfirmation(reading: GlucoseReading) {
        AlertDialog.Builder(this)
            .setTitle("Delete Reading")
            .setMessage("Delete ${reading.timeSlot} reading of ${reading.glucoseLevel} mg/dL from ${reading.date}?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    dao.deleteReading(reading.toEntity())
                    readings.remove(reading)
                    runOnUiThread {
                        if (isMonthlyViewVisible) {
                            updateMonthlyDisplay()
                        }
                        updateDisplay()
                        Toast.makeText(this@MainActivity, "Reading deleted from database!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateGlucoseReading(reading: GlucoseReading, newGlucoseLevel: Float) {
        lifecycleScope.launch {
            try {
                if (reading.id == 0L) {
                    // New reading - insert into database
                    val newReading = reading.copy(glucoseLevel = newGlucoseLevel)
                    val newId = dao.insertReading(newReading.toEntity())
                    val savedReading = newReading.copy(id = newId)
                    readings.add(savedReading)
                } else {
                    // Existing reading - update
                    val updatedReading = reading.copy(glucoseLevel = newGlucoseLevel)
                    dao.updateReading(updatedReading.toEntity())

                    // Update local list
                    val index = readings.indexOfFirst { it.id == reading.id }
                    if (index != -1) {
                        readings[index] = updatedReading
                    }
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Reading saved successfully!", Toast.LENGTH_SHORT).show()
                    updateMonthlyDisplay() // ✅ This now updates RecyclerView
                    updateDisplay()
                    populateHorizontalDates()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Error saving reading: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getGlucoseColor(glucoseLevel: Float, timeSlot: String): Int {
        return when (timeSlot) {
            "Fasting" -> {
                when {
                    glucoseLevel < 80 -> Color.RED                    // Dangerously low
                    glucoseLevel in 80f..120f -> Color.parseColor("#00AA00")  // Normal (green)
                    glucoseLevel in 121f..140f -> Color.parseColor("#FF8800") // Warning (orange)
                    glucoseLevel > 140 -> Color.RED                   // High (red)
                    else -> Color.parseColor("#CC6600")
                }
            }
            else -> { // Morning, Afternoon, Evening
                when {
                    glucoseLevel < 100 -> Color.RED                   // Dangerously low
                    glucoseLevel in 100f..160f -> Color.parseColor("#00AA00") // Normal (green)
                    glucoseLevel in 161f..180f -> Color.parseColor("#FF8800") // Warning (orange)
                    glucoseLevel > 180 -> Color.RED                   // High (red)
                    else -> Color.parseColor("#CC6600")
                }
            }
        }
    }


    private fun updateSelectedDate(timeInMillis: Long) {
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        selectedDate = dateFormat.format(Date(timeInMillis))
        updateDisplay()
    }

    private fun showGlucoseDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add Reading for $selectedDate")

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

        val timeSlotLabel = TextView(this)
        timeSlotLabel.text = "Time Slot:"
        timeSlotLabel.textSize = 16f
        layout.addView(timeSlotLabel)

        val timeSlotSpinner = Spinner(this)
        val timeSlots = arrayOf("Fasting", "Morning", "Afternoon", "Evening")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, timeSlots)
        timeSlotSpinner.adapter = adapter
        layout.addView(timeSlotSpinner)

        val spacer1 = TextView(this)
        spacer1.height = 30
        layout.addView(spacer1)

        val glucoseLabel = TextView(this)
        glucoseLabel.text = "Glucose Level (mg/dL):"
        glucoseLabel.textSize = 16f
        layout.addView(glucoseLabel)

        val glucoseInput = EditText(this)
        glucoseInput.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        glucoseInput.hint = "Enter glucose level"
        layout.addView(glucoseInput)

        val spacer2 = TextView(this)
        spacer2.height = 30
        layout.addView(spacer2)



        builder.setView(layout)

        builder.setPositiveButton("Save") { _, _ ->
            val glucoseText = glucoseInput.text.toString()
            val timeSlot = timeSlotSpinner.selectedItem.toString()



            if (glucoseText.isNotEmpty()) {
                try {
                    val glucoseLevel = glucoseText.toFloat()
                    saveGlucoseReading(selectedDate, timeSlot, glucoseLevel)
                } catch (e: NumberFormatException) {
                    Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please enter glucose level", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun saveGlucoseReading(date: String, timeSlot: String, glucoseLevel: Float) {
        if (glucoseLevel < 20 || glucoseLevel > 600) {
            Toast.makeText(this, "Invalid glucose level (20-600 mg/dL)", Toast.LENGTH_LONG).show()
            return
        }

        val dbDate = displayDateToDbDate(date)
        val existingReading = readings.find { it.date == dbDate && it.timeSlot == timeSlot }

        lifecycleScope.launch {
            if (existingReading != null) {
                // Update existing reading with new values
                updateGlucoseReading(existingReading, glucoseLevel)
            } else {
                // Insert new reading
                val reading = GlucoseReading(
                    date = dbDate,
                    timeSlot = timeSlot,
                    glucoseLevel = glucoseLevel,
                    timestamp = System.currentTimeMillis()
                )

                val newId = dao.insertReading(reading.toEntity())
                val savedReading = reading.copy(id = newId)
                readings.add(savedReading)

                runOnUiThread {
                    if (isMonthlyViewVisible) {
                        updateMonthlyDisplay()
                    }
                    updateDisplay()
                    Toast.makeText(this@MainActivity, "Reading saved to database!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateDisplay() {
        lifecycleScope.launch {
            val totalReadings = dao.getTotalReadingsCount()
            val currentDateLong = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault()).format(Date())

            runOnUiThread {
                statusText.text = "Date: $currentDateLong | Total Readings: $totalReadings"
                statusText.setTextColor(Color.BLACK)
            }
        }
    }

    // Helper function to convert display date (DD-MM-YYYY) to database format (YYYY-MM-DD)
    private fun displayDateToDbDate(displayDate: String): String {
        return try {
            val displayFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            val dbFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = displayFormat.parse(displayDate)
            dbFormat.format(date!!)
        } catch (e: Exception) {
            displayDate // Return as-is if parsing fails
        }
    }

    // Helper function to convert database date (YYYY-MM-DD) to display format (DD-MM-YYYY)
    private fun dbDateToDisplayDate(dbDate: String): String {
        return try {
            val dbFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val displayFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            val date = dbFormat.parse(dbDate)
            displayFormat.format(date!!)
        } catch (e: Exception) {
            dbDate // Return as-is if parsing fails
        }
    }

    private fun scrollToCurrentDate() {
        val today = Calendar.getInstance()
        val currentDay = today.get(Calendar.DAY_OF_MONTH)
        val currentMonth = today.get(Calendar.MONTH)
        val currentYear = today.get(Calendar.YEAR)

        // Only scroll if we're viewing the current month and year
        if (currentCalendarMonth == currentMonth && currentCalendarYear == currentYear) {
            horizontalDateScroll.post {
                // Find the button for current day
                for (i in 0 until horizontalDateContainer.childCount) {
                    val child = horizontalDateContainer.getChildAt(i)
                    if (child is Button && child.text.toString() == currentDay.toString()) {
                        // Calculate scroll position to center the current date button
                        val scrollX = child.left - (horizontalDateScroll.width / 2) + (child.width / 2)
                        horizontalDateScroll.smoothScrollTo(maxOf(0, scrollX), 0)
                        break
                    }
                }
            }
        }
    }

}

// Enhanced data class for glucose readings
data class GlucoseReading(
    val id: Long = 0,
    val date: String,
    val timeSlot: String,
    val glucoseLevel: Float,
    val timestamp: Long
) {
    // Convert to database entity
    fun toEntity(): GlucoseReadingEntity {
        return GlucoseReadingEntity(
            id = this.id,
            date = this.date,
            timeSlot = this.timeSlot,
            glucoseLevel = this.glucoseLevel,
            timestamp = this.timestamp
        )
    }
}