package com.example.thryftapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
        /**
         * Attribution:
         * Website: Update XAxis values in MPAndroidChart

         *  Author: aiwiguna
         *  URL: https://stackoverflow.com/questions/63807093/update-xaxis-values-in-mpandroidchart
         *  Accessed on: 2025-06-06
        -        */
        //launch background coroutine to load transactions and draw chart
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val firestore = FirebaseFirestore.getInstance()

                //fetch transactions for current user
                val txnSnapshot = firestore.collection("transactions")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()

                //parse transactions from documents
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
                        Log.d("GraphActivity", "failed to parse transaction: ${e.message}") //log error
                        null
                    }
                }

                //update ui with data
                runOnUiThread {
                    if (transactions.isEmpty()) {
                        Toast.makeText(this@GraphActivity, "No transactions available", Toast.LENGTH_SHORT).show()
                        Log.d("GraphActivity", "no transactions found") //log empty state
                    } else {
                        graphHelper.setupExpenseVsIncomeChart(barChart, transactions)
                        Log.d("GraphActivity", "chart setup with ${transactions.size} transactions") //log success
                    }
                }

            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@GraphActivity, "Error loading data: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.d("GraphActivity", "error loading data: ${e.message}") //log failure
                }
            }
        }

        /**
         * Attribution:
         * Website: Update XAxis values in MPAndroidChart

         *  Author: aiwiguna
         *  URL: https://stackoverflow.com/questions/63807093/update-xaxis-values-in-mpandroidchart
         *  Accessed on: 2025-06-06
        -        */
        findViewById<Button>(R.id.incomePieChartBtn).setOnClickListener {
            startActivity(Intent(this, IncomePieChartActivity::class.java))
        }

        findViewById<Button>(R.id.expensePieChartBtn).setOnClickListener {
            startActivity(Intent(this, ExpensePieChartActivity::class.java))
        }
    }
}
