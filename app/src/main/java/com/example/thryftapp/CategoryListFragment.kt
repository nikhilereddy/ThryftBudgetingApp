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

    private lateinit var db: AppDatabase
    private lateinit var adapter: CategoryAdapter
    private var allCategories: List<Category> = emptyList()
    private var filteredList: List<Category> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_category_list, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        db = AppDatabase.getDatabase(requireContext())
        val prefs = requireContext().getSharedPreferences("thryft_session", 0)
        val userId = prefs.getInt("user_id", -1)

        if (userId == -1) {
            Toast.makeText(context, "Invalid User", Toast.LENGTH_SHORT).show()
            return
        }

        val listView: ListView = view.findViewById(R.id.categoryListView)
        val filterGroup: RadioGroup = view.findViewById(R.id.typeFilterGroup)
        val searchInput: EditText = view.findViewById(R.id.searchEditText)
        val addBtn: ImageView = view.findViewById(R.id.addCategoryIcon)
        val backBtn: ImageView = view.findViewById(R.id.backButton)

        adapter = CategoryAdapter(requireContext(), mutableListOf())
        listView.adapter = adapter

        loadCategories(userId)

        filterGroup.setOnCheckedChangeListener { _, _ ->
            val selectedType = when (filterGroup.checkedRadioButtonId) {
                R.id.expenseRadio -> "EXPENSE"
                R.id.incomeRadio -> "INCOME"
                else -> null
            }
            filterCategories(selectedType, searchInput.text.toString())
        }

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val selectedType = when (filterGroup.checkedRadioButtonId) {
                    R.id.expenseRadio -> "EXPENSE"
                    R.id.incomeRadio -> "INCOME"
                    else -> null
                }
                filterCategories(selectedType, s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        addBtn.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, AddCategoryFragment())
                .addToBackStack(null)
                .commit()
        }

        backBtn.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun loadCategories(userId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            allCategories = db.categoryDao().getAllCategories(userId)
            filteredList = allCategories
            launch(Dispatchers.Main) {
                adapter.updateList(filteredList)
            }
        }
    }

    private fun filterCategories(type: String?, query: String) {
        filteredList = allCategories.filter {
            (type == null || it.type == type) && it.name.contains(query, ignoreCase = true)
        }
        adapter.updateList(filteredList)
    }
}
