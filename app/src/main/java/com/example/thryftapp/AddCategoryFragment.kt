package com.example.thryftapp

import android.os.Bundle
import android.util.Log
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
//done
class AddCategoryFragment : Fragment() {

    private lateinit var db: AppDatabase
    private lateinit var firestore: FirebaseFirestore
    private var selectedIconId: String = "gmd_home"
    private lateinit var iconPreviewImage: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_add_category, container, false) //inflate layout

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        db = AppDatabase.getDatabase(requireContext()) //initialize room db
        firestore = FirebaseFirestore.getInstance() //initialize firestore

        val prefs = requireContext().getSharedPreferences("thryft_session", 0) //get shared prefs
        val firebaseUid = prefs.getString("user_id", null) //get user id

        //check if user is invalid
        if (firebaseUid.isNullOrEmpty()) {
            Toast.makeText(context, "Invalid User", Toast.LENGTH_SHORT).show()
            requireActivity().supportFragmentManager.popBackStack()
            return
        }

        val nameEditText = view.findViewById<EditText>(R.id.categoryNameEditText) //get name input
        val minEditText = view.findViewById<EditText>(R.id.minBudgetEditText) //get min input
        val maxEditText = view.findViewById<EditText>(R.id.maxBudgetEditText) //get max input
        val radioGroup = view.findViewById<RadioGroup>(R.id.typeRadioGroup) //get radio group
        val saveButton = view.findViewById<Button>(R.id.saveCategoryButton) //get save button
        val backButton = view.findViewById<ImageView>(R.id.backButton) //get back button
        iconPreviewImage = view.findViewById(R.id.iconPreviewImage) //get icon preview

        iconPreviewImage.setImageDrawable(
            IconicsDrawable(requireContext(), GoogleMaterial.Icon.valueOf(selectedIconId)) //set default icon
        )
        iconPreviewImage.setBackgroundResource(R.drawable.icon_glow) //set background glow
        iconPreviewImage.setPadding(24, 24, 24, 24) //add padding

        iconPreviewImage.setOnClickListener {
            IconPickerDialogFragment().show(parentFragmentManager, "iconPicker") //open icon picker
        }

        parentFragmentManager.setFragmentResultListener("icon_picker_result", viewLifecycleOwner) { _, result ->
            val iconId = result.getString("selected_icon_id") ?: "gmd_home" //get picked icon
            selectedIconId = iconId //update icon id
            iconPreviewImage.setImageDrawable(
                IconicsDrawable(requireContext(), GoogleMaterial.Icon.valueOf(iconId)) //update icon drawable
            )
            iconPreviewImage.setBackgroundResource(R.drawable.icon_glow) //update background glow
        }

        backButton.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack() //go back
        }

        saveButton.setOnClickListener {
            val name = nameEditText.text.toString().trim() //get name value
            val minBudget = minEditText.text.toString().toDoubleOrNull() //get min value
            val maxBudget = maxEditText.text.toString().toDoubleOrNull() //get max value
            val type = when (radioGroup.checkedRadioButtonId) {
                R.id.expenseRadio -> "EXPENSE" //check if expense
                R.id.incomeRadio -> "INCOME" //check if income
                else -> null //null if none
            }

            //check if any field is empty or null
            if (name.isEmpty() || type == null || minBudget == null || maxBudget == null) {
                Toast.makeText(context, "Fill all required fields", Toast.LENGTH_SHORT).show()
                Log.d("AddCategoryFragment", "fields missing or null") //log missing fields
                return@setOnClickListener
            }

            //check if min budget is greater than max
            if (minBudget > maxBudget) {
                Toast.makeText(context, "Min budget cannot exceed max", Toast.LENGTH_SHORT).show()
                Log.d("AddCategoryFragment", "min budget > max budget") //log invalid budget
                return@setOnClickListener
            }

            //generate id manually and sync to room & firestore
            val newDocRef = firestore.collection("users")
                .document(firebaseUid)
                .collection("categories")
                .document() //manually create doc ref

            val newCategoryId = newDocRef.id //get new category id

            val categoryData = hashMapOf(
                "id" to newCategoryId,
                "userId" to firebaseUid,
                "name" to name,
                "type" to type,
                "minBudget" to minBudget,
                "maxBudget" to maxBudget,
                "iconId" to selectedIconId
            ) //create firestore data

            newDocRef.set(categoryData)
                .addOnSuccessListener {
                    Log.d("AddCategoryFragment", "category saved to firestore") //log firestore success
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
                        ) //save to room
                        launch(Dispatchers.Main) {
                            Toast.makeText(context, "Category added!", Toast.LENGTH_SHORT).show()
                            Log.d("AddCategoryFragment", "category saved to room") //log room success
                            requireActivity().supportFragmentManager.popBackStack() //go back
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to save to Firebase: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.d("AddCategoryFragment", "failed to save category: ${e.message}") //log error
                }
        }
    }
}
