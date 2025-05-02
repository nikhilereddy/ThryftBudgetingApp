package com.example.thryftapp

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class TransactionFragment : Fragment(R.layout.fragment_view_transactions) {

    private lateinit var db: AppDatabase
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val transactionLayout = view.findViewById<LinearLayout>(R.id.transactionListView) // This is now a LinearLayout
        val header = view.findViewById<TextView>(R.id.categoryHeader)
        val totalBalanceText = view.findViewById<TextView>(R.id.totalBalanceText)

        // Back Button click listener
        view.findViewById<ImageView>(R.id.backButton)?.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()  // This will pop the current fragment from the back stack
        }
        val searchButton = view.findViewById<Button>(R.id.searchButton)

        val analyticsButton = view.findViewById<Button>(R.id.analyticsButton)
        searchButton.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, SearchFragment())
                .addToBackStack(null)
                .commit()
        }
        analyticsButton.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, AnalyticsFragment())
                .addToBackStack(null)
                .commit()
        }

        // Assume userId is stored in shared preferences
        val prefs = requireContext().getSharedPreferences("thryft_session", 0)
        val userId = prefs.getInt("user_id", -1)

        if (userId == -1) {
            Toast.makeText(requireContext(), "Invalid user", Toast.LENGTH_SHORT).show()
            return
        }

        db = AppDatabase.getDatabase(requireContext())

        lifecycleScope.launch(Dispatchers.IO) {
            // Fetch transactions for the user
            val transactions = db.transactionDao().getAllTransactions(userId)

            // Group transactions by date
            val groupedTransactions = transactions.groupBy {
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it.date)
            }

            // Calculate income and expense totals
            val income = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
            val expense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
            val totalBalance = income - expense

            // Prepare the list to display
            val formattedTransactions = mutableListOf<Any>()  // List to hold both date headers and transactions
            groupedTransactions.forEach { (date, transactionList) ->
                formattedTransactions.add(date)  // Add date header
                formattedTransactions.addAll(transactionList)  // Add transactions for that date
            }

            launch(Dispatchers.Main) {
                // Set header text
                header.text = "Transactions"

                // Set the total balance formatted text
                totalBalanceText.text = "R ${(totalBalance)}"

                // Dynamically add transactions to the layout
                transactionLayout.removeAllViews() // Clear any previous items
                formattedTransactions.forEach { item ->
                    if (item is String) { // Date header
                        val dateHeaderView = layoutInflater.inflate(R.layout.list_item_date_header, transactionLayout, false)
                        val dateHeaderTextView = dateHeaderView.findViewById<TextView>(R.id.dateHeader)
                        dateHeaderTextView.text = item
                        transactionLayout.addView(dateHeaderView)
                    } else if (item is Transaction) { // Transaction item
                        val transactionView = layoutInflater.inflate(R.layout.list_item_transaction, transactionLayout, false)
                        val transactionName = transactionView.findViewById<TextView>(R.id.transactionName)
                        val transactionDate = transactionView.findViewById<TextView>(R.id.transactionDate)
                        val transactionAmount = transactionView.findViewById<TextView>(R.id.transactionAmount)

                        transactionName.text = item.description
                        transactionDate.text = dateFormat.format(item.date)
                        transactionAmount.text = "R ${item.amount}"

                        // Set color based on transaction type
                        if (item.type == "EXPENSE") {
                            transactionAmount.setTextColor(resources.getColor(R.color.red))  // Red for expenses
                        } else {
                            transactionAmount.setTextColor(resources.getColor(R.color.green))  // Green for income
                        }

                        transactionLayout.addView(transactionView)
                    }
                }
            }
        }
    }
}
//correct bersion