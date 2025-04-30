package com.example.thryftapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

class TransactionActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var photoUploadHelper: PhotoUploadHelper
    private var photoUri: String? = null
    private val PICK_IMAGE_REQUEST = 1
    private var selectedType: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction)

        db = AppDatabase.getDatabase(this)
        photoUploadHelper = PhotoUploadHelper(this)

        // Get user ID from Intent or SharedPreferences
        val intentUserId = intent.getIntExtra("USER_ID", -1)
        val prefs = getSharedPreferences("thryft_session", MODE_PRIVATE)
        val sharedUserId = prefs.getInt("user_id", -1)
        val userId = if (intentUserId != -1) intentUserId else sharedUserId

        if (userId == -1) {
            Toast.makeText(this, "User ID is invalid", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val amountEditText = findViewById<EditText>(R.id.amountEditText)
        val descriptionEditText = findViewById<EditText>(R.id.descriptionEditText)
        val categorySpinner = findViewById<Spinner>(R.id.categorySpinner)
        val typeSpinner = findViewById<Spinner>(R.id.typeSpinner)
        val uploadPhotoButton = findViewById<Button>(R.id.uploadPhotoButton)
        val clearPhotoButton = findViewById<Button>(R.id.clearPhotoButton)
        val addCategoryButton = findViewById<Button>(R.id.addCategoryButton)
        val photoPreviewImageView = findViewById<ImageView>(R.id.photoPreviewImageView)
        val saveButton = findViewById<Button>(R.id.saveButton)

        // Setup transaction type spinner
        ArrayAdapter.createFromResource(
            this,
            R.array.transaction_types,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            typeSpinner.adapter = adapter
        }

        typeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                selectedType = parent.getItemAtPosition(position).toString()
                Log.d("TransactionActivity", "Selected type: $selectedType")
                loadCategories(userId, selectedType, categorySpinner)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedType = null
            }
        }

        addCategoryButton.setOnClickListener {
            val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_category, null)
            val categoryNameEditText = dialogView.findViewById<EditText>(R.id.categoryNameEditText)
            val categoryTypeSpinner = dialogView.findViewById<Spinner>(R.id.categoryTypeSpinner)

            ArrayAdapter.createFromResource(
                this,
                R.array.transaction_types,
                android.R.layout.simple_spinner_item
            ).also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                categoryTypeSpinner.adapter = adapter
            }

            selectedType?.let {
                val typeIndex = resources.getStringArray(R.array.transaction_types).indexOf(it)
                if (typeIndex >= 0) categoryTypeSpinner.setSelection(typeIndex)
            }

            AlertDialog.Builder(this)
                .setTitle("Add Category")
                .setView(dialogView)
                .setPositiveButton("Add") { _, _ ->
                    val name = categoryNameEditText.text.toString()
                    val type = categoryTypeSpinner.selectedItem.toString()
                    if (name.isBlank()) {
                        Toast.makeText(this, "Please enter a category name", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    CoroutineScope(Dispatchers.IO).launch {
                        db.categoryDao().insertCategory(Category(userId = userId, name = name, type = type))
                        runOnUiThread {
                            Toast.makeText(this@TransactionActivity, "Category added", Toast.LENGTH_SHORT).show()
                            loadCategories(userId, selectedType, categorySpinner)
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        uploadPhotoButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        clearPhotoButton.setOnClickListener {
            photoUri = null
            photoPreviewImageView.setImageDrawable(null)
            photoPreviewImageView.visibility = ImageView.GONE
            Toast.makeText(this, "Photo cleared", Toast.LENGTH_SHORT).show()
        }

        saveButton.setOnClickListener {
            val amount = amountEditText.text.toString().toDoubleOrNull()
            val description = descriptionEditText.text.toString()
            val categoryName = categorySpinner.selectedItem?.toString()
            val type = selectedType

            if (amount == null) {
                Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (categoryName == null) {
                Toast.makeText(this, "Please select a category or add a new one", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (type == null) {
                Toast.makeText(this, "Please select a transaction type", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            CoroutineScope(Dispatchers.IO).launch {
                val category = db.categoryDao().getCategoryByName(userId, categoryName)
                val transaction = Transaction(
                    userId = userId,
                    categoryId = category?.id,
                    amount = amount,
                    type = type,
                    description = description,
                    photoUri = photoUri,
                    date = Date(),
                    createdAt = Date()
                )
                db.transactionDao().insertTransaction(transaction)
                runOnUiThread {
                    Toast.makeText(this@TransactionActivity, "Transaction saved successfully", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    private fun loadCategories(userId: Int, type: String?, categorySpinner: Spinner) {
        CoroutineScope(Dispatchers.IO).launch {
            val categories = if (type != null) {
                db.categoryDao().getCategoriesByType(userId, type)
            } else {
                db.categoryDao().getAllCategories(userId)
            }

            runOnUiThread {
                val categoryNames = categories.map { it.name }
                val adapter = ArrayAdapter(
                    this@TransactionActivity,
                    android.R.layout.simple_spinner_item,
                    categoryNames
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                categorySpinner.adapter = adapter

                if (categoryNames.isEmpty()) {
                    Toast.makeText(
                        this@TransactionActivity,
                        "No categories available for $type. Tap 'Add Category' to create one.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data?.data != null) {
            val uri = data.data
            photoUri = uri?.let { photoUploadHelper.savePhoto(it) }
            if (photoUri != null) {
                val photoFile = File(photoUri)
                val photoUriForDisplay = FileProvider.getUriForFile(
                    this,
                    "$packageName.fileprovider",
                    photoFile
                )
                findViewById<ImageView>(R.id.photoPreviewImageView).apply {
                    setImageURI(photoUriForDisplay)
                    visibility = ImageView.VISIBLE
                }
                Toast.makeText(this, "Photo uploaded successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to upload photo", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

