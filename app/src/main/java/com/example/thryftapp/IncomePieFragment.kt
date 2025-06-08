/*
package com.example.thryftapp

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.PieChart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class IncomePieFragment : Fragment() {

    private lateinit var pieChart: PieChart //pie chart view
    private lateinit var db: AppDatabase //database reference
    private lateinit var graphHelper: GraphHelper //chart helper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_income_pie, container, false) //inflate layout

        pieChart = view.findViewById(R.id.pieChart) //bind pie chart
        db = AppDatabase.getDatabase(requireContext()) //init database
        graphHelper = GraphHelper(requireContext()) //init chart helper

        loadData() //load and display data

        return view
    }

    private fun loadData() {
        val prefs = requireContext().getSharedPreferences("thryft_session", Context.MODE_PRIVATE) //get session prefs
        val userId = prefs.getInt("user_id", -1) //get user id

        if (userId == -1) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show() //show error
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val transactions = db.transactionDao().getAllTransactions(userId) //get transactions
            val categories = db.categoryDao().getAllCategories(userId) //get categories
            val incomeTransactions = transactions.filter { it.type.equals("INCOME", true) } //filter income

            withContext(Dispatchers.Main) {
                pieChart.setUsePercentValues(true) //enable percent display
                graphHelper.setupPieChartByCategory(pieChart, incomeTransactions, categories) //render pie chart
            }
        }
    }
}
*/
/*
package com.example.thryftapp

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.PieChart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class IncomePieFragment : Fragment() {

    private lateinit var pieChart: PieChart //pie chart view
    private lateinit var db: AppDatabase //database reference
    private lateinit var graphHelper: GraphHelper //chart helper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_income_pie, container, false) //inflate layout

        pieChart = view.findViewById(R.id.pieChart) //bind pie chart
        db = AppDatabase.getDatabase(requireContext()) //init database
        graphHelper = GraphHelper(requireContext()) //init chart helper

        loadData() //load and display data

        return view
    }

    private fun loadData() {
        val prefs = requireContext().getSharedPreferences("thryft_session", Context.MODE_PRIVATE)
        val userId = prefs.getString("user_id", null) //get user id as String (Firebase UID)

        if (userId.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val transactions = db.transactionDao().getAllTransactions(userId) //get transactions
            val categories = db.categoryDao().getAllCategories(userId) //get categories
            val incomeTransactions = transactions.filter { it.type.equals("INCOME", true) } //filter income

            withContext(Dispatchers.Main) {
                pieChart.setUsePercentValues(true) //enable percent display
                graphHelper.setupPieChartByCategory(pieChart, incomeTransactions, categories) //render pie chart
            }
        }
    }
}
*/
package com.example.thryftapp

import android.content.Context
import android.os.Bundle
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

    private fun loadData() {
        val prefs = requireContext().getSharedPreferences("thryft_session", Context.MODE_PRIVATE)
        val userId = prefs.getString("user_id", null)

        if (userId.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
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
                    Toast.makeText(requireContext(), "Error loading data: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
