package com.example.thryftapp

import android.content.Context
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

class GraphHelper(private val context: Context) {

    fun setupExpenseVsIncomeChart(chart: BarChart, transactions: List<Transaction>) {
        val incomeEntries = mutableListOf<BarEntry>()
        val expenseEntries = mutableListOf<BarEntry>()

        // Aggregate data
        val income = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
        val expense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }

        incomeEntries.add(BarEntry(0f, income.toFloat()))
        expenseEntries.add(BarEntry(1f, expense.toFloat())) // Set index to 1 for expense

        val incomeDataSet = BarDataSet(incomeEntries, "Income").apply {
            color = ColorTemplate.COLORFUL_COLORS[0]
        }
        val expenseDataSet = BarDataSet(expenseEntries, "Expense").apply {
            color = ColorTemplate.COLORFUL_COLORS[1]
        }

        val barData = BarData(incomeDataSet, expenseDataSet)

        // Set properties for grouped bars
        val groupSpace = 0.2f
        val barSpace = 0.05f
        val barWidth = 0.35f

        barData.barWidth = barWidth
        chart.data = barData

        chart.xAxis.apply {
            axisMinimum = 0f
            axisMaximum = 1f
            granularity = 1f
            isGranularityEnabled = true
            valueFormatter = IndexAxisValueFormatter(listOf("Income", "Expense")) // Custom labels
            textSize = 12f // Adjust text size if needed
        }

        chart.groupBars(0f, groupSpace, barSpace)
        chart.setFitBars(true)
        chart.description.isEnabled = false
        chart.legend.isEnabled = true // Enable legend
        chart.animateY(1000)
        chart.invalidate()
    }


    fun setupPieChartByCategory(pieChart: PieChart, transactions: List<Transaction>, categories: List<Category>) {
        val entries = mutableListOf<PieEntry>()

        val categoryMap = categories.associateBy { it.id }

        val totalsByCategory = transactions.groupBy { it.categoryId }.mapValues { entry ->
            entry.value.sumOf { it.amount }
        }

        for ((catId, total) in totalsByCategory) {
            val categoryName = categoryMap[catId]?.name ?: "Unknown"
            entries.add(PieEntry(total.toFloat(), categoryName))
        }

        val dataSet = PieDataSet(entries, "By Category")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        val data = PieData(dataSet)
        data.setValueTextSize(14f)

        pieChart.data = data
        pieChart.description.isEnabled = false
        pieChart.animateY(1000)
        pieChart.invalidate()
    }



}