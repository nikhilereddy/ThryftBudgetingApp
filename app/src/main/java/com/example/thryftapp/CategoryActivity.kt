package com.example.thryftapp

import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.setFragmentResultListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.googlematerial.GoogleMaterial
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CategoryActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var iconPreviewImage: ImageView
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var selectedIconId: String = "gmd_home"

    //initialize category activity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category)

        //initialize database and firebase instances
        db = AppDatabase.getDatabase(this)
        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        //check if user is logged in
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Invalid User", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val userId = currentUser.uid
        Log.d("CategoryActivity", "logged in user: $userId") //log user id

        //bind ui elements
        val categoryNameEditText = findViewById<EditText>(R.id.categoryNameEditText)
        val typeSpinner = findViewById<Spinner>(R.id.categoryTypeSpinner)
        val minBudgetEditText = findViewById<EditText>(R.id.minBudgetEditText)
        val maxBudgetEditText = findViewById<EditText>(R.id.maxBudgetEditText)
        val addCategoryButton = findViewById<Button>(R.id.addCategoryButton)
        val categoryListView = findViewById<ListView>(R.id.categoryListView)
        iconPreviewImage = findViewById(R.id.iconPreviewImage)

        //set default icon preview
        updateIconPreview(selectedIconId)

        //handle icon picker result
        supportFragmentManager.setFragmentResultListener("icon_picker_result", this) { _, bundle ->
            val iconId = bundle.getString("selected_icon_id")
            if (iconId != null) {
                selectedIconId = iconId
                updateIconPreview(iconId)
                Log.d("CategoryActivity", "icon selected: $iconId") //log icon selection
            }
        }

        //open icon picker on click
        iconPreviewImage.setOnClickListener {
            IconPickerDialogFragment().show(supportFragmentManager, "iconPicker")
            Log.d("CategoryActivity", "icon picker opened") //log icon picker click
        }

        //setup spinner for category type
        ArrayAdapter.createFromResource(
            this,
            R.array.transaction_types,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            typeSpinner.adapter = adapter
            Log.d("CategoryActivity", "spinner populated") //log spinner setup
        }

        fun loadCategories() {
            firestore.collection("users")
                .document(userId)
                .collection("categories")
                .get()
                .addOnSuccessListener { result ->
                    val list = result.map {
                        val name = it.getString("name") ?: "Unnamed"
                        val type = it.getString("type") ?: "N/A"
                        val min = it.getDouble("minBudget") ?: 0.0
                        val max = it.getDouble("maxBudget") ?: 0.0
                        "$name ($type) - Min: $min, Max: $max"
                    }
                    categoryListView.adapter = ArrayAdapter(
                        this@CategoryActivity,
                        android.R.layout.simple_list_item_1,
                        list
                    )
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to load categories", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this, "Min budget cannot be greater than max", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            //Create Firestore document with generated ID
            val categoryRef = firestore.collection("users")
                .document(userId)
                .collection("categories")
                .document() // generate unique ID

            val categoryId = categoryRef.id

            val categoryMap = hashMapOf(
                "id" to categoryId,
                "name" to name,
                "type" to type,
                "minBudget" to minBudget,
                "maxBudget" to maxBudget,
                "iconId" to selectedIconId,
                "userId" to userId,
                "timestamp" to System.currentTimeMillis()
            )

            //Save to Firestore
            categoryRef.set(categoryMap)
                .addOnSuccessListener {
                    Toast.makeText(this, "Category added", Toast.LENGTH_SHORT).show()
                    categoryNameEditText.text.clear()
                    minBudgetEditText.text.clear()
                    maxBudgetEditText.text.clear()
                    selectedIconId = "gmd_home"
                    updateIconPreview(selectedIconId)
                    loadCategories()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to save category", Toast.LENGTH_SHORT).show()
                }

            // 💾 Save to Room (with same ID)
            CoroutineScope(Dispatchers.IO).launch {
                db.categoryDao().insertCategory(
                    Category(
                        id = categoryId, // <- ensure same ID
                        userId = userId,
                        name = name,
                        type = type,
                        minBudget = minBudget,
                        maxBudget = maxBudget,
                        iconId = selectedIconId
                    )
                )
            }
        }
    }


        private fun updateIconPreview(iconId: String) {
        iconPreviewImage.setImageDrawable(
            IconicsDrawable(this, GoogleMaterial.Icon.valueOf(iconId))
        )
    }
}
