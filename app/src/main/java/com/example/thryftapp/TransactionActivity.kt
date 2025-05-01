package com.example.thryftapp

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.googlematerial.GoogleMaterial
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
    private var selectedIconId: String = "gmd_home"
    private lateinit var iconPreviewImage: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction)

        db = AppDatabase.getDatabase(this)
        photoUploadHelper = PhotoUploadHelper(this)

        val intentUserId = intent.getIntExtra("USER_ID", -1)
        val prefs = getSharedPreferences("thryft_session", MODE_PRIVATE)
        val sharedUserId = prefs.getInt("user_id", -1)
        val userId = if (intentUserId != -1) intentUserId else sharedUserId

        if (userId == -1) {
            Toast.makeText(this, "User ID is invalid", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "thryft_channel",
                "Thryft Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Channel for transaction notifications"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
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
                loadCategories(userId, selectedType, categorySpinner)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedType = null
            }
        }

        supportFragmentManager.setFragmentResultListener("icon_picker_result", this) { _, bundle ->
            val iconId = bundle.getString("selected_icon_id")
            if (iconId != null) {
                selectedIconId = iconId
                iconPreviewImage.setImageDrawable(
                    IconicsDrawable(this, GoogleMaterial.Icon.valueOf(iconId))
                )
            }
        }

        addCategoryButton.setOnClickListener {
            val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_category, null)
            val categoryNameEditText = dialogView.findViewById<EditText>(R.id.categoryNameEditText)
            val categoryTypeSpinner = dialogView.findViewById<Spinner>(R.id.categoryTypeSpinner)
            val minEditText = dialogView.findViewById<EditText>(R.id.minBudgetEditText)
            val maxEditText = dialogView.findViewById<EditText>(R.id.maxBudgetEditText)
            iconPreviewImage = dialogView.findViewById(R.id.iconPreviewImage)

            selectedIconId = "gmd_home"
            iconPreviewImage.setImageDrawable(
                IconicsDrawable(this, GoogleMaterial.Icon.valueOf(selectedIconId))
            )

            iconPreviewImage.setOnClickListener {
                IconPickerDialogFragment().show(supportFragmentManager, "iconPicker")
            }

            ArrayAdapter.createFromResource(
                this,
                R.array.transaction_types,
                android.R.layout.simple_spinner_item
            ).also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                categoryTypeSpinner.adapter = adapter
            }

            selectedType?.let {
                val index = resources.getStringArray(R.array.transaction_types).indexOf(it)
                if (index >= 0) categoryTypeSpinner.setSelection(index)
            }

            AlertDialog.Builder(this)
                .setTitle("Add Category")
                .setView(dialogView)
                .setPositiveButton("Add") { _, _ ->
                    val name = categoryNameEditText.text.toString()
                    val type = categoryTypeSpinner.selectedItem.toString()
                    val minBudget = minEditText.text.toString().toDoubleOrNull() ?: 0.0
                    val maxBudget = maxEditText.text.toString().toDoubleOrNull() ?: 0.0

                    if (name.isBlank()) {
                        Toast.makeText(this, "Please enter a category name", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    CoroutineScope(Dispatchers.IO).launch {
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

            if (amount == null || categoryName == null || type == null) {
                Toast.makeText(this, "Please complete all fields", Toast.LENGTH_SHORT).show()
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
                    if (getSharedPreferences("thryft_session", MODE_PRIVATE).getBoolean("notifications_enabled", true)) {
                        sendTransactionNotification()
                    }
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
            }
        }
    }

    private fun sendTransactionNotification() {
        val builder = NotificationCompat.Builder(this, "thryft_channel")
            .setSmallIcon(R.drawable.ic_bell)
            .setContentTitle("Transaction Added")
            .setContentText("Your new transaction has been recorded.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        val notificationManager = NotificationManagerCompat.from(this)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        notificationManager.notify(1001, builder.build())
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
