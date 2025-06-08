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

class IncomePieChartActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase //database reference
    private lateinit var graphHelper: GraphHelper //chart helper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pie_chart) //set layout

        db = AppDatabase.getDatabase(this) //init database
        graphHelper = GraphHelper(this) //init graph helper

        val prefs = getSharedPreferences("thryft_session", MODE_PRIVATE) //get session prefs
        val userId = prefs.getInt("user_id", -1) //get user id

        if (userId == -1) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show() //show error
            return
        }

        val pieChart = findViewById<PieChart>(R.id.pieChart) //find pie chart view

        CoroutineScope(Dispatchers.IO).launch {
            val transactions = db.transactionDao().getAllTransactions(userId) //get transactions
            pieChart.setUsePercentValues(true) //show percentages
            val categories = db.categoryDao().getAllCategories(userId) //get categories
            val incomeTransactions = transactions.filter { it.type == "INCOME" } //filter income
            runOnUiThread {
                graphHelper.setupPieChartByCategory(pieChart, incomeTransactions, categories) //draw chart
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

class IncomePieChartActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase //database reference
    private lateinit var graphHelper: GraphHelper //chart helper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pie_chart) //set layout

        db = AppDatabase.getDatabase(this) //init database
        graphHelper = GraphHelper(this) //init graph helper

        val prefs = getSharedPreferences("thryft_session", MODE_PRIVATE) //get session prefs
        val userId = prefs.getString("user_id", null) //get Firebase user ID as String

        if (userId.isNullOrEmpty()) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val pieChart = findViewById<PieChart>(R.id.pieChart) //find pie chart view

        CoroutineScope(Dispatchers.IO).launch {
            val transactions = db.transactionDao().getAllTransactions(userId) //get transactions for user
            val categories = db.categoryDao().getAllCategories(userId) //get user categories
            val incomeTransactions = transactions.filter { it.type == "INCOME" } //filter income

            runOnUiThread {
                pieChart.setUsePercentValues(true) //enable percentage view
                graphHelper.setupPieChartByCategory(pieChart, incomeTransactions, categories) //draw chart
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.*

class IncomePieChartActivity : AppCompatActivity() {

    private lateinit var graphHelper: GraphHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pie_chart)

        graphHelper = GraphHelper(this)
        val pieChart = findViewById<PieChart>(R.id.pieChart)

        val prefs = getSharedPreferences("thryft_session", MODE_PRIVATE)
        val userId = prefs.getString("user_id", null)

        if (userId.isNullOrEmpty()) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val firestore = FirebaseFirestore.getInstance()

                val txnSnapshot = firestore.collection("transactions")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("type", "INCOME")
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
                }

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
                    Toast.makeText(this@IncomePieChartActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
