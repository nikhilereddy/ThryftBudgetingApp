/*
package com.example.thryftapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class AnalyticsFragment : Fragment() {

    private lateinit var pieChart: PieChart //pie chart for income vs expense
    private lateinit var incomeByCategoryChart: PieChart //pie chart for income by category
    private lateinit var expenseByCategoryChart: PieChart //pie chart for expense by category
    private lateinit var barChart: BarChart //bar chart for income vs expense
    private lateinit var db: AppDatabase //database instance
    private lateinit var graphHelper: GraphHelper //helper class to setup charts

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_analytics, container, false) //inflate layout

        pieChart = view.findViewById(R.id.incomeExpensePieChart) //find pie chart
        incomeByCategoryChart = view.findViewById(R.id.incomeByCategoryChart) //find income category chart
        expenseByCategoryChart = view.findViewById(R.id.expenseByCategoryChart) //find expense category chart
        barChart = view.findViewById(R.id.categoryBarChart) //find bar chart

        db = AppDatabase.getDatabase(requireContext()) //initialize database
        graphHelper = GraphHelper(requireContext()) //initialize graph helper

        view.findViewById<Button>(R.id.exportPdfBtn).setOnClickListener {
            view.postDelayed({
                exportAnalyticsAsPdf() //export chart as pdf
            }, 500) //ensures layout is complete
        }
        view.findViewById<ImageView>(R.id.backButton)?.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack() //navigate back
        }

        loadAnalytics() //load chart data

        return view
    }

    private fun loadAnalytics() {
        val userId = getUserIdFromPrefs() //get user id
        if (userId == -1) return //exit if user not found

        lifecycleScope.launch(Dispatchers.IO) {
            val transactions = db.transactionDao().getAllTransactions(userId) //get all transactions
            val categories = db.categoryDao().getAllCategories(userId) //get all categories

            val incomeTransactions = transactions.filter { it.type.equals("INCOME", true) } //filter income
            val expenseTransactions = transactions.filter { it.type.equals("EXPENSE", true) } //filter expenses

            val incomeTotal = incomeTransactions.sumOf { it.amount } //calculate income total
            val expenseTotal = expenseTransactions.sumOf { it.amount } //calculate expense total

            withContext(Dispatchers.Main) {
                showPieChart(incomeTotal, expenseTotal) //show income vs expense pie
                showBarChart(incomeTotal, expenseTotal) //show income vs expense bar

                populateInsights(expenseTransactions, incomeTransactions, categories) //show insights

                graphHelper.setupPieChartByCategory(
                    incomeByCategoryChart,
                    incomeTransactions,
                    categories
                )
                graphHelper.setupPieChartByCategory(
                    expenseByCategoryChart,
                    expenseTransactions,
                    categories
                )
            }
        }
    }

    private fun showPieChart(income: Double, expense: Double) {
        if (income == 0.0 && expense == 0.0) {
            pieChart.clear()
            pieChart.setNoDataText("No income or expense data available") //no data message
            return
        }

        val entries = mutableListOf<PieEntry>() //create pie entries
        if (income > 0.0) entries.add(PieEntry(income.toFloat(), "Income")) //add income
        if (expense > 0.0) entries.add(PieEntry(expense.toFloat(), "Expense")) //add expense

        val dataSet = PieDataSet(entries, "") //create dataset
        dataSet.colors = listOf(Color.GREEN, Color.RED) //set colors
        dataSet.valueTextSize = 14f
        dataSet.sliceSpace = 3f
        dataSet.valueTextColor = Color.BLACK
        dataSet.xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        dataSet.yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        dataSet.valueLinePart1Length = 0.6f
        dataSet.valueLinePart2Length = 0.4f
        dataSet.valueLineColor = Color.DKGRAY

        val data = PieData(dataSet) //create pie data

        pieChart.setUsePercentValues(true)
        pieChart.data = data
        pieChart.setEntryLabelColor(Color.TRANSPARENT)
        pieChart.setExtraOffsets(10f, 20f, 10f, 20f)
        pieChart.description.isEnabled = false
        pieChart.legend.isEnabled = true
        pieChart.animateY(1000)
        pieChart.invalidate()
    }

    private fun showBarChart(income: Double, expense: Double) {
        if (income == 0.0 && expense == 0.0) {
            barChart.clear()
            barChart.setNoDataText("No income or expense data available") //no data message
            return
        }

        val entries = listOf(
            BarEntry(0f, income.toFloat()), //income bar
            BarEntry(1f, expense.toFloat()) //expense bar
        )

        val dataSet = BarDataSet(entries, "Income vs Expense") //dataset for bar chart
        dataSet.colors = listOf(
            Color.parseColor("#4CAF50"), //green for income
            Color.parseColor("#F44336") //red for expense
        )
        dataSet.valueTextSize = 14f

        val barData = BarData(dataSet)
        barData.barWidth = 0.4f

        barChart.data = barData
        barChart.setFitBars(true)
        barChart.description.isEnabled = false

        barChart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(listOf("Income", "Expense")) //set x-axis labels
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
        return prefs.getInt("user_id", -1) //get stored user id
    }

    private fun populateInsights(
        expenses: List<Transaction>,
        incomes: List<Transaction>,
        categories: List<Category>
    ) {
        val topCategoryView = requireView().findViewById<TextView>(R.id.topCategoryText) //text for top categories
        val highestTxnView = requireView().findViewById<TextView>(R.id.highestTransactionText) //text for highest txn
        val suggestionView = requireView().findViewById<TextView>(R.id.suggestionText) //text for tips or warnings

        val categoryMap = categories.associateBy { it.id } //map of categories by id

        if (expenses.isEmpty()) {
            topCategoryView.text = "Top Expense Category: No expenses yet"
            highestTxnView.text = "Highest Transaction: None"
            suggestionView.text = "Tip: Start tracking to receive insights."
            return
        }

        val groupedExpenses = expenses.filter { it.categoryId != null }
            .groupBy { it.categoryId!! }
            .mapValues { it.value.sumOf { it.amount } }

        val groupedIncomes = incomes.filter { it.categoryId != null }
            .groupBy { it.categoryId!! }
            .mapValues { it.value.sumOf { it.amount } }

        val topExpense = groupedExpenses.maxByOrNull { it.value } //find top expense category
        val topExpenseCategoryName = topExpense?.key?.let { categoryMap[it]?.name } ?: "Unknown"
        val topExpenseAmount = topExpense?.value ?: 0.0

        val topIncome = groupedIncomes.maxByOrNull { it.value } //find top income category
        val topIncomeCategoryName = topIncome?.key?.let { categoryMap[it]?.name } ?: "Unknown"
        val topIncomeAmount = topIncome?.value ?: 0.0

        val highestTxn = expenses.maxByOrNull { it.amount } //find highest transaction
        val highestTxnCategory = highestTxn?.categoryId?.let { categoryMap[it]?.name } ?: "Unknown"

        topCategoryView.text = """
            Top Expense: $topExpenseCategoryName (R${"%.2f".format(topExpenseAmount)})
            Top Income: $topIncomeCategoryName (R${"%.2f".format(topIncomeAmount)})
        """.trimIndent()

        highestTxnView.text = "Highest Transaction: ${highestTxn?.description ?: "N/A"} " +
                "(R${"%.2f".format(highestTxn?.amount ?: 0.0)}, Category: $highestTxnCategory)"

        val budgetWarnings = mutableListOf<String>()
        for ((categoryId, totalSpent) in groupedExpenses) {
            val cat = categoryMap[categoryId] ?: continue
            if (cat.maxBudget > 0 && totalSpent > cat.maxBudget) {
                val over = totalSpent - cat.maxBudget
                budgetWarnings.add("⚠️ ${cat.name}: Over max budget by R${"%.2f".format(over)}")
            }
        }
        for ((categoryId, totalIncome) in groupedIncomes) {
            val cat = categoryMap[categoryId] ?: continue
            if (cat.minBudget > 0 && totalIncome < cat.minBudget) {
                val under = cat.minBudget - totalIncome
                budgetWarnings.add("💡 ${cat.name}: Income below goal by R${"%.2f".format(under)}")
            }
        }

        suggestionView.text = if (budgetWarnings.isNotEmpty()) {
            budgetWarnings.joinToString("\n")
        } else {
            "🎯 You're within your budgets. Keep up the good work!"
        }
    }

    private fun exportAnalyticsAsPdf() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(permission), 1002) //request notification permission
                return
            }
        }

        try {
            val pdfDocument = PdfDocument() //create new pdf
            val charts = listOf(pieChart, incomeByCategoryChart, expenseByCategoryChart, barChart) //list of charts

            var currentPageY = 0
            charts.forEachIndexed { index, chart ->
                val bmp = chart.chartBitmap
                val pageInfo = PdfDocument.PageInfo.Builder(bmp.width, bmp.height, index + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas
                canvas.drawBitmap(bmp, 0f, 0f, null)
                pdfDocument.finishPage(page)
                currentPageY += bmp.height
            }

            val filename = "analytics_export_${System.currentTimeMillis()}.pdf"
            val file = File(requireContext().getExternalFilesDir(null), filename)
            val out = FileOutputStream(file)
            pdfDocument.writeTo(out)
            pdfDocument.close()
            out.close()

            val uri = FileProvider.getUriForFile(
                requireContext(),
                "com.example.thryftapp.fileprovider", //match manifest
                file
            )

            val openIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }

            val pendingIntent = PendingIntent.getActivity(
                requireContext(), 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val channelId = "analytics_export_channel"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Analytics Export",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                val manager = requireContext().getSystemService(NotificationManager::class.java)
                manager.createNotificationChannel(channel)
            }

            val notification = NotificationCompat.Builder(requireContext(), channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Analytics PDF Exported")
                .setContentText("Tap to open your analytics report.")
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            NotificationManagerCompat.from(requireContext()).notify(1001, notification)

            Toast.makeText(requireContext(), "Exported to ${file.name}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1002 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            exportAnalyticsAsPdf() //call export if permission granted
        }
    }
}
*/
/*
package com.example.thryftapp

import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.*
import android.view.*
import android.widget.*
import androidx.core.app.*
import androidx.core.content.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.*
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.*
import java.io.*

class AnalyticsFragment : Fragment() {

    private lateinit var db: AppDatabase
    private lateinit var graphHelper: GraphHelper

    private lateinit var pieChart: PieChart
    private lateinit var incomeByCategoryChart: PieChart
    private lateinit var expenseByCategoryChart: PieChart
    private lateinit var barChart: BarChart

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_analytics, container, false)

        // initialize views
        pieChart = view.findViewById(R.id.incomeExpensePieChart)
        incomeByCategoryChart = view.findViewById(R.id.incomeByCategoryChart)
        expenseByCategoryChart = view.findViewById(R.id.expenseByCategoryChart)
        barChart = view.findViewById(R.id.categoryBarChart)

        // initialize db and helper
        db = AppDatabase.getDatabase(requireContext())
        graphHelper = GraphHelper(requireContext())

        view.findViewById<ImageView>(R.id.backButton)?.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        view.findViewById<Button>(R.id.exportPdfBtn).setOnClickListener {
            view.postDelayed({ exportAnalyticsAsPdf() }, 500)
        }

        loadAnalytics()

        return view
    }

    private fun getUserIdFromPrefs(): String {
        val prefs = requireContext().getSharedPreferences("thryft_session", Context.MODE_PRIVATE)
        return prefs.getString("user_id", "") ?: ""
    }

    private fun loadAnalytics() {
        val userId = getUserIdFromPrefs()
        if (userId.isEmpty()) return

        lifecycleScope.launch(Dispatchers.IO) {
            val transactions = db.transactionDao().getAllTransactions(userId)
            val categories = db.categoryDao().getAllCategories(userId)

            val incomeTxns = transactions.filter { it.type.equals("INCOME", true) }
            val expenseTxns = transactions.filter { it.type.equals("EXPENSE", true) }

            val incomeTotal = incomeTxns.sumOf { it.amount }
            val expenseTotal = expenseTxns.sumOf { it.amount }

            withContext(Dispatchers.Main) {
                showPieChart(incomeTotal, expenseTotal)
                showBarChart(incomeTotal, expenseTotal)

                populateInsights(expenseTxns, incomeTxns, categories)

                graphHelper.setupPieChartByCategory(incomeByCategoryChart, incomeTxns, categories)
                graphHelper.setupPieChartByCategory(expenseByCategoryChart, expenseTxns, categories)
            }
        }
    }

    private fun showPieChart(income: Double, expense: Double) {
        if (income == 0.0 && expense == 0.0) {
            pieChart.clear()
            pieChart.setNoDataText("No income or expense data available")
            return
        }

        val entries = mutableListOf<PieEntry>()
        if (income > 0) entries.add(PieEntry(income.toFloat(), "Income"))
        if (expense > 0) entries.add(PieEntry(expense.toFloat(), "Expense"))

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = listOf(Color.GREEN, Color.RED)
        dataSet.valueTextSize = 14f
        dataSet.sliceSpace = 3f
        dataSet.valueTextColor = Color.BLACK
        dataSet.xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        dataSet.yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        dataSet.valueLinePart1Length = 0.6f
        dataSet.valueLinePart2Length = 0.4f
        dataSet.valueLineColor = Color.DKGRAY

        pieChart.data = PieData(dataSet)
        pieChart.setUsePercentValues(true)
        pieChart.setEntryLabelColor(Color.TRANSPARENT)
        pieChart.setExtraOffsets(10f, 20f, 10f, 20f)
        pieChart.description.isEnabled = false
        pieChart.legend.isEnabled = true
        pieChart.animateY(1000)
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
        dataSet.colors = listOf(Color.parseColor("#4CAF50"), Color.parseColor("#F44336"))
        dataSet.valueTextSize = 14f

        val barData = BarData(dataSet).apply { barWidth = 0.4f }

        barChart.data = barData
        barChart.setFitBars(true)
        barChart.description.isEnabled = false

        barChart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(listOf("Income", "Expense"))
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            textSize = 12f
            setDrawGridLines(false)
        }

        barChart.axisLeft.axisMinimum = 0f
        barChart.axisRight.isEnabled = false
        barChart.legend.isEnabled = true
        barChart.animateY(1000)
        barChart.invalidate()
    }

    private fun populateInsights(
        expenses: List<Transaction>,
        incomes: List<Transaction>,
        categories: List<Category>
    ) {
        val topCategoryText = requireView().findViewById<TextView>(R.id.topCategoryText)
        val highestTransactionText = requireView().findViewById<TextView>(R.id.highestTransactionText)
        val suggestionText = requireView().findViewById<TextView>(R.id.suggestionText)

        val categoryMap = categories.associateBy { it.id }

        if (expenses.isEmpty()) {
            topCategoryText.text = "Top Expense Category: No expenses yet"
            highestTransactionText.text = "Highest Transaction: None"
            suggestionText.text = "Tip: Start tracking to receive insights."
            return
        }

        val expenseTotals = expenses.groupBy { it.categoryId }.mapValues { it.value.sumOf { txn -> txn.amount } }
        val incomeTotals = incomes.groupBy { it.categoryId }.mapValues { it.value.sumOf { txn -> txn.amount } }

        val topExpense = expenseTotals.maxByOrNull { it.value }
        val topIncome = incomeTotals.maxByOrNull { it.value }

        val highestTxn = expenses.maxByOrNull { it.amount }

        val topExpenseCat = topExpense?.key?.let { categoryMap[it]?.name } ?: "Unknown"
        val topIncomeCat = topIncome?.key?.let { categoryMap[it]?.name } ?: "Unknown"
        val highestCat = highestTxn?.categoryId?.let { categoryMap[it]?.name } ?: "Unknown"

        topCategoryText.text = """
            Top Expense: $topExpenseCat (R${"%.2f".format(topExpense?.value ?: 0.0)})
            Top Income: $topIncomeCat (R${"%.2f".format(topIncome?.value ?: 0.0)})
        """.trimIndent()

        highestTransactionText.text = "Highest Transaction: ${highestTxn?.description ?: "N/A"} " +
                "(R${"%.2f".format(highestTxn?.amount ?: 0.0)}, Category: $highestCat)"

        val budgetWarnings = mutableListOf<String>()

        expenseTotals.forEach { (id, spent) ->
            val cat = categoryMap[id] ?: return@forEach
            if (cat.maxBudget > 0 && spent > cat.maxBudget) {
                val over = spent - cat.maxBudget
                budgetWarnings.add("⚠️ ${cat.name}: Over budget by R${"%.2f".format(over)}")
            }
        }

        incomeTotals.forEach { (id, received) ->
            val cat = categoryMap[id] ?: return@forEach
            if (cat.minBudget > 0 && received < cat.minBudget) {
                val under = cat.minBudget - received
                budgetWarnings.add("💡 ${cat.name}: Income below goal by R${"%.2f".format(under)}")
            }
        }

        suggestionText.text = if (budgetWarnings.isNotEmpty()) {
            budgetWarnings.joinToString("\n")
        } else {
            "🎯 You're within your budgets. Keep it up!"
        }
    }

    private fun exportAnalyticsAsPdf() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(permission), 1002)
                return
            }
        }

        try {
            val charts = listOf(pieChart, incomeByCategoryChart, expenseByCategoryChart, barChart)
            val document = PdfDocument()

            charts.forEachIndexed { index, chart ->
                val bitmap = chart.chartBitmap
                val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1).create()
                val page = document.startPage(pageInfo)
                page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                document.finishPage(page)
            }

            val file = File(requireContext().getExternalFilesDir(null), "analytics_${System.currentTimeMillis()}.pdf")
            val out = FileOutputStream(file)
            document.writeTo(out)
            document.close()
            out.close()

            val uri = FileProvider.getUriForFile(
                requireContext(),
                "com.example.thryftapp.fileprovider",
                file
            )

            val openIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val pendingIntent = PendingIntent.getActivity(
                requireContext(), 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val channelId = "analytics_export_channel"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(channelId, "Analytics Export", NotificationManager.IMPORTANCE_DEFAULT)
                val manager = requireContext().getSystemService(NotificationManager::class.java)
                manager.createNotificationChannel(channel)
            }

            val notification = NotificationCompat.Builder(requireContext(), channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Analytics Exported")
                .setContentText("Tap to open PDF.")
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            NotificationManagerCompat.from(requireContext()).notify(1001, notification)
            Toast.makeText(requireContext(), "Exported to ${file.name}", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1002 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            exportAnalyticsAsPdf()
        }
    }
}
*/
package com.example.thryftapp

import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.*
import android.view.*
import android.widget.*
import androidx.core.app.*
import androidx.core.content.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.*
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class AnalyticsFragment : Fragment() {

    private lateinit var graphHelper: GraphHelper

    private lateinit var pieChart: PieChart
    private lateinit var incomeByCategoryChart: PieChart
    private lateinit var expenseByCategoryChart: PieChart
    private lateinit var barChart: BarChart
    private lateinit var goalsTimelineChart: LineChart
    private lateinit var categoryGoalsChart: BarChart
    private var customStartDate: Date? = null
    private var customEndDate: Date? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_analytics, container, false)
        goalsTimelineChart = view.findViewById(R.id.goalsTimelineChart)
        categoryGoalsChart = view.findViewById(R.id.categoryGoalsChart)

        pieChart = view.findViewById(R.id.incomeExpensePieChart)
        incomeByCategoryChart = view.findViewById(R.id.incomeByCategoryChart)
        expenseByCategoryChart = view.findViewById(R.id.expenseByCategoryChart)
        barChart = view.findViewById(R.id.categoryBarChart)

        graphHelper = GraphHelper(requireContext())
        val startDateInput = view.findViewById<EditText>(R.id.startDateInput)
        val endDateInput = view.findViewById<EditText>(R.id.endDateInput)
        val applyButton = view.findViewById<Button>(R.id.applyDateFilterButton)

        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        startDateInput.setOnClickListener {
            showDatePicker { date ->
                customStartDate = date
                startDateInput.setText(dateFormat.format(date))
            }
        }

        endDateInput.setOnClickListener {
            showDatePicker { date ->
                customEndDate = date
                endDateInput.setText(dateFormat.format(date))
            }
        }

        applyButton.setOnClickListener {
            if (customStartDate != null && customEndDate != null) {
                loadAnalyticsWithFilter("Custom Range")
            } else {
                Toast.makeText(requireContext(), "Please select both dates", Toast.LENGTH_SHORT).show()
            }
        }

        view.findViewById<ImageView>(R.id.backButton)?.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        view.findViewById<Button>(R.id.exportPdfBtn).setOnClickListener {
            view.postDelayed({ exportAnalyticsAsPdf() }, 500)
        }


        return view
    }

    private fun showDateRangePicker() {
        val today = Calendar.getInstance()

        val startListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
            val startCal = Calendar.getInstance().apply {
                set(year, month, day, 0, 0, 0)
            }
            customStartDate = startCal.time

            val endListener = DatePickerDialog.OnDateSetListener { _, endYear, endMonth, endDay ->
                val endCal = Calendar.getInstance().apply {
                    set(endYear, endMonth, endDay, 23, 59, 59)
                }
                customEndDate = endCal.time
                loadAnalyticsWithFilter("Custom Range")
            }

            val datePickerEnd = DatePickerDialog(requireContext(), endListener,
                today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH)
            )
            datePickerEnd.datePicker.minDate = startCal.timeInMillis
            datePickerEnd.show()
        }

        val datePickerStart = DatePickerDialog(requireContext(), startListener,
            today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH)
        )
        datePickerStart.datePicker.maxDate = today.timeInMillis
        datePickerStart.show()
    }

    private fun getUserIdFromPrefs(): String {
        val prefs = requireContext().getSharedPreferences("thryft_session", Context.MODE_PRIVATE)
        return prefs.getString("user_id", "") ?: ""
    }


    private fun showDatePicker(isEndDate: Boolean = false, onDateSelected: (Date) -> Unit) {
        val now = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, year, month, day ->
            val cal = Calendar.getInstance().apply {
                if (isEndDate) {
                    set(year, month, day, 23, 59, 59)
                } else {
                    set(year, month, day, 0, 0, 0)
                }
            }
            onDateSelected(cal.time)
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show()
    }


    private fun loadAnalyticsWithFilter(filter: String) {
        val userId = getUserIdFromPrefs()
        if (userId.isEmpty()) return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val firestore = FirebaseFirestore.getInstance()

                val txnSnapshot = firestore.collection("transactions")
                    .whereEqualTo("userId", userId)
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

                val filteredTxns = transactions.filter {
                    val date = it.date
                    (customStartDate == null || !date.before(customStartDate)) &&
                            (customEndDate == null || !date.after(customEndDate))
                }

                val incomeTxns = filteredTxns.filter { it.type.equals("INCOME", true) }
                val expenseTxns = filteredTxns.filter { it.type.equals("EXPENSE", true) }

                val incomeTotal = incomeTxns.sumOf { it.amount }
                val expenseTotal = expenseTxns.sumOf { it.amount }

                withContext(Dispatchers.Main) {
                    showPieChart(incomeTotal, expenseTotal)
                    showBarChart(incomeTotal, expenseTotal)

                    populateInsights(expenseTxns, incomeTxns, categories)

                    graphHelper.setupPieChartByCategory(
                        incomeByCategoryChart,
                        incomeTxns,
                        categories
                    )
                    graphHelper.setupPieChartByCategory(
                        expenseByCategoryChart,
                        expenseTxns,
                        categories
                    )
                    graphHelper.setupGoalsTimelineChart(
                        chart = goalsTimelineChart,
                        transactions = filteredTxns,
                        categories = categories
                    )
                    graphHelper.setupCategorySpendingWithGoalsChart(
                        categoryGoalsChart,
                        filteredTxns,
                        categories
                    )
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Failed to load analytics: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    fun setupGoalsTimelineChart(chart: LineChart, transactions: List<Transaction>, categories: List<Category>) {
        val goalMap = categories.associateBy { it.id }

        // Group by month (e.g., "2024-06")
        val dateFormatter = java.text.SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val groupedActuals = transactions.groupBy {
            dateFormatter.format(it.date)
        }.mapValues { entry ->
            entry.value.sumOf { it.amount }
        }

        val groupedGoals = transactions.groupBy {
            dateFormatter.format(it.date)
        }.mapValues { entry ->
            entry.value.sumOf { txn ->
                val cat = goalMap[txn.categoryId]
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

    private fun showPieChart(income: Double, expense: Double) {
        if (income == 0.0 && expense == 0.0) {
            pieChart.clear()
            pieChart.setNoDataText("No income or expense data available")
            return
        }

        val entries = mutableListOf<PieEntry>()
        if (income > 0) entries.add(PieEntry(income.toFloat(), "Income"))
        if (expense > 0) entries.add(PieEntry(expense.toFloat(), "Expense"))

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = listOf(Color.GREEN, Color.RED)
        dataSet.valueTextSize = 14f
        dataSet.sliceSpace = 3f
        dataSet.valueTextColor = Color.BLACK
        dataSet.xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        dataSet.yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        dataSet.valueLinePart1Length = 0.6f
        dataSet.valueLinePart2Length = 0.4f
        dataSet.valueLineColor = Color.DKGRAY

        pieChart.data = PieData(dataSet)
        pieChart.setUsePercentValues(true)
        pieChart.setEntryLabelColor(Color.TRANSPARENT)
        pieChart.setExtraOffsets(10f, 20f, 10f, 20f)
        pieChart.description.isEnabled = false
        pieChart.legend.isEnabled = true
        pieChart.animateY(1000)
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
        dataSet.colors = listOf(Color.parseColor("#4CAF50"), Color.parseColor("#F44336"))
        dataSet.valueTextSize = 14f

        val barData = BarData(dataSet).apply { barWidth = 0.4f }

        barChart.data = barData
        barChart.setFitBars(true)
        barChart.description.isEnabled = false

        barChart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(listOf("Income", "Expense"))
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            textSize = 12f
            setDrawGridLines(false)
        }

        barChart.axisLeft.axisMinimum = 0f
        barChart.axisRight.isEnabled = false
        barChart.legend.isEnabled = true
        barChart.animateY(1000)
        barChart.invalidate()
    }

    private fun populateInsights(
        expenses: List<Transaction>,
        incomes: List<Transaction>,
        categories: List<Category>
    ) {
        val topCategoryText = requireView().findViewById<TextView>(R.id.topCategoryText)
        val highestTransactionText =
            requireView().findViewById<TextView>(R.id.highestTransactionText)
        val suggestionText = requireView().findViewById<TextView>(R.id.suggestionText)

        val categoryMap = categories.associateBy { it.id }

        if (expenses.isEmpty()) {
            topCategoryText.text = "Top Expense Category: No expenses yet"
            highestTransactionText.text = "Highest Transaction: None"
            suggestionText.text = "Tip: Start tracking to receive insights."
            return
        }

        val expenseTotals =
            expenses.groupBy { it.categoryId }.mapValues { it.value.sumOf { txn -> txn.amount } }
        val incomeTotals =
            incomes.groupBy { it.categoryId }.mapValues { it.value.sumOf { txn -> txn.amount } }

        val topExpense = expenseTotals.maxByOrNull { it.value }
        val topIncome = incomeTotals.maxByOrNull { it.value }

        val highestTxn = expenses.maxByOrNull { it.amount }

        val topExpenseCat = topExpense?.key?.let { categoryMap[it]?.name } ?: "Unknown"
        val topIncomeCat = topIncome?.key?.let { categoryMap[it]?.name } ?: "Unknown"
        val highestCat = highestTxn?.categoryId?.let { categoryMap[it]?.name } ?: "Unknown"

        topCategoryText.text = """
            Top Expense: $topExpenseCat (R${"%.2f".format(topExpense?.value ?: 0.0)})
            Top Income: $topIncomeCat (R${"%.2f".format(topIncome?.value ?: 0.0)})
        """.trimIndent()

        highestTransactionText.text = "Highest Transaction: ${highestTxn?.description ?: "N/A"} " +
                "(R${"%.2f".format(highestTxn?.amount ?: 0.0)}, Category: $highestCat)"

        val budgetWarnings = mutableListOf<String>()

        expenseTotals.forEach { (id, spent) ->
            val cat = categoryMap[id] ?: return@forEach
            if (cat.maxBudget > 0 && spent > cat.maxBudget) {
                val over = spent - cat.maxBudget
                budgetWarnings.add("⚠️ ${cat.name}: Over budget by R${"%.2f".format(over)}")
            }
        }

        incomeTotals.forEach { (id, received) ->
            val cat = categoryMap[id] ?: return@forEach
            if (cat.minBudget > 0 && received < cat.minBudget) {
                val under = cat.minBudget - received
                budgetWarnings.add("💡 ${cat.name}: Income below goal by R${"%.2f".format(under)}")
            }
        }

        suggestionText.text = if (budgetWarnings.isNotEmpty()) {
            budgetWarnings.joinToString("\n")
        } else {
            "🎯 You're within your budgets. Keep it up!"
        }
    }

    private fun exportAnalyticsAsPdf() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(permission), 1002)
                return
            }
        }

        try {
            val charts = listOf(
                pieChart,
                incomeByCategoryChart,
                expenseByCategoryChart,
                barChart,
                goalsTimelineChart,
                categoryGoalsChart
            )
            val document = PdfDocument()

            charts.forEachIndexed { index, chart ->
                val bitmap = chart.chartBitmap
                val pageInfo =
                    PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1).create()
                val page = document.startPage(pageInfo)
                page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                document.finishPage(page)
            }

            val file = File(
                requireContext().getExternalFilesDir(null),
                "analytics_${System.currentTimeMillis()}.pdf"
            )
            val out = FileOutputStream(file)
            document.writeTo(out)
            document.close()
            out.close()

            val uri = FileProvider.getUriForFile(
                requireContext(),
                "com.example.thryftapp.fileprovider",
                file
            )

            val openIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val pendingIntent = PendingIntent.getActivity(
                requireContext(), 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val channelId = "analytics_export_channel"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Analytics Export",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                val manager = requireContext().getSystemService(NotificationManager::class.java)
                manager.createNotificationChannel(channel)
            }

            val notification = NotificationCompat.Builder(requireContext(), channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Analytics Exported")
                .setContentText("Tap to open PDF.")
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            NotificationManagerCompat.from(requireContext()).notify(1001, notification)
            Toast.makeText(requireContext(), "Exported to ${file.name}", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Export failed: ${e.message}", Toast.LENGTH_LONG)
                .show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1002 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            exportAnalyticsAsPdf()
        }
    }
}