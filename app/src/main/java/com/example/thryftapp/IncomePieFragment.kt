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
