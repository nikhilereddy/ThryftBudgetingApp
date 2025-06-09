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

// back button to return to the previous screen
        val backButton = view.findViewById<ImageView>(R.id.backButton)

// input for transaction amount
        val amountEditText = view.findViewById<EditText>(R.id.amountEditText)

// radio group for selecting transaction type (income/expense)
        val typeRadioGroup = view.findViewById<RadioGroup>(R.id.typeRadioGroup)

// spinner to select category for the transaction
        val categorySpinner = view.findViewById<Spinner>(R.id.categorySpinner)

// checkbox to toggle the photo upload section
        val photoUploadCheckBox = view.findViewById<CheckBox>(R.id.photoUploadCheckBox)

// container layout that holds the photo upload section
        val photoUploadSection = view.findViewById<LinearLayout>(R.id.photoUploadSection)

// button to pick a photo from gallery
        val uploadPhotoButton = view.findViewById<Button>(R.id.uploadPhotoButton)

// button to clear the uploaded photo
        val clearPhotoButton = view.findViewById<Button>(R.id.clearPhotoButton)

// button to add a new category
        val addCategoryButton = view.findViewById<Button>(R.id.addCategoryButton)

// image view to preview the uploaded photo
        val photoPreviewImageView = view.findViewById<ImageView>(R.id.photoPreviewImageView)

// button to save the transaction
        val saveButton = view.findViewById<Button>(R.id.saveButton)

// input for entering transaction description
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
        //get categories for user and selected type
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .collection("categories")
            .whereEqualTo("type", type) //filter by income/expense
            .get()
            .addOnSuccessListener { result ->
                //extract category names
                val categories = result.documents.mapNotNull { doc ->
                    doc.getString("name") //get name field
                }

                //set spinner adapter with category names
                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    categories
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) //set dropdown layout
                categorySpinner.adapter = adapter //apply adapter to spinner
            }
            .addOnFailureListener {
                //show error if categories can't load
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
        /**
         * Attribution:
         * Website: How to Implement the Android Photo Picker in Your App

         *  Author: Emran Khandaker Evan
         *  URL:https://blog.evanemran.info/how-to-implement-the-android-photo-picker-in-your-app
         *  Accessed on: 2025-06-07
        -        */
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK && photoUri != null) {
            val file = File(photoUri!!) //get file from uri
            val displayUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider", //define authority
                file
            )

            preview?.apply {
                setImageURI(displayUri) //show image in preview
                visibility = ImageView.VISIBLE //make image view visible
            }

            Toast.makeText(requireContext(), "Photo taken", Toast.LENGTH_SHORT).show() //notify user
        }


    }
}
