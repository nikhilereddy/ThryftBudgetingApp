package com.example.thryftapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.BarChart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GraphActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase //database reference
    private lateinit var graphHelper: GraphHelper //helper for drawing charts

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_graph) //set layout

        db = AppDatabase.getDatabase(this) //init db
        graphHelper = GraphHelper(this) //init chart helper

        //retrieve user id from session
        val prefs = getSharedPreferences("thryft_session", MODE_PRIVATE)
        val userId = prefs.getInt("user_id", -1)

        if (userId == -1) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val barChart = findViewById<BarChart>(R.id.barChart) //find bar chart view

        //fetch transactions and update chart
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val transactions = db.transactionDao().getAllTransactions(userId) //get all transactions

                //if there are no transactions
                if (transactions.isEmpty()) {
                    runOnUiThread {
                        Toast.makeText(this@GraphActivity, "No transactions available", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    runOnUiThread {
                        graphHelper.setupExpenseVsIncomeChart(barChart, transactions) //draw chart
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@GraphActivity, "Error loading data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        findViewById<Button>(R.id.incomePieChartBtn).setOnClickListener {
            val intent = Intent(this, IncomePieChartActivity::class.java)
            startActivity(intent) //navigate to income chart
        }

        findViewById<Button>(R.id.expensePieChartBtn).setOnClickListener {
            val intent = Intent(this, ExpensePieChartActivity::class.java)
            startActivity(intent) //navigate to expense chart
        }
    }
}
