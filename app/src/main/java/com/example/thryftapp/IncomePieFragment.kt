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

    private lateinit var pieChart: PieChart
    private lateinit var db: AppDatabase
    private lateinit var graphHelper: GraphHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_income_pie, container, false)

        pieChart = view.findViewById(R.id.pieChart)
        db = AppDatabase.getDatabase(requireContext())
        graphHelper = GraphHelper(requireContext())

        loadData()

        return view
    }

    private fun loadData() {
        val prefs = requireContext().getSharedPreferences("thryft_session", Context.MODE_PRIVATE)
        val userId = prefs.getInt("user_id", -1)

        if (userId == -1) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val transactions = db.transactionDao().getAllTransactions(userId)
            val categories = db.categoryDao().getAllCategories(userId)
            val incomeTransactions = transactions.filter { it.type.equals("INCOME", true) }

            withContext(Dispatchers.Main) {
                pieChart.setUsePercentValues(true)
                graphHelper.setupPieChartByCategory(pieChart, incomeTransactions, categories)
            }
        }
    }
}
