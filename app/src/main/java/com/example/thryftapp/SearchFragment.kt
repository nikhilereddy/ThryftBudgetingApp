package com.example.thryftapp
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class SearchFragment : Fragment() {

    private lateinit var fromDateEditText: EditText
    private lateinit var toDateEditText: EditText
    private lateinit var searchOptionGroup: RadioGroup
    private lateinit var searchButton: Button
    private lateinit var resultsContainer: LinearLayout
    private lateinit var transactionTypeGroup: RadioGroup

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search, container, false)

        view.findViewById<ImageView>(R.id.backButton)?.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        fromDateEditText = view.findViewById(R.id.fromDateEditText)
        toDateEditText = view.findViewById(R.id.toDateEditText)
        searchOptionGroup = view.findViewById(R.id.searchOptionGroup)
        searchButton = view.findViewById(R.id.searchButton)
        resultsContainer = view.findViewById(R.id.searchResultsContainer)
        transactionTypeGroup = view.findViewById(R.id.transactionTypeGroup)

        setupDatePickers()

        searchButton.setOnClickListener {
            performSearch()
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

        fromDateEditText.setOnClickListener { showDatePicker(fromDateEditText) }
        toDateEditText.setOnClickListener { showDatePicker(toDateEditText) }
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
        }.time

        val selectedOption = searchOptionGroup.checkedRadioButtonId
        resultsContainer.removeAllViews()

        when (selectedOption) {
            R.id.radioTransactions -> fetchTransactions(fromDate, toDateEndOfDay)
            R.id.radioCategories -> fetchCategoryTotals(fromDate, toDateEndOfDay)
            else -> Toast.makeText(requireContext(), "Please choose a search option", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchTransactions(fromDate: Date, toDate: Date) {
        val prefs = requireContext().getSharedPreferences("thryft_session", 0)
        val userId = prefs.getString("user_id", null) ?: return
        val firestore = FirebaseFirestore.getInstance()

        val selectedType = when (transactionTypeGroup.checkedRadioButtonId) {
            R.id.typeIncome -> "Income"
            R.id.typeExpense -> "Expense"
            else -> null
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val snapshot = firestore.collection("transactions")
                    .whereEqualTo("userId", userId)
                    .whereGreaterThanOrEqualTo("date", fromDate)
                    .whereLessThanOrEqualTo("date", toDate)
                    .get().await()

                val transactions = snapshot.documents.mapNotNull { it.toTransaction() }
                    .filter { txn -> selectedType == null || txn.type.equals(selectedType, true) }

                launch(Dispatchers.Main) {
                    if (transactions.isEmpty()) {
                        addNoResultsText("No ${selectedType ?: "transactions"} found in this period.")
                    } else {
                        for (txn in transactions) {
                            val itemView = layoutInflater.inflate(R.layout.item_transaction_result, resultsContainer, false)
                            itemView.findViewById<TextView>(R.id.transactionName).text = txn.description
                            itemView.findViewById<TextView>(R.id.transactionDate).text = dateFormat.format(txn.date)
                            itemView.findViewById<TextView>(R.id.transactionAmount).text = "R${txn.amount}"
                            resultsContainer.addView(itemView)
                        }
                    }
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    addNoResultsText("Error: ${e.localizedMessage}")
                }
            }
        }
    }

    private fun fetchCategoryTotals(fromDate: Date, toDate: Date) {
        val prefs = requireContext().getSharedPreferences("thryft_session", 0)
        val userId = prefs.getString("user_id", null) ?: return
        val firestore = FirebaseFirestore.getInstance()

        val selectedType = when (transactionTypeGroup.checkedRadioButtonId) {
            R.id.typeIncome -> "Income"
            R.id.typeExpense -> "Expense"
            else -> null
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val snapshot = firestore.collection("transactions")
                    .whereEqualTo("userId", userId)
                    .whereGreaterThanOrEqualTo("date", fromDate)
                    .whereLessThanOrEqualTo("date", toDate)
                    .get().await()

                val transactions = snapshot.documents.mapNotNull { it.toTransaction() }
                    .filter { txn -> selectedType == null || txn.type.equals(selectedType, true) }

                val grouped = transactions.groupBy { it.categoryId }

                val totalsWithNames = mutableMapOf<String, Double>()

                val categoryTasks = grouped.mapNotNull { (categoryId, txns) ->
                    if (categoryId == null) return@mapNotNull null

                    firestore.collection("users")
                        .document(userId)
                        .collection("categories")
                        .document(categoryId)
                        .get()
                        .continueWith { task ->
                            val doc = task.result
                            val name = doc?.getString("name") ?: "Unknown"
                            val total = txns.sumOf { it.amount }
                            name to total
                        }
                }

                val results = categoryTasks.mapNotNull { it.await() }
                results.forEach { (name, total) -> totalsWithNames[name] = total }

                launch(Dispatchers.Main) {
                    if (totalsWithNames.isEmpty()) {
                        addNoResultsText("No ${selectedType ?: "Income/Expense"} category totals found.")
                    } else {
                        for ((name, total) in totalsWithNames) {
                            val itemView = layoutInflater.inflate(R.layout.item_category_total, resultsContainer, false)
                            itemView.findViewById<TextView>(R.id.categoryNameText).text = name
                            itemView.findViewById<TextView>(R.id.categoryTotalText).text = "R$total"
                            resultsContainer.addView(itemView)
                        }
                    }
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    addNoResultsText("Error: ${e.localizedMessage}")
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

    private fun DocumentSnapshot.toTransaction(): Transaction? {
        return try {
            Transaction(
                id = 0,
                userId = getString("userId") ?: return null,
                categoryId = getString("categoryId"),
                amount = getDouble("amount") ?: 0.0,
                type = getString("type") ?: "",
                description = getString("description"),
                photoUri = getString("photoUri"),
                date = getDate("date") ?: Date(),
                createdAt = getDate("createdAt") ?: Date()
            )
        } catch (e: Exception) {
            null
        }
    }
}
