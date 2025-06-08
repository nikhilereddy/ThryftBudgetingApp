/*
package com.example.thryftapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.BarChart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GraphActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase //database reference
    private lateinit var graphHelper: GraphHelper //helper for drawing charts

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_graph) //set layout

        db = AppDatabase.getDatabase(this) //init db
        graphHelper = GraphHelper(this) //init chart helper

        //retrieve user id from session
        val prefs = getSharedPreferences("thryft_session", MODE_PRIVATE)
        val userId = prefs.getInt("user_id", -1)

        if (userId == -1) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val barChart = findViewById<BarChart>(R.id.barChart) //find bar chart view

        //fetch transactions and update chart
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val transactions = db.transactionDao().getAllTransactions(userId) //get all transactions

                //if there are no transactions
                if (transactions.isEmpty()) {
                    runOnUiThread {
                        Toast.makeText(this@GraphActivity, "No transactions available", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    runOnUiThread {
                        graphHelper.setupExpenseVsIncomeChart(barChart, transactions) //draw chart
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@GraphActivity, "Error loading data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        findViewById<Button>(R.id.incomePieChartBtn).setOnClickListener {
            val intent = Intent(this, IncomePieChartActivity::class.java)
            startActivity(intent) //navigate to income chart
        }

        findViewById<Button>(R.id.expensePieChartBtn).setOnClickListener {
            val intent = Intent(this, ExpensePieChartActivity::class.java)
            startActivity(intent) //navigate to expense chart
        }
    }
}
*/
/*
package com.example.thryftapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.BarChart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GraphActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase //database reference
    private lateinit var graphHelper: GraphHelper //helper for drawing charts

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_graph) //set layout

        db = AppDatabase.getDatabase(this) //init database
        graphHelper = GraphHelper(this) //init chart helper

        val prefs = getSharedPreferences("thryft_session", MODE_PRIVATE)
        val userId = prefs.getString("user_id", null) //use string ID (Firebase UID)

        if (userId.isNullOrEmpty()) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val barChart = findViewById<BarChart>(R.id.barChart) //find bar chart view

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val transactions = db.transactionDao().getAllTransactions(userId) //get user transactions

                runOnUiThread {
                    if (transactions.isEmpty()) {
                        Toast.makeText(this@GraphActivity, "No transactions available", Toast.LENGTH_SHORT).show()
                    } else {
                        graphHelper.setupExpenseVsIncomeChart(barChart, transactions) //draw chart
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@GraphActivity, "Error loading data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        findViewById<Button>(R.id.incomePieChartBtn).setOnClickListener {
            startActivity(Intent(this, IncomePieChartActivity::class.java)) //navigate to income chart
        }

        findViewById<Button>(R.id.expensePieChartBtn).setOnClickListener {
            startActivity(Intent(this, ExpensePieChartActivity::class.java)) //navigate to expense chart
        }
    }
}
*/
package com.example.thryftapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.BarChart
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

class GraphActivity : AppCompatActivity() {

    private lateinit var graphHelper: GraphHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_graph)

        graphHelper = GraphHelper(this)

        val prefs = getSharedPreferences("thryft_session", MODE_PRIVATE)
        val userId = prefs.getString("user_id", null)

        if (userId.isNullOrEmpty()) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val barChart = findViewById<BarChart>(R.id.barChart)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val firestore = FirebaseFirestore.getInstance()

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
                }

                runOnUiThread {
                    if (transactions.isEmpty()) {
                        Toast.makeText(this@GraphActivity, "No transactions available", Toast.LENGTH_SHORT).show()
                    } else {
                        graphHelper.setupExpenseVsIncomeChart(barChart, transactions)
                    }
                }

            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@GraphActivity, "Error loading data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        findViewById<Button>(R.id.incomePieChartBtn).setOnClickListener {
            startActivity(Intent(this, IncomePieChartActivity::class.java))
        }

        findViewById<Button>(R.id.expensePieChartBtn).setOnClickListener {
            startActivity(Intent(this, ExpensePieChartActivity::class.java))
        }
    }
}
