package com.example.thryftapp

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.googlematerial.GoogleMaterial
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    /**
     * Attribution:
     * Website: Class IconicsDrawable

     *  Author: mikepenz
     *  URL: https://www.javadoc.io/doc/com.mikepenz/iconics-core/2.8.5/com/mikepenz/iconics/IconicsDrawable.html
     *  Accessed on: 2025-06-07
    -        */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction)

        db = AppDatabase.getDatabase(this)
        photoUploadHelper = PhotoUploadHelper(this)

        val intentUserId = intent.getStringExtra("USER_ID")
        val prefs = getSharedPreferences("thryft_session", MODE_PRIVATE)
        val sharedUserId = prefs.getString("firebase_uid", null)
        val userId = intentUserId ?: sharedUserId

        if (userId == null) {
            Toast.makeText(this, "User ID is invalid", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        //create notification channel for android o and above
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
            Log.d("TransactionActivity", "notification channel created") //log channel creation
        }

        //request notification permission for android 13 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
                Log.d("TransactionActivity", "notification permission requested") //log permission request
            }
        }

        //bind views from layout
        val amountEditText = findViewById<EditText>(R.id.amountEditText) //bind amount input
        val descriptionEditText = findViewById<EditText>(R.id.descriptionEditText) //bind description input
        val categorySpinner = findViewById<Spinner>(R.id.categorySpinner) //bind category dropdown
        val typeSpinner = findViewById<Spinner>(R.id.typeSpinner) //bind type dropdown
        val uploadPhotoButton = findViewById<Button>(R.id.uploadPhotoButton) //bind upload button
        val clearPhotoButton = findViewById<Button>(R.id.clearPhotoButton) //bind clear photo button
        val addCategoryButton = findViewById<Button>(R.id.addCategoryButton) //bind add category button
        val photoPreviewImageView = findViewById<ImageView>(R.id.photoPreviewImageView) //bind photo preview
        val saveButton = findViewById<Button>(R.id.saveButton) //bind save button

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
                    val name = categoryNameEditText.text.toString().trim()
                    val type = categoryTypeSpinner.selectedItem.toString()
                    val minBudget = minEditText.text.toString().toDoubleOrNull() ?: 0.0
                    val maxBudget = maxEditText.text.toString().toDoubleOrNull() ?: 0.0

                    //check if category name is empty
                    if (name.isBlank()) {
                        Toast.makeText(this, "Please enter a category name", Toast.LENGTH_SHORT).show()
                        Log.d("AddCategoryDialog", "category name is blank") //log validation fail
                        return@setPositiveButton
                    }


                    val firestore = FirebaseFirestore.getInstance()
                    val categoryRef = firestore.collection("users")
                        .document(userId)
                        .collection("categories")
                        .document()

                    val categoryId = categoryRef.id

                    val categoryMap = hashMapOf(
                        "id" to categoryId,
                        "userId" to userId,
                        "name" to name,
                        "type" to type,
                        "minBudget" to minBudget,
                        "maxBudget" to maxBudget,
                        "iconId" to selectedIconId
                    )

                    categoryRef.set(categoryMap).addOnSuccessListener {
                        CoroutineScope(Dispatchers.IO).launch {
                            db.categoryDao().insertCategory(
                                Category(
                                    id = categoryId,
                                    userId = userId,
                                    name = name,
                                    type = type,
                                    minBudget = minBudget,
                                    maxBudget = maxBudget,
                                    iconId = selectedIconId
                                )
                            )
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@TransactionActivity, "Category added", Toast.LENGTH_SHORT).show()
                                loadCategories(userId, selectedType, categorySpinner)
                            }
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        //open image picker when upload button is clicked
        uploadPhotoButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            Log.d("AddTransaction", "opening image picker") //log photo picker intent
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        clearPhotoButton.setOnClickListener {
            photoUri = null
            photoPreviewImageView.setImageDrawable(null)
            photoPreviewImageView.visibility = ImageView.GONE
            Toast.makeText(this, "Photo cleared", Toast.LENGTH_SHORT).show()
        }

        //save transaction when save button is clicked
        saveButton.setOnClickListener {
            val amount = amountEditText.text.toString().toDoubleOrNull()
            val description = descriptionEditText.text.toString()
            val categoryName = categorySpinner.selectedItem?.toString()
            val type = selectedType

            Log.d("AddTransaction", "save clicked with amount=$amount, category=$categoryName, type=$type") //log input data

            if (amount == null || categoryName == null || type == null) {
                Toast.makeText(this, "Please complete all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("users")
                .document(userId)
                .collection("categories")
                .whereEqualTo("name", categoryName)
                .limit(1)
                .get()
                .addOnSuccessListener { result ->
                    if (!result.isEmpty) {
                        val doc = result.documents[0]
                        val categoryId = doc.getString("id") ?: doc.id

                        Log.d("AddTransaction", "categoryId resolved: $categoryId") //log category id

                        val transaction = Transaction(
                            userId = userId,
                            categoryId = categoryId,
                            amount = amount,
                            type = type,
                            description = description,
                            photoUri = photoUri,
                            date = Date(),
                            createdAt = Date()
                        )

                        Log.d("AddTransaction", "transaction created: $transaction") //log transaction data

                        CoroutineScope(Dispatchers.IO).launch {
                            db.transactionDao().insertTransaction(transaction)

                            withContext(Dispatchers.Main) {
                                val transactionMap = hashMapOf(
                                    "userId" to userId,
                                    "categoryId" to categoryId,
                                    "amount" to amount,
                                    "type" to type,
                                    "description" to description,
                                    "photoUri" to photoUri,
                                    "date" to transaction.date,
                                    "createdAt" to transaction.createdAt
                                )

                                firestore.collection("transactions")
                                    .add(transactionMap)
                                    .addOnSuccessListener {
                                        Toast.makeText(this@TransactionActivity, "Transaction saved", Toast.LENGTH_SHORT).show()
                                        if (prefs.getBoolean("notifications_enabled", true)) {
                                            sendTransactionNotification()
                                        }
                                        finish()
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(this@TransactionActivity, "Failed to save to Firebase: ${it.message}", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        }
                    } else {
                        Toast.makeText(this, "Category not found in Firestore", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error fetching category: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun loadCategories(userId: String, type: String?, categorySpinner: Spinner) {
        CoroutineScope(Dispatchers.IO).launch {
            Log.d("LoadCategories", "Loading categories for user: $userId, type: $type") //log input

            val categories = if (type != null) {
                db.categoryDao().getCategoriesByType(userId, type)
            } else {
                db.categoryDao().getAllCategories(userId)
            }

            Log.d("LoadCategories", "Fetched ${categories.size} categories") //log result count


            withContext(Dispatchers.Main) {
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
        Log.d("Notification", "Attempting to send transaction notification") //log start

        val builder = NotificationCompat.Builder(this, "thryft_channel")
            .setSmallIcon(R.drawable.ic_bell)
            .setContentTitle("Transaction Added")
            .setContentText("Your new transaction has been recorded.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        val notificationManager = NotificationManagerCompat.from(this)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Log.d("Notification", "Permission not granted for POST_NOTIFICATIONS") //log permission check
            return
        }

        notificationManager.notify(1001, builder.build())
        Log.d("Notification", "Notification sent successfully") //log success
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data?.data != null) {
            val uri = data.data
            photoUri = uri?.let { photoUploadHelper.savePhoto(it) }
            if (photoUri != null) {
                val photoFile = File(photoUri)
                val displayUri = FileProvider.getUriForFile(
                    this,
                    "$packageName.fileprovider",
                    photoFile
                )
                findViewById<ImageView>(R.id.photoPreviewImageView).apply {
                    setImageURI(displayUri)
                    visibility = ImageView.VISIBLE
                }
                Toast.makeText(this, "Photo uploaded successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to upload photo", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
