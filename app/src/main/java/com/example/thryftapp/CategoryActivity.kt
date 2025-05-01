package com.example.thryftapp

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.setFragmentResultListener
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.googlematerial.GoogleMaterial
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CategoryActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var iconPreviewImage: ImageView
    private var selectedIconId: String = "gmd_home" // default

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category)

        db = AppDatabase.getDatabase(this)
        val prefs = getSharedPreferences("thryft_session", MODE_PRIVATE)
        val userId = prefs.getInt("user_id", -1)

        if (userId == -1) {
            Toast.makeText(this, "Invalid User ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val categoryNameEditText = findViewById<EditText>(R.id.categoryNameEditText)
        val typeSpinner = findViewById<Spinner>(R.id.categoryTypeSpinner)
        val minBudgetEditText = findViewById<EditText>(R.id.minBudgetEditText)
        val maxBudgetEditText = findViewById<EditText>(R.id.maxBudgetEditText)
        val addCategoryButton = findViewById<Button>(R.id.addCategoryButton)
        val categoryListView = findViewById<ListView>(R.id.categoryListView)
        iconPreviewImage = findViewById(R.id.iconPreviewImage)

        // ✅ Set initial icon
        updateIconPreview(selectedIconId)

        // ✅ Handle icon picker result
        supportFragmentManager.setFragmentResultListener("icon_picker_result", this) { _, bundle ->
            val iconId = bundle.getString("selected_icon_id")
            if (iconId != null) {
                selectedIconId = iconId
                updateIconPreview(iconId)
            }
        }

        // ✅ Launch icon picker dialog
        iconPreviewImage.setOnClickListener {
            IconPickerDialogFragment().show(supportFragmentManager, "iconPicker")
        }

        ArrayAdapter.createFromResource(
            this,
            R.array.transaction_types,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            typeSpinner.adapter = adapter
        }

        fun loadCategories() {
            CoroutineScope(Dispatchers.IO).launch {
                val categories = db.categoryDao().getAllCategories(userId)
                runOnUiThread {
                    val adapter = ArrayAdapter(
                        this@CategoryActivity,
                        android.R.layout.simple_list_item_1,
                        categories.map {
                            "${it.name} (${it.type}) - Min: ${it.minBudget}, Max: ${it.maxBudget}"
                        }
                    )
                    categoryListView.adapter = adapter
                }
            }
        }

        loadCategories()

        addCategoryButton.setOnClickListener {
            val name = categoryNameEditText.text.toString()
            val type = typeSpinner.selectedItem.toString()
            val minBudget = minBudgetEditText.text.toString().toDoubleOrNull()
            val maxBudget = maxBudgetEditText.text.toString().toDoubleOrNull()

            if (name.isEmpty() || minBudget == null || maxBudget == null) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (minBudget > maxBudget) {
                Toast.makeText(this, "Min budget cannot be greater than max", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            CoroutineScope(Dispatchers.IO).launch {
                val user = db.userDao().getUserById(userId)
                if (user != null) {
                    db.categoryDao().insertCategory(
                        Category(
                            userId = userId,
                            name = name,
                            type = type,
                            minBudget = minBudget,
                            maxBudget = maxBudget,
                            iconId = selectedIconId
                        )
                    )
                    runOnUiThread {
                        Toast.makeText(this@CategoryActivity, "Category added", Toast.LENGTH_SHORT).show()
                        categoryNameEditText.text.clear()
                        minBudgetEditText.text.clear()
                        maxBudgetEditText.text.clear()
                        selectedIconId = "gmd_home"
                        updateIconPreview(selectedIconId)
                        loadCategories()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@CategoryActivity, "User not found", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun updateIconPreview(iconId: String) {
        iconPreviewImage.setImageDrawable(
            IconicsDrawable(this, GoogleMaterial.Icon.valueOf(iconId))
        )
    }
}
