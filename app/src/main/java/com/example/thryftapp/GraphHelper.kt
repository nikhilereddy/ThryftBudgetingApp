package com.example.thryftapp

import android.content.Context
import android.graphics.Color
import android.util.Log
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
    /**
     * Attribution:
     * Website: Update XAxis values in MPAndroidChart

     *  Author: aiwiguna
     *  URL: https://stackoverflow.com/questions/63807093/update-xaxis-values-in-mpandroidchart
     *  Accessed on: 2025-06-06
    -        */

    //setup bar chart to compare income vs expense
    fun setupExpenseVsIncomeChart(chart: BarChart, transactions: List<Transaction>) {
        //initialize bar entries
        val incomeEntries = mutableListOf<BarEntry>()
        val expenseEntries = mutableListOf<BarEntry>()

        //calculate income total
        val income = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
        //calculate expense total
        val expense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }

        Log.d("AnalyticsFragment", "income=$income, expense=$expense") //log calculated totals

        //add income entry to chart
        incomeEntries.add(BarEntry(0f, income.toFloat()))
        //add expense entry to chart
        expenseEntries.add(BarEntry(1f, expense.toFloat()))

        //create income dataset
        val incomeDataSet = BarDataSet(incomeEntries, "Income").apply {
            color = ColorTemplate.COLORFUL_COLORS[0]
        }

        //create expense dataset
        val expenseDataSet = BarDataSet(expenseEntries, "Expense").apply {
            color = ColorTemplate.COLORFUL_COLORS[1]
        }

        //create combined bar data
        val barData = BarData(incomeDataSet, expenseDataSet)

        //define spacing for grouped bars
        val groupSpace = 0.2f
        val barSpace = 0.05f
        val barWidth = 0.35f

        //apply bar width to dataset
        barData.barWidth = barWidth
        //set data on chart
        chart.data = barData

        //configure x axis for grouping
        chart.xAxis.apply {
            axisMinimum = 0f
            axisMaximum = 1f
            granularity = 1f
            isGranularityEnabled = true
            valueFormatter = IndexAxisValueFormatter(listOf("Income", "Expense"))
            textSize = 12f
        }

        //group bars using spacing
        chart.groupBars(0f, groupSpace, barSpace)
        chart.setFitBars(true)
        chart.description.isEnabled = false
        chart.legend.isEnabled = true
        chart.animateY(1000)
        chart.invalidate()

        Log.d("AnalyticsFragment", "expense vs income chart setup complete") //log completion
    }


    //setup pie chart by category totals
    fun setupPieChartByCategory(pieChart: PieChart, transactions: List<Transaction>, categories: List<Category>) {
        //create list to hold pie entries
        val entries = mutableListOf<PieEntry>()

        //map category ids to category objects
        val categoryMap = categories.associateBy { it.id }

        //group transactions and sum amounts by category
        val totalsByCategory = transactions.groupBy { it.categoryId }.mapValues { entry ->
            entry.value.sumOf { it.amount }
        }

        //create pie entries for each category
        for ((catId, total) in totalsByCategory) {
            val categoryName = categoryMap[catId]?.name ?: "Unknown"
            entries.add(PieEntry(total.toFloat(), categoryName))
            Log.d("AnalyticsFragment", "added $categoryName with total $total to pie chart") //log entry
        }

        //create pie dataset with entries
        val dataSet = PieDataSet(entries, "By Category")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()

        //create pie data object
        val data = PieData(dataSet)
        data.setValueTextSize(14f)

        //set data to pie chart and render
        pieChart.data = data
        pieChart.description.isEnabled = false
        pieChart.animateY(1000)
        pieChart.invalidate()

        Log.d("AnalyticsFragment", "pie chart by category rendered with ${entries.size} entries") //log success
    }

    //setup line chart for goals vs actuals over time
    fun setupGoalsTimelineChart(chart: LineChart, transactions: List<Transaction>, categories: List<Category>) {
        //map categories by id
        val categoryMap = categories.associateBy { it.id }
        //format for month-year
        val dateFormatter = SimpleDateFormat("yyyy-MM", Locale.getDefault())

        //group actual totals by month
        val groupedActuals = transactions.groupBy {
            dateFormatter.format(it.date)
        }.mapValues { entry ->
            entry.value.sumOf { it.amount }
        }

        //group goal values by month
        val groupedGoals = transactions.groupBy {
            dateFormatter.format(it.date)
        }.mapValues { entry ->
            entry.value.sumOf { txn ->
                val cat = categoryMap[txn.categoryId]
                if (txn.type.equals("INCOME", true)) cat?.minBudget ?: 0.0 else cat?.maxBudget ?: 0.0
            }
        }

        //get sorted list of all months
        val months = groupedActuals.keys.union(groupedGoals.keys).sorted()
        //create line entries for actual and goal
        val actualEntries = mutableListOf<Entry>()
        val goalEntries = mutableListOf<Entry>()

        //build entry lists
        months.forEachIndexed { index, month ->
            val actual = groupedActuals[month] ?: 0.0
            val goal = groupedGoals[month] ?: 0.0
            actualEntries.add(Entry(index.toFloat(), actual.toFloat()))
            goalEntries.add(Entry(index.toFloat(), goal.toFloat()))
            Log.d("AnalyticsFragment", "month=$month, actual=$actual, goal=$goal") //log each point
        }

        //create dataset for actual values
        val actualDataSet = LineDataSet(actualEntries, "Actual").apply {
            color = Color.BLUE
            valueTextSize = 12f
            lineWidth = 2f
            circleRadius = 4f
        }

        //create dataset for goal values
        val goalDataSet = LineDataSet(goalEntries, "Goal").apply {
            color = Color.GRAY
            valueTextSize = 12f
            lineWidth = 2f
            circleRadius = 4f
        }

        //combine datasets into line data
        val lineData = LineData(actualDataSet, goalDataSet)
        chart.data = lineData

        //configure x axis with month labels
        chart.xAxis.apply {
            granularity = 1f
            valueFormatter = IndexAxisValueFormatter(months)
            position = XAxis.XAxisPosition.BOTTOM
            textSize = 10f
        }

        //final chart settings
        chart.axisLeft.axisMinimum = 0f
        chart.axisRight.isEnabled = false
        chart.description.isEnabled = false
        chart.legend.isEnabled = true
        chart.animateX(1000)
        chart.invalidate()

        Log.d("AnalyticsFragment", "goals timeline chart rendered with ${months.size} months") //log render
    }

    /**
     * Attribution:
     * Website: Update XAxis values in MPAndroidChart

     *  Author: aiwiguna
     *  URL: https://stackoverflow.com/questions/63807093/update-xaxis-values-in-mpandroidchart
     *  Accessed on: 2025-06-06
    -        */
    //setup bar chart showing category spending vs min and max goals
    fun setupCategorySpendingWithGoalsChart(
        chart: BarChart,
        transactions: List<Transaction>,
        categories: List<Category>
    ) {
        //map category ids to category objects
        val categoryMap = categories.associateBy { it.id }

        //group transactions by category
        val grouped = transactions
            .groupBy { it.categoryId }
            .filterKeys { categoryMap.containsKey(it) }

        //initialize bar entries and labels
        val actualEntries = mutableListOf<BarEntry>()
        val minEntries = mutableListOf<BarEntry>()
        val maxEntries = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()

        var index = 0f

        //build entries for each category
        for ((catId, txnList) in grouped) {
            val category = categoryMap[catId] ?: continue
            labels.add(category.name)

            val actualAmount = txnList.sumOf { it.amount }
            actualEntries.add(BarEntry(index, actualAmount.toFloat()))

            minEntries.add(BarEntry(index, category.minBudget.toFloat()))
            maxEntries.add(BarEntry(index, category.maxBudget.toFloat()))

            Log.d("AnalyticsFragment", "cat=${category.name}, actual=$actualAmount, min=${category.minBudget}, max=${category.maxBudget}") //log values

            index++
        }

        //create actual dataset
        val actualSet = BarDataSet(actualEntries, "Actual").apply {
            color = Color.BLUE
        }

        //create min goal dataset
        val minSet = BarDataSet(minEntries, "Min Goal").apply {
            color = Color.GREEN
        }

        //create max goal dataset
        val maxSet = BarDataSet(maxEntries, "Max Goal").apply {
            color = Color.RED
        }

        //combine all datasets
        val barData = BarData(actualSet, minSet, maxSet)

        //define spacing for grouped bars
        val barWidth = 0.2f
        val barSpace = 0.05f
        val groupSpace = 1f - (3 * barWidth + 2 * barSpace)

        barData.barWidth = barWidth

        //set data on chart
        chart.data = barData
        chart.setFitBars(true)

        //configure x axis
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

        //setup y axis and final render
        chart.axisLeft.axisMinimum = 0f
        chart.axisRight.isEnabled = false
        chart.description.isEnabled = false
        chart.legend.isEnabled = true
        chart.groupBars(0f, groupSpace, barSpace)
        chart.animateY(1000)
        chart.invalidate()

        Log.d("AnalyticsFragment", "category spending chart rendered with ${labels.size} categories") //log chart render
    }

    /**
     * Attribution:
     * Website: Update XAxis values in MPAndroidChart

     *  Author: aiwiguna
     *  URL: https://stackoverflow.com/questions/63807093/update-xaxis-values-in-mpandroidchart
     *  Accessed on: 2025-06-06
    -        */
}
