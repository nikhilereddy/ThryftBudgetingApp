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

    private lateinit var pieChart: PieChart
    private lateinit var incomeByCategoryChart: PieChart
    private lateinit var expenseByCategoryChart: PieChart
    private lateinit var barChart: BarChart
    private lateinit var db: AppDatabase
    private lateinit var graphHelper: GraphHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_analytics, container, false)

        pieChart = view.findViewById(R.id.incomeExpensePieChart)
        incomeByCategoryChart = view.findViewById(R.id.incomeByCategoryChart)
        expenseByCategoryChart = view.findViewById(R.id.expenseByCategoryChart)
        barChart = view.findViewById(R.id.categoryBarChart)

        db = AppDatabase.getDatabase(requireContext())
        graphHelper = GraphHelper(requireContext())

        view.findViewById<Button>(R.id.exportPdfBtn).setOnClickListener {
            view.postDelayed({
                exportAnalyticsAsPdf()
            }, 500) // Ensures layout is complete before bitmap capture
        }
        view.findViewById<ImageView>(R.id.backButton)?.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        loadAnalytics()

        return view
    }

    private fun loadAnalytics() {
        val userId = getUserIdFromPrefs()
        if (userId == -1) return

        lifecycleScope.launch(Dispatchers.IO) {
            val transactions = db.transactionDao().getAllTransactions(userId)
            val categories = db.categoryDao().getAllCategories(userId)

            val incomeTransactions = transactions.filter { it.type.equals("INCOME", true) }
            val expenseTransactions = transactions.filter { it.type.equals("EXPENSE", true) }

            val incomeTotal = incomeTransactions.sumOf { it.amount }
            val expenseTotal = expenseTransactions.sumOf { it.amount }

            withContext(Dispatchers.Main) {
                showPieChart(incomeTotal, expenseTotal)
                showBarChart(incomeTotal, expenseTotal)

                populateInsights(expenseTransactions, incomeTransactions, categories)

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
            pieChart.setNoDataText("No income or expense data available")
            return
        }

        val entries = mutableListOf<PieEntry>()
        if (income > 0.0) entries.add(PieEntry(income.toFloat(), "Income"))
        if (expense > 0.0) entries.add(PieEntry(expense.toFloat(), "Expense"))

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

        val data = PieData(dataSet)

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
            barChart.setNoDataText("No income or expense data available")
            return
        }

        val entries = listOf(
            BarEntry(0f, income.toFloat()),
            BarEntry(1f, expense.toFloat())
        )

        val dataSet = BarDataSet(entries, "Income vs Expense")
        dataSet.colors = listOf(
            Color.parseColor("#4CAF50"),
            Color.parseColor("#F44336")
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

    private fun populateInsights(
        expenses: List<Transaction>,
        incomes: List<Transaction>,
        categories: List<Category>
    ) {
        val topCategoryView = requireView().findViewById<TextView>(R.id.topCategoryText)
        val highestTxnView = requireView().findViewById<TextView>(R.id.highestTransactionText)
        val suggestionView = requireView().findViewById<TextView>(R.id.suggestionText)

        val categoryMap = categories.associateBy { it.id }

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

        val topExpense = groupedExpenses.maxByOrNull { it.value }
        val topExpenseCategoryName = topExpense?.key?.let { categoryMap[it]?.name } ?: "Unknown"
        val topExpenseAmount = topExpense?.value ?: 0.0

        val topIncome = groupedIncomes.maxByOrNull { it.value }
        val topIncomeCategoryName = topIncome?.key?.let { categoryMap[it]?.name } ?: "Unknown"
        val topIncomeAmount = topIncome?.value ?: 0.0

        val highestTxn = expenses.maxByOrNull { it.amount }
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
                requestPermissions(arrayOf(permission), 1002)
                return
            }
        }

        try {
            val pdfDocument = PdfDocument()
            val charts = listOf(pieChart, incomeByCategoryChart, expenseByCategoryChart, barChart)

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
                "com.example.thryftapp.fileprovider", // ✅ Match Manifest
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
            exportAnalyticsAsPdf()
        }
    }
}
