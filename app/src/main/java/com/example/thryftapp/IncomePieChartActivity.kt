package com.example.thryftapp

import android.os.Bundle
import android.util.Log
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
        /**
         * Attribution:
         * Website: Update XAxis values in MPAndroidChart

         *  Author: aiwiguna
         *  URL: https://stackoverflow.com/questions/63807093/update-xaxis-values-in-mpandroidchart
         *  Accessed on: 2025-06-06
        -        */
        //load income transactions and categories and setup pie chart
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val firestore = FirebaseFirestore.getInstance()

                //fetch income transactions for user
                val txnSnapshot = firestore.collection("transactions")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("type", "INCOME")
                    .get()
                    .await()

                //parse transaction documents
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
                        Log.d("IncomePieChart", "failed to parse transaction: ${e.message}") //log error
                        null
                    }
                }

                Log.d("IncomePieChart", "loaded ${transactions.size} income transactions") //log transaction count

                //fetch categories
                val catSnapshot = firestore.collection("users")
                    .document(userId)
                    .collection("categories")
                    .get()
                    .await()

                //parse category documents
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
                        Log.d("IncomePieChart", "failed to parse category: ${e.message}") //log error
                        null
                    }
                }
                /**
                 * Attribution:
                 * Website: Update XAxis values in MPAndroidChart

                 *  Author: aiwiguna
                 *  URL: https://stackoverflow.com/questions/63807093/update-xaxis-values-in-mpandroidchart
                 *  Accessed on: 2025-06-06
                -        */
                Log.d("IncomePieChart", "loaded ${categories.size} categories") //log category count

                //update pie chart on main thread
                withContext(Dispatchers.Main) {
                    pieChart.setUsePercentValues(true)
                    graphHelper.setupPieChartByCategory(pieChart, transactions, categories)
                    Log.d("IncomePieChart", "pie chart rendered") //log success
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@IncomePieChartActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.d("IncomePieChart", "error loading data: ${e.message}") //log failure
                }
            }
        }
    }
}
