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
import java.text.SimpleDateFormat

class ViewCategoryTransactionsActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private val formatter = DecimalFormat("#,##0.00") //formatter for amount

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_category_transactions)

        val categoryId = intent.getIntExtra("category_id", -1) //get category id from intent
        val prefs = getSharedPreferences("thryft_session", MODE_PRIVATE)
        val userId = prefs.getInt("user_id", -1) //get user id from session

        val header = findViewById<TextView>(R.id.categoryHeader)
        val listView = findViewById<ListView>(R.id.transactionListView)

        //validate ids
        if (categoryId == -1 || userId == -1) {
            Toast.makeText(this, "Invalid user or category", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        db = AppDatabase.getDatabase(this) //initialize db

        lifecycleScope.launch(Dispatchers.IO) {
            val category = db.categoryDao().getCategoryById(categoryId) //fetch category
            val transactions = db.transactionDao().getTransactionsByCategory(categoryId, userId) //fetch related transactions

            launch(Dispatchers.Main) {
                //set header
                if (category != null) {
                    header.text = "Transactions for ${category.name.toUpperCase()}"
                }

                //check if empty
                if (transactions.isEmpty()) {
                    listView.adapter = ArrayAdapter(
                        this@ViewCategoryTransactionsActivity,
                        android.R.layout.simple_list_item_1,
                        listOf("No transactions available")
                    )
                } else {
                    //map data into readable lines
                    val items = transactions.map {
                        " Description: ${it.description} \n Amt: R${formatter.format(it.amount)} \n Date:${SimpleDateFormat("dd MMM yyyy").format(it.date)} \n"
                    }

                    //bind to listview
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
