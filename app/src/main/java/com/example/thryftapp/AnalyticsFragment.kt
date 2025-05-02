package com.example.thryftapp

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AnalyticsFragment : Fragment() {

    private lateinit var pieChart: PieChart
    private lateinit var barChart: BarChart
    private lateinit var db: AppDatabase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_analytics, container, false)

        pieChart = view.findViewById(R.id.incomeExpensePieChart)
        barChart = view.findViewById(R.id.categoryBarChart)
        db = AppDatabase.getDatabase(requireContext())

        // Button navigation to pie fragments
        view.findViewById<Button>(R.id.incomePieChartBtn).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, IncomePieFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<Button>(R.id.expensePieChartBtn).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ExpensePieFragment())
                .addToBackStack(null)
                .commit()
        }

        loadAnalytics()

        return view
    }

    private fun loadAnalytics() {
        val userId = getUserIdFromPrefs()
        if (userId == -1) return

        lifecycleScope.launch(Dispatchers.IO) {
            val transactions = db.transactionDao().getAllTransactions(userId)
            val allCategories = db.categoryDao().getAllCategories(userId)
            val categoryMap = allCategories.associateBy { it.id }

            val incomeTotal = transactions.filter { it.type.equals("Income", true) }.sumOf { it.amount }
            val expenseTotal = transactions.filter { it.type.equals("Expense", true) }.sumOf { it.amount }

            val expensesByCategory = transactions
                .filter { it.type.equals("Expense", true) && it.categoryId != null }
                .groupBy { it.categoryId!! }
                .mapValues { entry -> entry.value.sumOf { it.amount } }

            withContext(Dispatchers.Main) {
                showPieChart(incomeTotal, expenseTotal)
                showBarChart(incomeTotal, expenseTotal)


        }
        }
    }


    private fun showPieChart(income: Double, expense: Double) {
        if (income == 0.0 && expense == 0.0) {
            pieChart.clear()
            pieChart.setNoDataText("No income or expense data available")
            return
        }

        val entries = listOf(
            PieEntry(income.toFloat(), "Income"),
            PieEntry(expense.toFloat(), "Expense")
        )

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = listOf(Color.GREEN, Color.RED)
        val data = PieData(dataSet)

        pieChart.data = data
        pieChart.description.isEnabled = false
        pieChart.setUsePercentValues(true)
        pieChart.setEntryLabelColor(Color.BLACK)
        pieChart.invalidate()
    }

    private fun showBarChart(income: Double, expense: Double) {
        if (income == 0.0 && expense == 0.0) {
            barChart.clear()
            barChart.setNoDataText("No income or expense data available")
            return
        }

        val entries = listOf(
            BarEntry(0f, income.toFloat()),
            BarEntry(1f, expense.toFloat())
        )

        val dataSet = BarDataSet(entries, "Income vs Expense")
        dataSet.colors = listOf(
            Color.parseColor("#4CAF50"), // Income
            Color.parseColor("#F44336")  // Expense
        )
        dataSet.valueTextSize = 14f

        val barData = BarData(dataSet)
        barData.barWidth = 0.4f

        barChart.data = barData
        barChart.setFitBars(true)
        barChart.description.isEnabled = false

        barChart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(listOf("Income", "Expense"))
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            setDrawLabels(true)
            setDrawGridLines(false)
            textSize = 12f
        }

        barChart.axisLeft.axisMinimum = 0f
        barChart.axisRight.isEnabled = false
        barChart.legend.isEnabled = true
        barChart.animateY(1000)
        barChart.invalidate()
    }

    private fun getUserIdFromPrefs(): Int {
        val prefs = requireContext().getSharedPreferences("thryft_session", Context.MODE_PRIVATE)
        return prefs.getInt("user_id", -1)
    }
}
