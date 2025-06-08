/*
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
}//correct version*/
*/
package com.example.thryftapp

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.googlematerial.GoogleMaterial
import kotlinx.coroutines.*
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
    private val REQUEST_CAMERA_PERMISSION = 200
    private val REQUEST_IMAGE_CAPTURE = 201


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_transaction_details, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        db = AppDatabase.getDatabase(requireContext())
        photoUploadHelper = PhotoUploadHelper(requireContext())

        val prefs = requireContext().getSharedPreferences("thryft_session", Context.MODE_PRIVATE)
        val userId = prefs.getString("user_id", null)

        if (userId.isNullOrBlank()) {
            Toast.makeText(context, "User ID is invalid", Toast.LENGTH_SHORT).show()
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
            val notificationManager =
                requireContext().getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }

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

        backButton.setOnClickListener { parentFragmentManager.popBackStack() }

        typeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            selectedType = when (checkedId) {
                R.id.incomeRadio -> "INCOME"
                R.id.expenseRadio -> "EXPENSE"
                else -> null
            }
            loadCategories(userId, selectedType, categorySpinner)
        }

        typeRadioGroup.check(R.id.expenseRadio)

        photoUploadCheckBox.setOnCheckedChangeListener { _, isChecked ->
            photoUploadSection.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        parentFragmentManager.setFragmentResultListener(
            "icon_picker_result",
            viewLifecycleOwner
        ) { _, bundle ->
            val iconId = bundle.getString("selected_icon_id")
            if (iconId != null) {
                selectedIconId = iconId
                iconPreviewImage.setImageDrawable(
                    IconicsDrawable(requireContext(), GoogleMaterial.Icon.valueOf(iconId))
                )
            }
        }

        addCategoryButton.setOnClickListener {
            val dialogView =
                LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_category, null)
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
                    val name = categoryNameEditText.text.toString().trim()
                    val type = categoryTypeSpinner.selectedItem.toString()
                    val minBudget = minEditText.text.toString().toDoubleOrNull() ?: 0.0
                    val maxBudget = maxEditText.text.toString().toDoubleOrNull() ?: 0.0

                    if (name.isBlank()) {
                        Toast.makeText(
                            requireContext(),
                            "Please enter a category name",
                            Toast.LENGTH_SHORT
                        ).show()
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
                                Toast.makeText(
                                    requireContext(),
                                    "Category added",
                                    Toast.LENGTH_SHORT
                                ).show()
                                loadCategories(userId, selectedType, categorySpinner)
                            }
                        }
                    }.addOnFailureListener {
                        Toast.makeText(
                            requireContext(),
                            "Failed to save to Firestore",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        uploadPhotoButton.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Select Image Source")
                .setItems(arrayOf("Take Photo", "Choose from Gallery")) { _, which ->
                    when (which) {
                        0 -> checkCameraPermissionAndLaunch()
                        1 -> {
                            val intent = Intent(Intent.ACTION_PICK)
                            intent.type = "image/*"
                            startActivityForResult(intent, PICK_IMAGE_REQUEST)
                        }
                    }
                }
                .show()
        }


        clearPhotoButton.setOnClickListener {
            photoUri = null
            photoPreviewImageView.setImageDrawable(null)
            photoPreviewImageView.visibility = ImageView.GONE
            Toast.makeText(requireContext(), "Photo cleared", Toast.LENGTH_SHORT).show()
        }

        saveButton.setOnClickListener {
            val amount = amountEditText.text.toString().toDoubleOrNull()
            val descriptionText = description.text.toString()
            val categoryName = categorySpinner.selectedItem?.toString()
            val type = selectedType

            if (amount == null || categoryName == null || type == null) {
                Toast.makeText(requireContext(), "Please complete all fields", Toast.LENGTH_SHORT)
                    .show()
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

                        val transaction = Transaction(
                            userId = userId,
                            categoryId = categoryId,
                            amount = amount,
                            type = type,
                            description = descriptionText,
                            photoUri = photoUri,
                            date = Date(),
                            createdAt = Date()
                        )

                        CoroutineScope(Dispatchers.IO).launch {
                            db.transactionDao().insertTransaction(transaction)

                            withContext(Dispatchers.Main) {
                                val transactionMap = hashMapOf(
                                    "userId" to userId,
                                    "categoryId" to categoryId,
                                    "amount" to amount,
                                    "type" to type,
                                    "description" to descriptionText,
                                    "photoUri" to photoUri,
                                    "date" to transaction.date,
                                    "createdAt" to transaction.createdAt
                                )

                                firestore.collection("transactions")
                                    .add(transactionMap)
                                    .addOnSuccessListener {
                                        Toast.makeText(
                                            requireContext(),
                                            "Transaction saved",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        if (prefs.getBoolean(
                                                "notifications_enabled",
                                                true
                                            )
                                        ) sendTransactionNotification()
                                        parentFragmentManager.popBackStack()
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(
                                            requireContext(),
                                            "Failed to save to Firebase: ${it.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            }
                        }
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Category not found in Firestore",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(
                        requireContext(),
                        "Error loading category: ${it.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    private fun checkCameraPermissionAndLaunch() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
        } else {
            launchCamera()
        }
    }

    private fun launchCamera() {
        val photoFile = File.createTempFile("IMG_", ".jpg", requireContext().cacheDir)
        val imageUriTemp = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            photoFile
        )
        photoUri = photoFile.absolutePath

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUriTemp)
        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
    }

    private fun loadCategories(userId: String, type: String?, categorySpinner: Spinner) {
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .collection("categories")
            .whereEqualTo("type", type)
            .get()
            .addOnSuccessListener { result ->
                val categories = result.documents.mapNotNull { doc ->
                    doc.getString("name")
                }

                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    categories
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                categorySpinner.adapter = adapter
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load categories", Toast.LENGTH_SHORT).show()
            }
    }


    private fun sendTransactionNotification() {
        val builder = NotificationCompat.Builder(requireContext(), "thryft_channel")
            .setSmallIcon(R.drawable.ic_bell)
            .setContentTitle("Transaction Added")
            .setContentText("Your new transaction has been recorded.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        val notificationManager = NotificationManagerCompat.from(requireContext())
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return
        notificationManager.notify(1001, builder.build())
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            launchCamera()
        } else {
            Toast.makeText(requireContext(), "Camera permission is required", Toast.LENGTH_SHORT)
                .show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val preview = view?.findViewById<ImageView>(R.id.photoPreviewImageView)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data?.data != null) {
            val uri = data.data
            photoUri = uri?.let { photoUploadHelper.savePhoto(it) }
            if (photoUri != null) {
                val file = File(photoUri!!)
                val displayUri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    file
                )
                preview?.apply {
                    setImageURI(displayUri)
                    visibility = ImageView.VISIBLE
                }
                Toast.makeText(requireContext(), "Photo uploaded", Toast.LENGTH_SHORT).show()
            }
        }

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK && photoUri != null) {
            val file = File(photoUri!!)
            val displayUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )
            preview?.apply {
                setImageURI(displayUri)
                visibility = ImageView.VISIBLE
            }
            Toast.makeText(requireContext(), "Photo taken", Toast.LENGTH_SHORT).show()
        }

}
}
