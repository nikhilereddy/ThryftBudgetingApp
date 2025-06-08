/*
package com.example.thryftapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.mikephil.charting.charts.PieChart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ExpensePieChartActivity : AppCompatActivity() {
    private lateinit var db: AppDatabase //database reference
    private lateinit var graphHelper: GraphHelper //chart helper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pie_chart) //set layout

        db = AppDatabase.getDatabase(this) //init db
        graphHelper = GraphHelper(this) //init helper

        val prefs = getSharedPreferences("thryft_session", MODE_PRIVATE) //get prefs
        val userId = prefs.getInt("user_id", -1) //get user id

        if (userId == -1) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val pieChart = findViewById<PieChart>(R.id.pieChart) //find pie chart view

        CoroutineScope(Dispatchers.IO).launch {
            val transactions = db.transactionDao().getAllTransactions(userId) //get transactions
            pieChart.setUsePercentValues(true) //show percentages
            val categories = db.categoryDao().getAllCategories(userId) //get categories
            val incomeTransactions = transactions.filter { it.type == "EXPENSE" } //filter expenses
            runOnUiThread {
                graphHelper.setupPieChartByCategory(pieChart, incomeTransactions, categories) //render chart
            }
        }
    }
}
*/
/*
package com.example.thryftapp

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.PieChart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ExpensePieChartActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase // database instance
    private lateinit var graphHelper: GraphHelper // graphing utility

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pie_chart) // set the layout

        db = AppDatabase.getDatabase(this) // initialize DB
        graphHelper = GraphHelper(this) // initialize chart helper

        val prefs = getSharedPreferences("thryft_session", MODE_PRIVATE)
        val userId = prefs.getString("user_id", null)

        if (userId.isNullOrEmpty()) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val pieChart = findViewById<PieChart>(R.id.pieChart)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val transactions = db.transactionDao().getAllTransactions(userId)
                val categories = db.categoryDao().getAllCategories(userId)
                val expenseTransactions = transactions.filter { it.type == "EXPENSE" }

                runOnUiThread {
                    pieChart.setUsePercentValues(true)
                    graphHelper.setupPieChartByCategory(pieChart, expenseTransactions, categories)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@ExpensePieChartActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
*/
package com.example.thryftapp

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.PieChart
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.util.*

class ExpensePieChartActivity : AppCompatActivity() {

    private lateinit var graphHelper: GraphHelper // chart helper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pie_chart)

        graphHelper = GraphHelper(this)

        val prefs = getSharedPreferences("thryft_session", MODE_PRIVATE)
        val userId = prefs.getString("user_id", null)

        if (userId.isNullOrEmpty()) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val pieChart = findViewById<PieChart>(R.id.pieChart)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val firestore = FirebaseFirestore.getInstance()

                // Fetch transactions
                val txnSnapshot = firestore.collection("transactions")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()

                val transactions = txnSnapshot.documents.mapNotNull { doc ->
                    try {
                        Transaction(
                            id = 0,
                            userId = userId,
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
                }.filter { it.type == "EXPENSE" }

                // Fetch categories
                val catSnapshot = firestore.collection("users")
                    .document(userId)
                    .collection("categories")
                    .get()
                    .await()

                val categories = catSnapshot.documents.mapNotNull { doc ->
                    try {
                        Category(
                            id = doc.getString("id") ?: doc.id,
                            userId = userId,
                            name = doc.getString("name") ?: "Unnamed",
                            type = doc.getString("type") ?: "EXPENSE",
                            minBudget = doc.getDouble("minBudget") ?: 0.0,
                            maxBudget = doc.getDouble("maxBudget") ?: 0.0,
                            iconId = doc.getString("iconId") ?: "gmd_home"
                        )
                    } catch (e: Exception) {
                        null
                    }
                }

                withContext(Dispatchers.Main) {
                    pieChart.setUsePercentValues(true)
                    graphHelper.setupPieChartByCategory(pieChart, transactions, categories)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ExpensePieChartActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
