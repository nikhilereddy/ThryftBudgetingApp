package com.example.thryftapp

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.PieChart
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.*

class IncomePieFragment : Fragment() {

    private lateinit var pieChart: PieChart
    private lateinit var graphHelper: GraphHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_income_pie, container, false)

        pieChart = view.findViewById(R.id.pieChart)
        graphHelper = GraphHelper(requireContext())

        loadData()

        return view
    }
    /**
     * Attribution:
     * Website: Update XAxis values in MPAndroidChart

     *  Author: aiwiguna
     *  URL: https://stackoverflow.com/questions/63807093/update-xaxis-values-in-mpandroidchart
     *  Accessed on: 2025-06-06
    -        */
    //load income transactions and categories for the current user
    private fun loadData() {
        val prefs = requireContext().getSharedPreferences("thryft_session", Context.MODE_PRIVATE)
        val userId = prefs.getString("user_id", null)

        //check if user is logged in
        if (userId.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            Log.d("IncomeFragment", "user_id not found in shared prefs") //log missing user
            return
        }

        //launch coroutine to fetch data
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val firestore = FirebaseFirestore.getInstance()

                //fetch income transactions
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
                        Log.d("IncomeFragment", "failed to parse transaction: ${e.message}") //log error
                        null
                    }
                }

                Log.d("IncomeFragment", "loaded ${transactions.size} income transactions") //log count

                //fetch category data
                val catSnapshot = firestore.collection("users")
                    .document(userId)
                    .collection("categories")
                    .get()
                    .await()

                //parse categories
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
                        Log.d("IncomeFragment", "failed to parse category: ${e.message}") //log error
                        null
                    }
                }

                Log.d("IncomeFragment", "loaded ${categories.size} categories") //log count

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
                    Toast.makeText(requireContext(), "Error loading data: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
