package com.example.thryftapp

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.PieChart
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.util.*

class ExpensePieChartActivity : AppCompatActivity() {

    private lateinit var graphHelper: GraphHelper //chart helper

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
        /**
         * Attribution:
         * Website: Update XAxis values in MPAndroidChart

         *  Author: aiwiguna
         *  URL: https://stackoverflow.com/questions/63807093/update-xaxis-values-in-mpandroidchart
         *  Accessed on: 2025-06-06
        -        */
        val pieChart = findViewById<PieChart>(R.id.pieChart)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val firestore = FirebaseFirestore.getInstance()

                //Fetch transactions
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

                //Fetch categories
                val catSnapshot = firestore.collection("users")
                    .document(userId)
                    .collection("categories")
                    .get()
                    .await()

                //map category documents to category objects
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
                        Log.d("AnalyticsFragment", "failed to parse category: ${e.message}") //log parse error
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
