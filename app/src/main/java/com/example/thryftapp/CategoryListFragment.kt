package com.example.thryftapp

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CategoryListFragment : Fragment() {

    private lateinit var db: AppDatabase //database reference
    private lateinit var adapter: CategoryAdapter //custom adapter for list
    private var allCategories: List<Category> = emptyList() //full list of categories
    private var filteredList: List<Category> = emptyList() //filtered list

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_category_list, container, false) //inflate layout

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        db = AppDatabase.getDatabase(requireContext()) //init db
        val prefs = requireContext().getSharedPreferences("thryft_session", 0) //get prefs
        val userId = prefs.getInt("user_id", -1) //get user id

        if (userId == -1) { //check if user valid
            Toast.makeText(context, "Invalid User", Toast.LENGTH_SHORT).show()
            return
        }

        val listView: ListView = view.findViewById(R.id.categoryListView) //find list view
        val filterGroup: RadioGroup = view.findViewById(R.id.typeFilterGroup) //find radio group
        val searchInput: EditText = view.findViewById(R.id.searchEditText) //find search input
        val addBtn: ImageView = view.findViewById(R.id.addCategoryIcon) //find add button
        val backBtn: ImageView = view.findViewById(R.id.backButton) //find back button

        adapter = CategoryAdapter(requireContext(), mutableListOf()) //init adapter
        listView.adapter = adapter //set adapter to list

        loadCategories(userId) //load all categories

        filterGroup.setOnCheckedChangeListener { _, _ ->
            val selectedType = when (filterGroup.checkedRadioButtonId) {
                R.id.expenseRadio -> "EXPENSE"
                R.id.incomeRadio -> "INCOME"
                else -> null
            }
            filterCategories(selectedType, searchInput.text.toString()) //filter on type
        }

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val selectedType = when (filterGroup.checkedRadioButtonId) {
                    R.id.expenseRadio -> "EXPENSE"
                    R.id.incomeRadio -> "INCOME"
                    else -> null
                }
                filterCategories(selectedType, s.toString()) //filter on text
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        addBtn.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, AddCategoryFragment())
                .addToBackStack(null)
                .commit() //navigate to add screen
        }

        backBtn.setOnClickListener {
            parentFragmentManager.popBackStack() //navigate back
        }
    }

    private fun loadCategories(userId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            allCategories = db.categoryDao().getAllCategories(userId) //fetch all categories
            filteredList = allCategories //set filtered list
            launch(Dispatchers.Main) {
                adapter.updateList(filteredList) //update ui
            }
        }
    }

    private fun filterCategories(type: String?, query: String) {
        filteredList = allCategories.filter {
            (type == null || it.type == type) && it.name.contains(query, ignoreCase = true)
        } //apply filter
        adapter.updateList(filteredList) //update list
    }
}
