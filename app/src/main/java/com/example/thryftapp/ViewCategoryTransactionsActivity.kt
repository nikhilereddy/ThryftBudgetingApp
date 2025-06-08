/*
package com.example.thryftapp

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.SimpleDateFormat

class ViewCategoryTransactionsActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private val formatter = DecimalFormat("#,##0.00") //formatter for amount

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_category_transactions)

        val categoryId = intent.getIntExtra("category_id", -1) //get category id from intent
        val prefs = getSharedPreferences("thryft_session", MODE_PRIVATE)
        val userId = prefs.getInt("user_id", -1) //get user id from session

        val header = findViewById<TextView>(R.id.categoryHeader)
        val listView = findViewById<ListView>(R.id.transactionListView)

        //validate ids
        if (categoryId == -1 || userId == -1) {
            Toast.makeText(this, "Invalid user or category", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        db = AppDatabase.getDatabase(this) //initialize db

        lifecycleScope.launch(Dispatchers.IO) {
            val category = db.categoryDao().getCategoryById(categoryId) //fetch category
            val transactions = db.transactionDao().getTransactionsByCategory(categoryId, userId) //fetch related transactions

            launch(Dispatchers.Main) {
                //set header
                if (category != null) {
                    header.text = "Transactions for ${category.name.toUpperCase()}"
                }

                //check if empty
                if (transactions.isEmpty()) {
                    listView.adapter = ArrayAdapter(
                        this@ViewCategoryTransactionsActivity,
                        android.R.layout.simple_list_item_1,
                        listOf("No transactions available")
                    )
                } else {
                    //map data into readable lines
                    val items = transactions.map {
                        " Description: ${it.description} \n Amt: R${formatter.format(it.amount)} \n Date:${SimpleDateFormat("dd MMM yyyy").format(it.date)} \n"
                    }

                    //bind to listview
                    listView.adapter = ArrayAdapter(
                        this@ViewCategoryTransactionsActivity,
                        android.R.layout.simple_list_item_1,
                        items
                    )
                }
            }
        }
    }
}
*/
package com.example.thryftapp

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

class ViewCategoryTransactionsActivity : AppCompatActivity() {

    private val formatter = DecimalFormat("#,##0.00") // format for currency
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_category_transactions)

        val categoryId = intent.getStringExtra("category_id") // Firestore category IDs are Strings
        val prefs = getSharedPreferences("thryft_session", MODE_PRIVATE)
        val userId = prefs.getString("user_id", null)

        val headerText = findViewById<TextView>(R.id.categoryHeader)
        val listView = findViewById<ListView>(R.id.transactionListView)

        if (categoryId.isNullOrBlank() || userId == null) {
            Toast.makeText(this, "Invalid user or category", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val categorySnapshot = firestore.collection("users")
                    .document(userId)
                    .collection("categories")
                    .document(categoryId)
                    .get().await()

                val categoryName = categorySnapshot.getString("name") ?: "Category"

                val transactionSnapshot = firestore.collection("transactions")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("categoryId", categoryId)
                    .get().await()

                val transactions = transactionSnapshot.documents.mapNotNull { doc ->
                    try {
                        Transaction(
                            id = 0,
                            userId = doc.getString("userId") ?: return@mapNotNull null,
                            categoryId = doc.getString("categoryId"),
                            amount = doc.getDouble("amount") ?: 0.0,
                            type = doc.getString("type") ?: "",
                            description = doc.getString("description"),
                            photoUri = doc.getString("photoUri"),
                            date = doc.getDate("date") ?: Date(),
                            createdAt = doc.getDate("createdAt") ?: Date()
                        )
                    } catch (e: Exception) {
                        null
                    }
                }

                launch(Dispatchers.Main) {
                    headerText.text = "Transactions for ${categoryName.uppercase(Locale.getDefault())}"

                    if (transactions.isEmpty()) {
                        listView.adapter = ArrayAdapter(
                            this@ViewCategoryTransactionsActivity,
                            android.R.layout.simple_list_item_1,
                            listOf("No transactions available")
                        )
                    } else {
                        val items = transactions.map {
                            "Description: ${it.description}\n" +
                                    "Amount: R${formatter.format(it.amount)}\n" +
                                    "Date: ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(it.date)}"
                        }

                        listView.adapter = ArrayAdapter(
                            this@ViewCategoryTransactionsActivity,
                            android.R.layout.simple_list_item_1,
                            items
                        )
                    }
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    Toast.makeText(this@ViewCategoryTransactionsActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
