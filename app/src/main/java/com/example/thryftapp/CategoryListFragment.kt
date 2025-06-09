package com.example.thryftapp

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CategoryListFragment : Fragment() {

    private lateinit var adapter: CategoryAdapter
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private var allCategories: MutableList<Category> = mutableListOf()
    private var filteredList: MutableList<Category> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_category_list, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            Toast.makeText(context, "Invalid User", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = currentUser.uid
        val listView: ListView = view.findViewById(R.id.categoryListView) //bind category list view
        val filterGroup: RadioGroup = view.findViewById(R.id.typeFilterGroup) //bind radio filter group
        val searchInput: EditText = view.findViewById(R.id.searchEditText) //bind search input field
        val addBtn: ImageView = view.findViewById(R.id.addCategoryIcon) //bind add category button
        val backBtn: ImageView = view.findViewById(R.id.backButton) //bind back button


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

        //listen for search input text change
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val selectedType = when (filterGroup.checkedRadioButtonId) {
                    R.id.expenseRadio -> "EXPENSE"
                    R.id.incomeRadio -> "INCOME"
                    else -> null
                }
                Log.d("CategoryFilter", "search=${s.toString()}, type=$selectedType") //log filter inputs
                filterCategories(selectedType, s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        //handle add category button click
        addBtn.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, AddCategoryFragment())
                .addToBackStack(null)
                .commit()
            Log.d("CategoryListFragment", "navigated to add category fragment") //log navigation
        }


        backBtn.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    //load categories from firestore into list
    private fun loadCategories(userId: String) {
        firestore.collection("users")
            .document(userId)
            .collection("categories")
            .get()
            .addOnSuccessListener { result ->
                allCategories.clear()
                for (doc in result) {
                    val name = doc.getString("name") ?: continue
                    val type = doc.getString("type") ?: continue
                    val min = doc.getDouble("minBudget") ?: 0.0
                    val max = doc.getDouble("maxBudget") ?: 0.0
                    val icon = doc.getString("iconId") ?: "gmd_home"

                    val id = doc.getString("id") ?: doc.id

                    allCategories.add(
                        Category(
                            id = id,
                            userId = userId,
                            name = name,
                            type = type,
                            minBudget = min,
                            maxBudget = max,
                            iconId = icon
                        )
                    )

                    Log.d("CategoryListFragment", "loaded category: $name [$type]") //log each loaded category
                }

                //update filtered list
                filteredList = allCategories.toMutableList()
                adapter.updateList(filteredList)
                Log.d("CategoryListFragment", "total loaded: ${allCategories.size}") //log total count
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to load categories: ${it.message}", Toast.LENGTH_SHORT).show()
                Log.d("CategoryListFragment", "load failed: ${it.message}") //log error
            }
    }


    private fun filterCategories(type: String?, query: String) {
        filteredList = allCategories.filter {
            (type == null || it.type == type) && it.name.contains(query, ignoreCase = true)
        }.toMutableList()
        adapter.updateList(filteredList)
    }
}
