package com.example.thryftapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.mikephil.charting.charts.PieChart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ExpensePieChartActivity : AppCompatActivity() {
    private lateinit var db: AppDatabase
    private lateinit var graphHelper: GraphHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pie_chart)

        db = AppDatabase.getDatabase(this)
        graphHelper = GraphHelper(this)

        val prefs = getSharedPreferences("thryft_session", MODE_PRIVATE)
        val userId = prefs.getInt("user_id", -1)

        if (userId == -1) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val pieChart = findViewById<PieChart>(R.id.pieChart)


        CoroutineScope(Dispatchers.IO).launch {
            val transactions = db.transactionDao().getAllTransactions(userId)
            pieChart.setUsePercentValues(true) // ✅ Show percentages
            val categories = db.categoryDao().getAllCategories(userId)
            val incomeTransactions = transactions.filter { it.type == "EXPENSE" }
            runOnUiThread {
                graphHelper.setupPieChartByCategory(pieChart, incomeTransactions, categories)
            }
        }
    }
}