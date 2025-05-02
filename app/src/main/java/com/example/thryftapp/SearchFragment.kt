package com.example.thryftapp

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SearchFragment : Fragment() {

    private lateinit var fromDateEditText: EditText //from date input
    private lateinit var toDateEditText: EditText //to date input
    private lateinit var searchOptionGroup: RadioGroup //search option radio
    private lateinit var searchButton: Button //search button
    private lateinit var resultsContainer: LinearLayout //search results view
    private lateinit var db: AppDatabase //db instance
    private lateinit var transactionTypeGroup: RadioGroup //transaction type filter

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) //date format

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search, container, false) //inflate layout

        db = AppDatabase.getDatabase(requireContext()) //init db

        fromDateEditText = view.findViewById(R.id.fromDateEditText)
        toDateEditText = view.findViewById(R.id.toDateEditText)
        searchOptionGroup = view.findViewById(R.id.searchOptionGroup)
        searchButton = view.findViewById(R.id.searchButton)
        resultsContainer = view.findViewById(R.id.searchResultsContainer)
        transactionTypeGroup = view.findViewById(R.id.transactionTypeGroup)

        setupDatePickers() //setup date pickers

        searchButton.setOnClickListener {
            performSearch() //trigger search
        }

        return view
    }

    private fun setupDatePickers() {
        val calendar = Calendar.getInstance()

        val showDatePicker = { target: EditText ->
            val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
                calendar.set(year, month, day)
                target.setText(dateFormat.format(calendar.time))
            }

            DatePickerDialog(
                requireContext(), dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        fromDateEditText.setOnClickListener { showDatePicker(fromDateEditText) } //from date
        toDateEditText.setOnClickListener { showDatePicker(toDateEditText) } //to date
    }

    private fun performSearch() {
        val fromDateStr = fromDateEditText.text.toString()
        val toDateStr = toDateEditText.text.toString()

        if (fromDateStr.isBlank() || toDateStr.isBlank()) {
            Toast.makeText(requireContext(), "Please select both dates", Toast.LENGTH_SHORT).show()
            return
        }

        val fromDate = dateFormat.parse(fromDateStr)
        val toDateRaw = dateFormat.parse(toDateStr)

        if (fromDate == null || toDateRaw == null) {
            Toast.makeText(requireContext(), "Invalid date format", Toast.LENGTH_SHORT).show()
            return
        }
        if (fromDate.after(toDateRaw)) {
            Toast.makeText(requireContext(), "Start date cannot be after end date", Toast.LENGTH_SHORT).show()
            return
        }

        val toDateEndOfDay = Calendar.getInstance().apply {
            time = toDateRaw
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.time //adjust end date

        val selectedOption = searchOptionGroup.checkedRadioButtonId
        resultsContainer.removeAllViews() //clear previous results

        when (selectedOption) {
            R.id.radioTransactions -> fetchTransactions(fromDate.time, toDateEndOfDay.time)
            R.id.radioCategories -> fetchCategoryTotals(fromDate.time, toDateEndOfDay.time)
            else -> Toast.makeText(requireContext(), "Please choose a search option", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchTransactions(fromMillis: Long, toMillis: Long) {
        val selectedType = when (transactionTypeGroup.checkedRadioButtonId) {
            R.id.typeIncome -> "Income"
            R.id.typeExpense -> "Expense"
            else -> null //both types
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val allTxns = db.transactionDao().getTransactionsBetweenDates(fromMillis, toMillis)

            val filteredTxns = if (selectedType != null) {
                allTxns.filter { it.type.equals(selectedType, ignoreCase = true) }
            } else {
                allTxns
            }

            launch(Dispatchers.Main) {
                if (filteredTxns.isEmpty()) {
                    addNoResultsText("No ${selectedType ?: "transactions"} found in this period.")
                    return@launch
                }

                for (txn in filteredTxns) {
                    val itemView = layoutInflater.inflate(R.layout.item_transaction_result, resultsContainer, false)

                    val nameText = itemView.findViewById<TextView>(R.id.transactionName)
                    val dateText = itemView.findViewById<TextView>(R.id.transactionDate)
                    val amountText = itemView.findViewById<TextView>(R.id.transactionAmount)

                    nameText.text = txn.description
                    dateText.text = dateFormat.format(txn.date)
                    amountText.text = "R${txn.amount}"

                    resultsContainer.addView(itemView)
                }
            }
        }
    }

    private fun fetchCategoryTotals(fromMillis: Long, toMillis: Long) {
        val selectedType = when (transactionTypeGroup.checkedRadioButtonId) {
            R.id.typeIncome -> "Income"
            R.id.typeExpense -> "Expense"
            else -> null //both types
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val allTransactions = db.transactionDao().getTransactionsBetweenDates(fromMillis, toMillis)

            val filteredTransactions = if (selectedType != null) {
                allTransactions.filter { it.type.equals(selectedType, ignoreCase = true) }
            } else {
                allTransactions
            }

            val grouped = filteredTransactions.groupBy { it.categoryId }
            val categoryDao = db.categoryDao()
            val totalsWithNames = mutableMapOf<String, Double>()

            for ((categoryId, txns) in grouped) {
                if (categoryId == null) continue
                val categoryName = categoryDao.getCategoryNameById(categoryId) ?: "Unknown"
                val total = txns.sumOf { it.amount }
                totalsWithNames[categoryName] = total
            }

            launch(Dispatchers.Main) {
                if (totalsWithNames.isEmpty()) {
                    addNoResultsText("No ${selectedType ?: "Income/Expense"} category totals found in this period.")
                    return@launch
                }

                for ((name, total) in totalsWithNames) {
                    val itemView = layoutInflater.inflate(R.layout.item_category_total, resultsContainer, false)

                    val categoryNameText = itemView.findViewById<TextView>(R.id.categoryNameText)
                    val categoryTotalText = itemView.findViewById<TextView>(R.id.categoryTotalText)

                    categoryNameText.text = name
                    categoryTotalText.text = "R$total"

                    resultsContainer.addView(itemView)
                }
            }
        }
    }

    private fun addNoResultsText(message: String) {
        val noResultsText = TextView(requireContext()).apply {
            text = message
            setPadding(0, 20, 0, 8)
            textSize = 16f
            setTextColor(resources.getColor(android.R.color.darker_gray, null))
            textAlignment = View.TEXT_ALIGNMENT_CENTER
        }
        resultsContainer.addView(noResultsText)
    }
}
