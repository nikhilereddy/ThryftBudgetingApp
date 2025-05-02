package com.example.thryftapp

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.googlematerial.GoogleMaterial
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

class TransactionDetailsFragment : Fragment() {

    private lateinit var db: AppDatabase
    private lateinit var photoUploadHelper: PhotoUploadHelper
    private var photoUri: String? = null
    private val PICK_IMAGE_REQUEST = 1
    private var selectedType: String? = null
    private var selectedIconId: String = "gmd_home"
    private lateinit var iconPreviewImage: ImageView
    private val TAG = "TransactionDetails"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_transaction_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = AppDatabase.getDatabase(requireContext())
        photoUploadHelper = PhotoUploadHelper(requireContext())

        // Access SharedPreferences using requireContext() to ensure fragment is attached
        val prefs = requireContext().getSharedPreferences("thryft_session", MODE_PRIVATE)
        val userId = prefs.getInt("user_id", -1)

        if (userId == -1) {
            Toast.makeText(context, "User ID is invalid", Toast.LENGTH_SHORT).show()
            return
        }

        // Notification Channel Setup
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "thryft_channel",
                "Thryft Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Channel for transaction notifications"
            }
            val notificationManager = requireContext().getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        // Set up views
        val backButton = view.findViewById<ImageView>(R.id.backButton)
        val amountEditText = view.findViewById<EditText>(R.id.amountEditText)
        val typeRadioGroup = view.findViewById<RadioGroup>(R.id.typeRadioGroup)
        val categorySpinner = view.findViewById<Spinner>(R.id.categorySpinner)
        val photoUploadCheckBox = view.findViewById<CheckBox>(R.id.photoUploadCheckBox)
        val photoUploadSection = view.findViewById<LinearLayout>(R.id.photoUploadSection)
        val uploadPhotoButton = view.findViewById<Button>(R.id.uploadPhotoButton)
        val clearPhotoButton = view.findViewById<Button>(R.id.clearPhotoButton)
        val addCategoryButton = view.findViewById<Button>(R.id.addCategoryButton)
        val photoPreviewImageView = view.findViewById<ImageView>(R.id.photoPreviewImageView)
        val saveButton = view.findViewById<Button>(R.id.saveButton)
        val description = view.findViewById<EditText>(R.id.descriptionEditText)

        // Back button listener
        backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Transaction type radio group listener
        typeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            selectedType = when (checkedId) {
                R.id.incomeRadio -> "INCOME"
                R.id.expenseRadio -> "EXPENSE"
                else -> null
            }
            Log.d(TAG, "Selected type: $selectedType")
            loadCategories(userId, selectedType, categorySpinner)
        }

        // Set default selection to Expense
        typeRadioGroup.check(R.id.expenseRadio)

        // Photo upload checkbox listener
        photoUploadCheckBox.setOnCheckedChangeListener { _, isChecked ->
            photoUploadSection.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // Icon picker result listener
        parentFragmentManager.setFragmentResultListener("icon_picker_result", viewLifecycleOwner) { _, bundle ->
            val iconId = bundle.getString("selected_icon_id")
            if (iconId != null) {
                selectedIconId = iconId
                iconPreviewImage.setImageDrawable(
                    IconicsDrawable(requireContext(), GoogleMaterial.Icon.valueOf(iconId))
                )
            }
        }

        // Add category button listener
        addCategoryButton.setOnClickListener {
            val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_category, null)
            val categoryNameEditText = dialogView.findViewById<EditText>(R.id.categoryNameEditText)
            val categoryTypeSpinner = dialogView.findViewById<Spinner>(R.id.categoryTypeSpinner)
            val minEditText = dialogView.findViewById<EditText>(R.id.minBudgetEditText)
            val maxEditText = dialogView.findViewById<EditText>(R.id.maxBudgetEditText)
            iconPreviewImage = dialogView.findViewById(R.id.iconPreviewImage)

            selectedIconId = "gmd_home"
            iconPreviewImage.setImageDrawable(
                IconicsDrawable(requireContext(), GoogleMaterial.Icon.valueOf(selectedIconId))
            )

            iconPreviewImage.setOnClickListener {
                IconPickerDialogFragment().show(parentFragmentManager, "iconPicker")
            }

            ArrayAdapter.createFromResource(
                requireContext(),
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

            AlertDialog.Builder(requireContext())
                .setTitle("Add Category")
                .setView(dialogView)
                .setPositiveButton("Add") { _, _ ->
                    val name = categoryNameEditText.text.toString()
                    val type = categoryTypeSpinner.selectedItem.toString()
                    val minBudget = minEditText.text.toString().toDoubleOrNull() ?: 0.0
                    val maxBudget = maxEditText.text.toString().toDoubleOrNull() ?: 0.0

                    if (name.isBlank()) {
                        Toast.makeText(requireContext(), "Please enter a category name", Toast.LENGTH_SHORT).show()
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
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "Category added", Toast.LENGTH_SHORT).show()
                            loadCategories(userId, selectedType, categorySpinner)
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Photo upload button listener
        uploadPhotoButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        // Clear photo button listener
        clearPhotoButton.setOnClickListener {
            photoUri = null
            photoPreviewImageView.setImageDrawable(null)
            photoPreviewImageView.visibility = ImageView.GONE
            Toast.makeText(requireContext(), "Photo cleared", Toast.LENGTH_SHORT).show()
        }

        // Save button listener
        saveButton.setOnClickListener {
            val amount = amountEditText.text.toString().toDoubleOrNull()
            val description = description.text.toString()
            val categoryName = categorySpinner.selectedItem?.toString()
            val type = selectedType

            if (amount == null || categoryName == null || type == null) {
                Toast.makeText(requireContext(), "Please complete all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            CoroutineScope(Dispatchers.IO).launch {
                val category = db.categoryDao().getCategoryByName(userId, categoryName)
                val transaction = Transaction(
                    userId = userId,
                    categoryId = category?.id,
                    amount = amount,
                    type = type,
                    description = description, // Description field removed from XML, setting empty
                    photoUri = photoUri,
                    date = Date(),
                    createdAt = Date()
                )

                db.transactionDao().insertTransaction(transaction)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Transaction saved successfully", Toast.LENGTH_SHORT).show()
                    if (requireContext().getSharedPreferences("thryft_session", MODE_PRIVATE).getBoolean("notifications_enabled", true)) {
                        sendTransactionNotification()
                    }
                    parentFragmentManager.popBackStack()  // Navigate back
                }
            }
        }
    }

    private fun loadCategories(userId: Int, type: String?, categorySpinner: Spinner) {
        Log.d(TAG, "Loading categories for userId: $userId, type: $type")
        CoroutineScope(Dispatchers.IO).launch {
            val categories = if (type != null) {
                db.categoryDao().getCategoriesByType(userId, type)
            } else {
                db.categoryDao().getAllCategories(userId)
            }
            Log.d(TAG, "Categories loaded: ${categories.size}")

            withContext(Dispatchers.Main) {
                val categoryNames = categories.map { it.name }
                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    categoryNames
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                categorySpinner.adapter = adapter
                adapter.notifyDataSetChanged()
                Log.d(TAG, "Spinner adapter updated with ${categoryNames.size} items")
            }
        }
    }

    private fun sendTransactionNotification() {
        val builder = NotificationCompat.Builder(requireContext(), "thryft_channel")
            .setSmallIcon(R.drawable.ic_bell)
            .setContentTitle("Transaction Added")
            .setContentText("Your new transaction has been recorded.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        val notificationManager = NotificationManagerCompat.from(requireContext())
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
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
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    photoFile
                )
                view?.findViewById<ImageView>(R.id.photoPreviewImageView)?.apply {
                    setImageURI(photoUriForDisplay)
                    visibility = ImageView.VISIBLE
                }
                Toast.makeText(requireContext(), "Photo uploaded successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Failed to upload photo", Toast.LENGTH_SHORT).show()
            }
        }
    }
}//correct version