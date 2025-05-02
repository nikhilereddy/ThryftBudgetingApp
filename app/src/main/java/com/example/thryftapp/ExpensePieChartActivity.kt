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
    private lateinit var db: AppDatabase //database reference
    private lateinit var graphHelper: GraphHelper //chart helper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pie_chart) //set layout

        db = AppDatabase.getDatabase(this) //init db
        graphHelper = GraphHelper(this) //init helper

        val prefs = getSharedPreferences("thryft_session", MODE_PRIVATE) //get prefs
        val userId = prefs.getInt("user_id", -1) //get user id

        if (userId == -1) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val pieChart = findViewById<PieChart>(R.id.pieChart) //find pie chart view

        CoroutineScope(Dispatchers.IO).launch {
            val transactions = db.transactionDao().getAllTransactions(userId) //get transactions
            pieChart.setUsePercentValues(true) //show percentages
            val categories = db.categoryDao().getAllCategories(userId) //get categories
            val incomeTransactions = transactions.filter { it.type == "EXPENSE" } //filter expenses
            runOnUiThread {
                graphHelper.setupPieChartByCategory(pieChart, incomeTransactions, categories) //render chart
            }
        }
    }
}
