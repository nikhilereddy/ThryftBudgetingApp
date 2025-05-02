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

class ExpensePieFragment : Fragment() {

    private lateinit var pieChart: PieChart //pie chart view
    private lateinit var db: AppDatabase //database reference
    private lateinit var graphHelper: GraphHelper //helper for chart setup

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_expense_pie, container, false) //inflate layout

        pieChart = view.findViewById(R.id.pieChart) //find pie chart
        db = AppDatabase.getDatabase(requireContext()) //init db
        graphHelper = GraphHelper(requireContext()) //init graph helper

        loadData() //load data into chart

        return view
    }

    private fun loadData() {
        val prefs = requireContext().getSharedPreferences("thryft_session", Context.MODE_PRIVATE) //get prefs
        val userId = prefs.getInt("user_id", -1) //get user id

        if (userId == -1) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val transactions = db.transactionDao().getAllTransactions(userId) //get transactions
            val categories = db.categoryDao().getAllCategories(userId) //get categories
            val expenseTransactions = transactions.filter { it.type.equals("EXPENSE", ignoreCase = true) } //filter expense

            withContext(Dispatchers.Main) {
                pieChart.setUsePercentValues(true) //enable percent values
                graphHelper.setupPieChartByCategory(pieChart, expenseTransactions, categories) //draw chart
            }
        }
    }
}
