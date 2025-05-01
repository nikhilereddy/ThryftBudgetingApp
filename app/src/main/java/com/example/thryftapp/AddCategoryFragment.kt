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
    private var selectedIconId: String = "gmd_home"
    private lateinit var iconPreviewImage: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_add_category, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        db = AppDatabase.getDatabase(requireContext())
        val prefs = requireContext().getSharedPreferences("thryft_session", 0)
        val userId = prefs.getInt("user_id", -1)

        if (userId == -1) {
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

        // Set default icon with glow
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
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "Category added!", Toast.LENGTH_SHORT).show()
                    requireActivity().supportFragmentManager.popBackStack()
                }
            }
        }
    }
}
