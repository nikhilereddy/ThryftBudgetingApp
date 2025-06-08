package com.example.thryftapp

import android.content.Context
import android.graphics.Color
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.github.mikephil.charting.components.XAxis
import java.text.SimpleDateFormat
import java.util.*

class GraphHelper(private val context: Context) {

    fun setupExpenseVsIncomeChart(chart: BarChart, transactions: List<Transaction>) {
        val incomeEntries = mutableListOf<BarEntry>()
        val expenseEntries = mutableListOf<BarEntry>()

        val income = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
        val expense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }

        incomeEntries.add(BarEntry(0f, income.toFloat()))
        expenseEntries.add(BarEntry(1f, expense.toFloat()))

        val incomeDataSet = BarDataSet(incomeEntries, "Income").apply {
            color = ColorTemplate.COLORFUL_COLORS[0]
        }
        val expenseDataSet = BarDataSet(expenseEntries, "Expense").apply {
            color = ColorTemplate.COLORFUL_COLORS[1]
        }

        val barData = BarData(incomeDataSet, expenseDataSet)

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
            valueFormatter = IndexAxisValueFormatter(listOf("Income", "Expense"))
            textSize = 12f
        }

        chart.groupBars(0f, groupSpace, barSpace)
        chart.setFitBars(true)
        chart.description.isEnabled = false
        chart.legend.isEnabled = true
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

    fun setupGoalsTimelineChart(chart: LineChart, transactions: List<Transaction>, categories: List<Category>) {
        val categoryMap = categories.associateBy { it.id }
        val dateFormatter = SimpleDateFormat("yyyy-MM", Locale.getDefault())

        val groupedActuals = transactions.groupBy {
            dateFormatter.format(it.date)
        }.mapValues { entry ->
            entry.value.sumOf { it.amount }
        }

        val groupedGoals = transactions.groupBy {
            dateFormatter.format(it.date)
        }.mapValues { entry ->
            entry.value.sumOf { txn ->
                val cat = categoryMap[txn.categoryId]
                if (txn.type.equals("INCOME", true)) cat?.minBudget ?: 0.0 else cat?.maxBudget ?: 0.0
            }
        }

        val months = groupedActuals.keys.union(groupedGoals.keys).sorted()
        val actualEntries = mutableListOf<Entry>()
        val goalEntries = mutableListOf<Entry>()

        months.forEachIndexed { index, month ->
            val actual = groupedActuals[month] ?: 0.0
            val goal = groupedGoals[month] ?: 0.0
            actualEntries.add(Entry(index.toFloat(), actual.toFloat()))
            goalEntries.add(Entry(index.toFloat(), goal.toFloat()))
        }

        val actualDataSet = LineDataSet(actualEntries, "Actual").apply {
            color = Color.BLUE
            valueTextSize = 12f
            lineWidth = 2f
            circleRadius = 4f
        }

        val goalDataSet = LineDataSet(goalEntries, "Goal").apply {
            color = Color.GRAY
            valueTextSize = 12f
            lineWidth = 2f
            circleRadius = 4f
        }

        val lineData = LineData(actualDataSet, goalDataSet)
        chart.data = lineData

        chart.xAxis.apply {
            granularity = 1f
            valueFormatter = IndexAxisValueFormatter(months)
            position = XAxis.XAxisPosition.BOTTOM
            textSize = 10f
        }

        chart.axisLeft.axisMinimum = 0f
        chart.axisRight.isEnabled = false
        chart.description.isEnabled = false
        chart.legend.isEnabled = true
        chart.animateX(1000)
        chart.invalidate()
    }

    fun setupCategorySpendingWithGoalsChart(
        chart: BarChart,
        transactions: List<Transaction>,
        categories: List<Category>
    ) {
        val categoryMap = categories.associateBy { it.id }

        val grouped = transactions
            .groupBy { it.categoryId }
            .filterKeys { categoryMap.containsKey(it) }

        val actualEntries = mutableListOf<BarEntry>()
        val minEntries = mutableListOf<BarEntry>()
        val maxEntries = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()

        var index = 0f

        for ((catId, txnList) in grouped) {
            val category = categoryMap[catId] ?: continue
            labels.add(category.name)

            val actualAmount = txnList.sumOf { it.amount }
            actualEntries.add(BarEntry(index, actualAmount.toFloat()))

            minEntries.add(BarEntry(index, category.minBudget.toFloat()))
            maxEntries.add(BarEntry(index, category.maxBudget.toFloat()))

            index++
        }

        val actualSet = BarDataSet(actualEntries, "Actual").apply {
            color = Color.BLUE
        }
        val minSet = BarDataSet(minEntries, "Min Goal").apply {
            color = Color.GREEN
        }
        val maxSet = BarDataSet(maxEntries, "Max Goal").apply {
            color = Color.RED
        }

        val barData = BarData(actualSet, minSet, maxSet)

        // Define spacing
        val barWidth = 0.2f
        val barSpace = 0.05f
        val groupSpace = 1f - (3 * barWidth + 2 * barSpace)

        barData.barWidth = barWidth

        chart.data = barData
        chart.setFitBars(true)

        chart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(labels)
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            isGranularityEnabled = true
            setDrawGridLines(false)
            labelRotationAngle = -15f
            textSize = 12f
            axisMinimum = 0f
            axisMaximum = labels.size.toFloat()
        }

        chart.axisLeft.axisMinimum = 0f
        chart.axisRight.isEnabled = false
        chart.description.isEnabled = false
        chart.legend.isEnabled = true
        chart.groupBars(0f, groupSpace, barSpace)
        chart.animateY(1000)
        chart.invalidate()
    }

}
