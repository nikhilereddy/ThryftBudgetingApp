/*
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

        //ui components
        val transactionLayout = view.findViewById<LinearLayout>(R.id.transactionListView)
        val header = view.findViewById<TextView>(R.id.categoryHeader)
        val totalBalanceText = view.findViewById<TextView>(R.id.totalBalanceText)

        //back button
        view.findViewById<ImageView>(R.id.backButton)?.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        //search and analytics navigation
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

        //get user id
        val prefs = requireContext().getSharedPreferences("thryft_session", 0)
        val userId = prefs.getInt("user_id", -1)
        if (userId == -1) {
            Toast.makeText(requireContext(), "Invalid user", Toast.LENGTH_SHORT).show()
            return
        }

        db = AppDatabase.getDatabase(requireContext())

        //fetch transactions
        lifecycleScope.launch(Dispatchers.IO) {
            val transactions = db.transactionDao().getAllTransactions(userId)

            //group by date
            val groupedTransactions = transactions.groupBy {
                dateFormat.format(it.date)
            }

            //calculate balance
            val income = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
            val expense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
            val totalBalance = income - expense

            //combine date headers and items
            val formattedTransactions = mutableListOf<Any>()
            groupedTransactions.forEach { (date, transactionList) ->
                formattedTransactions.add(date)
                formattedTransactions.addAll(transactionList)
            }

            launch(Dispatchers.Main) {
                //update ui
                header.text = "Transactions"
                totalBalanceText.text = "R ${(totalBalance)}"
                transactionLayout.removeAllViews()

                //loop over items
                formattedTransactions.forEach { item ->
                    if (item is String) {
                        //date header
                        val dateHeaderView = layoutInflater.inflate(R.layout.list_item_date_header, transactionLayout, false)
                        val dateHeaderTextView = dateHeaderView.findViewById<TextView>(R.id.dateHeader)
                        dateHeaderTextView.text = item
                        transactionLayout.addView(dateHeaderView)
                    } else if (item is Transaction) {
                        //transaction view
                        val transactionView = layoutInflater.inflate(R.layout.list_item_transaction, transactionLayout, false)
                        val transactionName = transactionView.findViewById<TextView>(R.id.transactionName)
                        val transactionDate = transactionView.findViewById<TextView>(R.id.transactionDate)
                        val transactionAmount = transactionView.findViewById<TextView>(R.id.transactionAmount)

                        transactionName.text = item.description
                        transactionDate.text = dateFormat.format(item.date)
                        transactionAmount.text = "R ${item.amount}"

                        //color based on type
                        if (item.type == "EXPENSE") {
                            transactionAmount.setTextColor(resources.getColor(R.color.red))
                        } else {
                            transactionAmount.setTextColor(resources.getColor(R.color.green))
                        }

                        //view details dialog
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

                            //handle image preview
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

                            //edit not implemented
                            editBtn.setOnClickListener {
                                Toast.makeText(requireContext(), "Edit not implemented yet, Feature coming soon!", Toast.LENGTH_SHORT).show()
                            }

                            //delete transaction
                            deleteBtn.setOnClickListener {
                                lifecycleScope.launch(Dispatchers.IO) {
                                    db.transactionDao().deleteTransaction(item)
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(requireContext(), "Transaction deleted", Toast.LENGTH_SHORT).show()
                                        dialog.dismiss()
                                        reloadTransactions() //refresh list
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

    //reload transactions after delete
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
                //update ui again
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

                        transactionLayout.addView(transactionView)
                    }
                }
            }
        }
    }
}
*/

package com.example.thryftapp

import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class TransactionFragment : Fragment(R.layout.fragment_view_transactions) {

    private lateinit var db: AppDatabase
    private val firestore = FirebaseFirestore.getInstance()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private lateinit var firebaseAuth: FirebaseAuth
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val transactionLayout = view.findViewById<LinearLayout>(R.id.transactionListView)
        val header = view.findViewById<TextView>(R.id.categoryHeader)
        val totalBalanceText = view.findViewById<TextView>(R.id.totalBalanceText)

        view.findViewById<ImageView>(R.id.backButton)?.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        view.findViewById<Button>(R.id.searchButton).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, SearchFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<Button>(R.id.analyticsButton).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, AnalyticsFragment())
                .addToBackStack(null)
                .commit()
        }

        val prefs = requireContext().getSharedPreferences("thryft_session", 0)
        firebaseAuth = FirebaseAuth.getInstance()

        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            Toast.makeText(context, "Invalid user or category", Toast.LENGTH_SHORT).show()
            return
        }
        val userId = currentUser.uid
        db = AppDatabase.getDatabase(requireContext())

        loadTransactions(userId, transactionLayout, header, totalBalanceText)
    }

    private fun loadTransactions(userId: String, layout: LinearLayout, header: TextView, balanceText: TextView) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val snapshot = firestore.collection("transactions")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()



                val transactions = snapshot.documents.mapNotNull { doc ->
                    try {
                        Transaction(
                            id = 0,
                            userId = userId,
                            categoryId = doc.getString("categoryId"), // 🔥 From Firestore directly as String
                            amount = doc.getDouble("amount") ?: 0.0,
                            type = doc.getString("type") ?: "",
                            description = doc.getString("description"),
                            photoUri = doc.getString("photoUri"),
                            date = doc.getTimestamp("date")?.toDate() ?: Date(),
                            createdAt = doc.getTimestamp("createdAt")?.toDate() ?: Date()
                        )
                    } catch (e: Exception) {
                        null
                    }
                }

                //transactions.forEach { db.transactionDao().insertTransaction(it) }

                displayTransactions(transactions, layout, header, balanceText)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "User ID: $userId", Toast.LENGTH_SHORT).show()
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("FirestoreError", "Failed to fetch transactions: ${e.message}", e)
                    Toast.makeText(requireContext(), "Failed to load from Firestore", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun displayTransactions(transactions: List<Transaction>, layout: LinearLayout, header: TextView, balanceText: TextView) {
        val grouped = transactions.groupBy { dateFormat.format(it.date) }
        val formatted = mutableListOf<Any>()
        grouped.forEach { (date, list) ->
            formatted.add(date)
            formatted.addAll(list)
        }

        val income = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
        val expense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val balance = income - expense

        lifecycleScope.launch(Dispatchers.Main) {
            header.text = "Transactions"
            balanceText.text = "R $balance"
            layout.removeAllViews()

            formatted.forEach { item ->
                if (item is String) {
                    val headerView = layoutInflater.inflate(R.layout.list_item_date_header, layout, false)
                    headerView.findViewById<TextView>(R.id.dateHeader).text = item
                    layout.addView(headerView)
                } else if (item is Transaction) {
                    val txView = layoutInflater.inflate(R.layout.list_item_transaction, layout, false)
                    txView.findViewById<TextView>(R.id.transactionName).text = item.description
                    txView.findViewById<TextView>(R.id.transactionDate).text = dateFormat.format(item.date)
                    val amountView = txView.findViewById<TextView>(R.id.transactionAmount)
                    amountView.text = "R ${item.amount}"
                    amountView.setTextColor(resources.getColor(if (item.type == "EXPENSE") R.color.red else R.color.green))

                    txView.setOnClickListener { showTransactionDialog(item) }
                    layout.addView(txView)
                }
            }
        }
    }

    private fun showTransactionDialog(tx: Transaction) {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_transaction_details)

        dialog.findViewById<TextView>(R.id.dialogDescription).text = tx.description
        dialog.findViewById<TextView>(R.id.dialogDate).text = "Date: ${dateFormat.format(tx.date)}"
        dialog.findViewById<TextView>(R.id.dialogAmount).text = "Amount: R${tx.amount}"

        val image = dialog.findViewById<ImageView>(R.id.dialogTransactionImage)
        val status = dialog.findViewById<TextView>(R.id.dialogImageStatus)

        if (!tx.photoUri.isNullOrEmpty()) {
            image.setImageURI(Uri.parse(tx.photoUri))
            image.visibility = View.VISIBLE
            status.visibility = View.GONE
        } else {
            image.visibility = View.GONE
            status.visibility = View.VISIBLE
        }

        dialog.findViewById<Button>(R.id.dialogEditBtn).setOnClickListener {
            Toast.makeText(requireContext(), "Edit Coming Soon", Toast.LENGTH_SHORT).show()
        }

        dialog.findViewById<Button>(R.id.dialogDeleteBtn).setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                db.transactionDao().deleteTransaction(tx)
                firestore.collection("transactions")
                    .whereEqualTo("createdAt", tx.createdAt)
                    .get()
                    .addOnSuccessListener { query ->
                        for (doc in query) doc.reference.delete()
                    }
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Delete Coming Soon", Toast.LENGTH_SHORT).show()
                    reload()
                }
            }
        }

        dialog.show()
    }

    private fun reload() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val layout = view?.findViewById<LinearLayout>(R.id.transactionListView) ?: return
        val header = view?.findViewById<TextView>(R.id.categoryHeader) ?: return
        val balance = view?.findViewById<TextView>(R.id.totalBalanceText) ?: return
        loadTransactions(userId, layout, header, balance)
    }
}
