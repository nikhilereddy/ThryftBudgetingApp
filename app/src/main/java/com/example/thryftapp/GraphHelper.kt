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
        val incomeEntries = mutableListOf<BarEntry>() //bar entries for income
        val expenseEntries = mutableListOf<BarEntry>() //bar entries for expense

        //aggregate data
        val income = transactions.filter { it.type == "INCOME" }.sumOf { it.amount } //sum income
        val expense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount } //sum expense

        incomeEntries.add(BarEntry(0f, income.toFloat()))
        expenseEntries.add(BarEntry(1f, expense.toFloat())) //set index to 1 for expense

        val incomeDataSet = BarDataSet(incomeEntries, "Income").apply {
            color = ColorTemplate.COLORFUL_COLORS[0] //set income color
        }
        val expenseDataSet = BarDataSet(expenseEntries, "Expense").apply {
            color = ColorTemplate.COLORFUL_COLORS[1] //set expense color
        }

        val barData = BarData(incomeDataSet, expenseDataSet) //combine datasets

        //set properties for grouped bars
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
            valueFormatter = IndexAxisValueFormatter(listOf("Income", "Expense")) //custom labels
            textSize = 12f //adjust text size
        }

        chart.groupBars(0f, groupSpace, barSpace)
        chart.setFitBars(true)
        chart.description.isEnabled = false
        chart.legend.isEnabled = true //enable legend
        chart.animateY(1000)
        chart.invalidate()
    }

    fun setupPieChartByCategory(pieChart: PieChart, transactions: List<Transaction>, categories: List<Category>) {
        val entries = mutableListOf<PieEntry>() //pie chart entries

        val categoryMap = categories.associateBy { it.id } //map category by id

        val totalsByCategory = transactions.groupBy { it.categoryId }.mapValues { entry ->
            entry.value.sumOf { it.amount } //sum totals by category
        }

        for ((catId, total) in totalsByCategory) {
            val categoryName = categoryMap[catId]?.name ?: "Unknown" //get category name
            entries.add(PieEntry(total.toFloat(), categoryName)) //add entry
        }

        val dataSet = PieDataSet(entries, "By Category") //create dataset
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList() //set colors
        val data = PieData(dataSet)
        data.setValueTextSize(14f)

        pieChart.data = data
        pieChart.description.isEnabled = false
        pieChart.animateY(1000)
        pieChart.invalidate()
    }
}
