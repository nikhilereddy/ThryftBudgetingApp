package com.example.thryftapp

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.DecimalFormat

class ViewCategoryTransactionsActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private val formatter = DecimalFormat("#,##0.00")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_category_transactions)

        val categoryId = intent.getIntExtra("category_id", -1)
        val prefs = getSharedPreferences("thryft_session", MODE_PRIVATE)
        val userId = prefs.getInt("user_id", -1)

        val header = findViewById<TextView>(R.id.categoryHeader)
        val listView = findViewById<ListView>(R.id.transactionListView)

        if (categoryId == -1 || userId == -1) {
            Toast.makeText(this, "Invalid user or category", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        db = AppDatabase.getDatabase(this)

        lifecycleScope.launch(Dispatchers.IO) {
            val category = db.categoryDao().getCategoryById(categoryId)
            val transactions = db.transactionDao().getTransactionsByCategory(categoryId, userId)

            launch(Dispatchers.Main) {
                header.text = "Transactions for ${category?.name ?: "Category"}"

                if (transactions.isEmpty()) {
                    listView.adapter = ArrayAdapter(
                        this@ViewCategoryTransactionsActivity,
                        android.R.layout.simple_list_item_1,
                        listOf("No transactions available")
                    )
                } else {
                    val items = transactions.map {
                        "${it.description}: R${formatter.format(it.amount)}"
                    }

                    listView.adapter = ArrayAdapter(
                        this@ViewCategoryTransactionsActivity,
                        android.R.layout.simple_list_item_1,
                        items
                    )
                }
            }
        }
    }
}
