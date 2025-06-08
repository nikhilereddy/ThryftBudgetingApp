/*
package com.example.thryftapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.googlematerial.GoogleMaterial
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AddCategoryFragment : Fragment() {

    private lateinit var db: AppDatabase
    private var selectedIconId: String = "gmd_home" //default icon
    private lateinit var iconPreviewImage: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_add_category, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        db = AppDatabase.getDatabase(requireContext()) //initialize database
        val prefs = requireContext().getSharedPreferences("thryft_session", 0)
        val userId = prefs.getInt("user_id", -1) //get userId from shared preferences

        if (userId == -1) { //check if userId is valid
            Toast.makeText(context, "Invalid User", Toast.LENGTH_SHORT).show() //toast for invalid user
            requireActivity().supportFragmentManager.popBackStack() //return to previous screen
            return
        }

        val nameEditText = view.findViewById<EditText>(R.id.categoryNameEditText) //find category name input
        val minEditText = view.findViewById<EditText>(R.id.minBudgetEditText) //find min budget input
        val maxEditText = view.findViewById<EditText>(R.id.maxBudgetEditText) //find max budget input
        val radioGroup = view.findViewById<RadioGroup>(R.id.typeRadioGroup) //find radio group for category type
        val saveButton = view.findViewById<Button>(R.id.saveCategoryButton) //find save button
        val backButton = view.findViewById<ImageView>(R.id.backButton) //find back button
        iconPreviewImage = view.findViewById(R.id.iconPreviewImage) //find icon preview image

        // Set default icon with glow
        iconPreviewImage.setImageDrawable(
            IconicsDrawable(requireContext(), GoogleMaterial.Icon.valueOf(selectedIconId)) //set default icon
        )
        iconPreviewImage.setBackgroundResource(R.drawable.icon_glow) //apply glow effect to icon
        iconPreviewImage.setPadding(24, 24, 24, 24) //set padding for icon preview

        iconPreviewImage.setOnClickListener { //icon click listener
            IconPickerDialogFragment().show(parentFragmentManager, "iconPicker") //show icon picker dialog
        }

        parentFragmentManager.setFragmentResultListener("icon_picker_result", viewLifecycleOwner) { _, result -> //listen for icon pick result
            val iconId = result.getString("selected_icon_id") ?: "gmd_home" //get selected icon ID or default
            selectedIconId = iconId //store selected icon ID
            iconPreviewImage.setImageDrawable(
                IconicsDrawable(requireContext(), GoogleMaterial.Icon.valueOf(iconId)) //set selected icon
            )
            iconPreviewImage.setBackgroundResource(R.drawable.icon_glow) //apply glow effect
        }

        backButton.setOnClickListener { //back button click listener
            requireActivity().supportFragmentManager.popBackStack() //go back to previous fragment
        }

        saveButton.setOnClickListener { //save button click listener
            val name = nameEditText.text.toString().trim() //get category name
            val minBudget = minEditText.text.toString().toDoubleOrNull() //get min budget and convert to double
            val maxBudget = maxEditText.text.toString().toDoubleOrNull() //get max budget and convert to double
            val type = when (radioGroup.checkedRadioButtonId) { //check selected category type
                R.id.expenseRadio -> "EXPENSE" //if expense radio button is checked
                R.id.incomeRadio -> "INCOME" //if income radio button is checked
                else -> null //if no type is selected
            }

            if (name.isEmpty() || type == null || minBudget == null || maxBudget == null) { //check if all fields are filled
                Toast.makeText(context, "Fill all required fields", Toast.LENGTH_SHORT).show() //show error if fields are empty
                return@setOnClickListener
            }

            if (minBudget > maxBudget) { //check if min budget exceeds max budget
                Toast.makeText(context, "Min budget cannot exceed max", Toast.LENGTH_SHORT).show() //show error
                return@setOnClickListener
            }

            CoroutineScope(Dispatchers.IO).launch { //start background task
                db.categoryDao().insertCategory( //insert new category into database
                    Category(
                        userId = userId, //user ID
                        name = name, //category name
                        type = type, //category type
                        minBudget = minBudget, //min budget
                        maxBudget = maxBudget, //max budget
                        iconId = selectedIconId //selected icon ID
                    )
                )
                launch(Dispatchers.Main) { //switch back to main thread
                    Toast.makeText(context, "Category added!", Toast.LENGTH_SHORT).show() //show success message
                    requireActivity().supportFragmentManager.popBackStack() //go back to previous fragment
                }
            }
        }
    }
}
*/package com.example.thryftapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.googlematerial.GoogleMaterial
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AddCategoryFragment : Fragment() {

    private lateinit var db: AppDatabase
    private lateinit var firestore: FirebaseFirestore
    private var selectedIconId: String = "gmd_home"
    private lateinit var iconPreviewImage: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_add_category, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        db = AppDatabase.getDatabase(requireContext())
        firestore = FirebaseFirestore.getInstance()

        val prefs = requireContext().getSharedPreferences("thryft_session", 0)
        val firebaseUid = prefs.getString("firebase_uid", null)

        if (firebaseUid.isNullOrEmpty()) {
            Toast.makeText(context, "Invalid User", Toast.LENGTH_SHORT).show()
            requireActivity().supportFragmentManager.popBackStack()
            return
        }

        val nameEditText = view.findViewById<EditText>(R.id.categoryNameEditText)
        val minEditText = view.findViewById<EditText>(R.id.minBudgetEditText)
        val maxEditText = view.findViewById<EditText>(R.id.maxBudgetEditText)
        val radioGroup = view.findViewById<RadioGroup>(R.id.typeRadioGroup)
        val saveButton = view.findViewById<Button>(R.id.saveCategoryButton)
        val backButton = view.findViewById<ImageView>(R.id.backButton)
        iconPreviewImage = view.findViewById(R.id.iconPreviewImage)

        iconPreviewImage.setImageDrawable(
            IconicsDrawable(requireContext(), GoogleMaterial.Icon.valueOf(selectedIconId))
        )
        iconPreviewImage.setBackgroundResource(R.drawable.icon_glow)
        iconPreviewImage.setPadding(24, 24, 24, 24)

        iconPreviewImage.setOnClickListener {
            IconPickerDialogFragment().show(parentFragmentManager, "iconPicker")
        }

        parentFragmentManager.setFragmentResultListener("icon_picker_result", viewLifecycleOwner) { _, result ->
            val iconId = result.getString("selected_icon_id") ?: "gmd_home"
            selectedIconId = iconId
            iconPreviewImage.setImageDrawable(
                IconicsDrawable(requireContext(), GoogleMaterial.Icon.valueOf(iconId))
            )
            iconPreviewImage.setBackgroundResource(R.drawable.icon_glow)
        }

        backButton.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        saveButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val minBudget = minEditText.text.toString().toDoubleOrNull()
            val maxBudget = maxEditText.text.toString().toDoubleOrNull()
            val type = when (radioGroup.checkedRadioButtonId) {
                R.id.expenseRadio -> "EXPENSE"
                R.id.incomeRadio -> "INCOME"
                else -> null
            }

            if (name.isEmpty() || type == null || minBudget == null || maxBudget == null) {
                Toast.makeText(context, "Fill all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (minBudget > maxBudget) {
                Toast.makeText(context, "Min budget cannot exceed max", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 🔐 Generate ID manually and sync to Room & Firestore
            val newDocRef = firestore.collection("users")
                .document(firebaseUid)
                .collection("categories")
                .document() // manually create doc ref

            val newCategoryId = newDocRef.id

            val categoryData = hashMapOf(
                "id" to newCategoryId,
                "userId" to firebaseUid,
                "name" to name,
                "type" to type,
                "minBudget" to minBudget,
                "maxBudget" to maxBudget,
                "iconId" to selectedIconId
            )

            newDocRef.set(categoryData)
                .addOnSuccessListener {
                    CoroutineScope(Dispatchers.IO).launch {
                        db.categoryDao().insertCategory(
                            Category(
                                id = newCategoryId,
                                userId = firebaseUid,
                                name = name,
                                type = type,
                                minBudget = minBudget,
                                maxBudget = maxBudget,
                                iconId = selectedIconId
                            )
                        )
                        launch(Dispatchers.Main) {
                            Toast.makeText(context, "Category added!", Toast.LENGTH_SHORT).show()
                            requireActivity().supportFragmentManager.popBackStack()
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to save to Firebase: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }
}
