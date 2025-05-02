package com.example.thryftapp

import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class TransactionFragment : Fragment(R.layout.fragment_view_transactions) {

    private lateinit var db: AppDatabase
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val transactionLayout = view.findViewById<LinearLayout>(R.id.transactionListView)
        val header = view.findViewById<TextView>(R.id.categoryHeader)
        val totalBalanceText = view.findViewById<TextView>(R.id.totalBalanceText)

        view.findViewById<ImageView>(R.id.backButton)?.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
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

        val prefs = requireContext().getSharedPreferences("thryft_session", 0)
        val userId = prefs.getInt("user_id", -1)
        if (userId == -1) {
            Toast.makeText(requireContext(), "Invalid user", Toast.LENGTH_SHORT).show()
            return
        }

        db = AppDatabase.getDatabase(requireContext())

        lifecycleScope.launch(Dispatchers.IO) {
            val transactions = db.transactionDao().getAllTransactions(userId)

            val groupedTransactions = transactions.groupBy {
                dateFormat.format(it.date)
            }

            val income = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
            val expense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
            val totalBalance = income - expense

            val formattedTransactions = mutableListOf<Any>()
            groupedTransactions.forEach { (date, transactionList) ->
                formattedTransactions.add(date)
                formattedTransactions.addAll(transactionList)
            }

            launch(Dispatchers.Main) {
                header.text = "Transactions"
                totalBalanceText.text = "R ${(totalBalance)}"
                transactionLayout.removeAllViews()

                formattedTransactions.forEach { item ->
                    if (item is String) {
                        val dateHeaderView = layoutInflater.inflate(R.layout.list_item_date_header, transactionLayout, false)
                        val dateHeaderTextView = dateHeaderView.findViewById<TextView>(R.id.dateHeader)
                        dateHeaderTextView.text = item
                        transactionLayout.addView(dateHeaderView)
                    } else if (item is Transaction) {
                        val transactionView = layoutInflater.inflate(R.layout.list_item_transaction, transactionLayout, false)
                        val transactionName = transactionView.findViewById<TextView>(R.id.transactionName)
                        val transactionDate = transactionView.findViewById<TextView>(R.id.transactionDate)
                        val transactionAmount = transactionView.findViewById<TextView>(R.id.transactionAmount)

                        transactionName.text = item.description
                        transactionDate.text = dateFormat.format(item.date)
                        transactionAmount.text = "R ${item.amount}"

                        if (item.type == "EXPENSE") {
                            transactionAmount.setTextColor(resources.getColor(R.color.red))
                        } else {
                            transactionAmount.setTextColor(resources.getColor(R.color.green))
                        }

                        transactionView.setOnClickListener {
                            val dialog = Dialog(requireContext())
                            dialog.setContentView(R.layout.dialog_transaction_details)

                            val descText = dialog.findViewById<TextView>(R.id.dialogDescription)
                            val dateText = dialog.findViewById<TextView>(R.id.dialogDate)
                            val amtText = dialog.findViewById<TextView>(R.id.dialogAmount)
                            val image = dialog.findViewById<ImageView>(R.id.dialogTransactionImage)
                            val imageStatus = dialog.findViewById<TextView>(R.id.dialogImageStatus)

                            descText.text = item.description
                            dateText.text = "Date: ${dateFormat.format(item.date)}"
                            amtText.text = "Amount: R${item.amount}"

                            if (!item.photoUri.isNullOrEmpty()) {
                                image.setImageURI(Uri.parse(item.photoUri))
                                image.visibility = View.VISIBLE
                                imageStatus.visibility = View.GONE
                            } else {
                                image.visibility = View.GONE
                                imageStatus.visibility = View.VISIBLE
                            }

                            val editBtn = dialog.findViewById<Button>(R.id.dialogEditBtn)
                            val deleteBtn = dialog.findViewById<Button>(R.id.dialogDeleteBtn)

                            editBtn.setOnClickListener {
                                Toast.makeText(requireContext(), "Edit not implemented yet, Feature coming soon!", Toast.LENGTH_SHORT).show()
                            }

                            deleteBtn.setOnClickListener {
                                lifecycleScope.launch(Dispatchers.IO) {
                                    db.transactionDao().deleteTransaction(item)
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(requireContext(), "Transaction deleted", Toast.LENGTH_SHORT).show()
                                        dialog.dismiss()
                                        reloadTransactions() // You must define this method to refresh the list
                                    }
                                }
                            }
                            dialog.show()
                        }

                        transactionLayout.addView(transactionView)
                    }
                }
            }
        }
    }
    private fun reloadTransactions() {
        val transactionLayout = view?.findViewById<LinearLayout>(R.id.transactionListView) ?: return
        val header = view?.findViewById<TextView>(R.id.categoryHeader)
        val totalBalanceText = view?.findViewById<TextView>(R.id.totalBalanceText)

        val prefs = requireContext().getSharedPreferences("thryft_session", 0)
        val userId = prefs.getInt("user_id", -1)
        if (userId == -1) return

        lifecycleScope.launch(Dispatchers.IO) {
            val transactions = db.transactionDao().getAllTransactions(userId)
            val groupedTransactions = transactions.groupBy {
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it.date)
            }

            val income = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
            val expense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
            val totalBalance = income - expense

            val formattedTransactions = mutableListOf<Any>()
            groupedTransactions.forEach { (date, list) ->
                formattedTransactions.add(date)
                formattedTransactions.addAll(list)
            }

            launch(Dispatchers.Main) {
                header?.text = "Transactions"
                totalBalanceText?.text = "R ${totalBalance}"
                transactionLayout.removeAllViews()

                formattedTransactions.forEach { item ->
                    if (item is String) {
                        val dateHeader = layoutInflater.inflate(R.layout.list_item_date_header, transactionLayout, false)
                        dateHeader.findViewById<TextView>(R.id.dateHeader).text = item
                        transactionLayout.addView(dateHeader)
                    } else if (item is Transaction) {
                        val transactionView = layoutInflater.inflate(R.layout.list_item_transaction, transactionLayout, false)
                        val transactionName = transactionView.findViewById<TextView>(R.id.transactionName)
                        val transactionDate = transactionView.findViewById<TextView>(R.id.transactionDate)
                        val transactionAmount = transactionView.findViewById<TextView>(R.id.transactionAmount)

                        transactionName.text = item.description
                        transactionDate.text = dateFormat.format(item.date)
                        transactionAmount.text = "R ${item.amount}"
                        transactionAmount.setTextColor(
                            resources.getColor(if (item.type == "EXPENSE") R.color.red else R.color.green)
                        )

                        // Reuse dialog logic if needed
                        transactionLayout.addView(transactionView)
                    }
                }
            }
        }
    }


}
