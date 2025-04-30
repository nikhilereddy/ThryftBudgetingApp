package com.example.thryftapp

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CategoryActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category)

        db = AppDatabase.getDatabase(this)
        val prefs = getSharedPreferences("thryft_session", MODE_PRIVATE)
        val userId = prefs.getInt("user_id", -1)

        if (userId == -1) {
            Toast.makeText(this, "Invalid User ID", Toast.LENGTH_SHORT).show()
            finish() // or redirect to login
            return
        }

        val categoryNameEditText = findViewById<EditText>(R.id.categoryNameEditText)
        val typeSpinner = findViewById<Spinner>(R.id.categoryTypeSpinner)
        val addCategoryButton = findViewById<Button>(R.id.addCategoryButton)
        val categoryListView = findViewById<ListView>(R.id.categoryListView)

        // Setup type spinner
        ArrayAdapter.createFromResource(
            this,
            R.array.transaction_types,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            typeSpinner.adapter = adapter
        }

        // Load categories into the list view
        fun loadCategories() {
            CoroutineScope(Dispatchers.IO).launch {
                val categories = db.categoryDao().getAllCategories(userId)
                runOnUiThread {
                    val adapter = ArrayAdapter(
                        this@CategoryActivity,
                        android.R.layout.simple_list_item_1,
                        categories.map { "${it.name} (${it.type})" }
                    )
                    categoryListView.adapter = adapter
                }
            }
        }

        loadCategories()

        // Handle add category button click
        addCategoryButton.setOnClickListener {
            val name = categoryNameEditText.text.toString()
            val type = typeSpinner.selectedItem.toString()

            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter a category name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Ensure user exists in the database before inserting category
            CoroutineScope(Dispatchers.IO).launch {
                val user = db.userDao().getUserById(userId)
                if (user != null) {
                    // Insert category if user exists
                    db.categoryDao().insertCategory(Category(userId = userId, name = name, type = type))
                    runOnUiThread {
                        Toast.makeText(this@CategoryActivity, "Category added", Toast.LENGTH_SHORT).show()
                        categoryNameEditText.text.clear()
                        loadCategories()
                    }
                } else {
                    // Show error if user does not exist
                    runOnUiThread {
                        Toast.makeText(this@CategoryActivity, "User not found", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
