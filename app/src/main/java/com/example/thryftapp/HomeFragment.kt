package com.example.thryftapp

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.thryftapp.databinding.FragmentHomeBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.googlematerial.GoogleMaterial
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.DecimalFormat
import java.util.*
/**
 * Attribution:
 * Website: Class IconicsDrawable

 *  Author: mikepenz
 *  URL: https://www.javadoc.io/doc/com.mikepenz/iconics-core/2.8.5/com/mikepenz/iconics/IconicsDrawable.html
 *  Accessed on: 2025-06-07
-        */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val decimalFormat = DecimalFormat("#,##0.00")
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    //handle dashboard view created
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //get user id from shared prefs
        val prefs = requireContext().getSharedPreferences("thryft_session", 0)
        val userId = prefs.getString("user_id", null)

        //check if user is logged in
        if (userId.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            Log.d("DashboardFragment", "user id not found in shared preferences") //log auth failure
            return
        }

        //navigate to menu fragment on menu icon click
        binding.menuIcon.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, MenuFragment())
                .addToBackStack(null)
                .commit()
            Log.d("DashboardFragment", "navigated to menu fragment") //log nav
        }

        //navigate to transactions on arrow click
        binding.arrow.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, TransactionFragment())
                .addToBackStack(null)
                .commit()
            Log.d("DashboardFragment", "navigated to transaction fragment") //log nav
        }

        //load user data
        loadDataFromFirestore(userId)
        Log.d("DashboardFragment", "loadDataFromFirestore called with $userId") //log data fetch
    }

    //load user transactions and categories from firestore
    private fun loadDataFromFirestore(userId: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                //fetch transactions for user
                val txnSnapshot = firestore.collection("transactions")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()

                //fetch categories for user
                val catSnapshot = firestore.collection("users")
                    .document(userId)
                    .collection("categories")
                    .get()
                    .await()

                //parse transactions
                val transactions = txnSnapshot.documents.mapNotNull { doc ->
                    try {
                        Transaction(
                            id = 0,
                            userId = doc.getString("userId") ?: return@mapNotNull null,
                            categoryId = doc.getString("categoryId"),
                            amount = doc.getDouble("amount") ?: 0.0,
                            type = doc.getString("type") ?: "",
                            description = doc.getString("description"),
                            photoUri = doc.getString("photoUri"),
                            date = doc.getDate("date") ?: Date(),
                            createdAt = doc.getDate("createdAt") ?: Date()
                        )
                    } catch (e: Exception) {
                        Log.d("DashboardFragment", "failed to parse transaction: ${e.message}") //log parse error
                        null
                    }
                }

                Log.d("DashboardFragment", "loaded ${transactions.size} transactions") //log count

                //parse categories
                val categories = catSnapshot.documents.mapNotNull { doc ->
                    try {
                        Category(
                            id = doc.getString("id") ?: doc.id,
                            userId = userId,
                            name = doc.getString("name") ?: "Unnamed",
                            type = doc.getString("type") ?: "EXPENSE",
                            minBudget = doc.getDouble("minBudget") ?: 0.0,
                            maxBudget = doc.getDouble("maxBudget") ?: 0.0,
                            iconId = doc.getString("iconId") ?: "gmd_home"
                        )
                    } catch (e: Exception) {
                        Log.d("DashboardFragment", "failed to parse category: ${e.message}") //log parse error
                        null
                    }
                }

                Log.d("DashboardFragment", "loaded ${categories.size} categories") //log count

                //update ui on main thread
                withContext(Dispatchers.Main) {
                    updateUI(transactions, categories)
                    Log.d("DashboardFragment", "ui updated with data") //log ui update
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error loading data: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.d("DashboardFragment", "error loading data: ${e.message}") //log failure
                }
            }
        }
    }

    //update ui with totals and category widgets
    private fun updateUI(transactions: List<Transaction>, categories: List<Category>) {
        //calculate totals
        val income = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
        val expense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val balance = income - expense

        //display totals
        binding.totalBalanceText.text = "R ${decimalFormat.format(balance)}"
        binding.incomeText.text = "Income\nR ${decimalFormat.format(income)}"
        binding.expenseText.text = "Expense\nR ${decimalFormat.format(expense)}"
        binding.balanceProgressBar.progress =
            if (income > 0) (expense / income * 100).toInt().coerceIn(0, 100) else 0

        Log.d("DashboardFragment", "income=$income, expense=$expense, balance=$balance") //log totals

        //clear previous category views
        binding.categoryContainer.removeAllViews()

        //render each category card
        categories.forEach { category ->
            val view = layoutInflater.inflate(R.layout.item_home_category, binding.categoryContainer, false)

            val categoryTransactions = transactions.filter { it.categoryId == category.id }
            val totalSpent = categoryTransactions.sumOf { it.amount }
            val min = category.minBudget
            val max = category.maxBudget

            view.findViewById<TextView>(R.id.categoryName).text = category.name

            //determine budget status
            val status = when {
                totalSpent > max -> "Exceeded by R${decimalFormat.format(totalSpent - max)}"
                totalSpent < min -> "Underspent by R${decimalFormat.format(min - totalSpent)}"
                else -> "Left R${decimalFormat.format(max - totalSpent)}"
            }

            view.findViewById<TextView>(R.id.budgetStatus).text = status
            view.findViewById<ProgressBar>(R.id.progressBar).progress =
                if (max > 0) (totalSpent / max * 100).toInt().coerceIn(0, 100) else 0

            Log.d("DashboardFragment", "${category.name}: spent=$totalSpent, min=$min, max=$max") //log category

            //render latest transactions (up to 2 lines)
            val lines = categoryTransactions.take(2).map {
                "${it.description} - R${decimalFormat.format(it.amount)}"
            }

            view.findViewById<TextView>(R.id.transactionLine1).text = lines.getOrNull(0) ?: ""
            view.findViewById<TextView>(R.id.transactionLine2).text = lines.getOrNull(1) ?: ""

            //set category icon
            setIconForCategory(category, view)

            //handle view all click
            view.findViewById<TextView>(R.id.viewAllBtn).setOnClickListener {
                val frag = CategoryTransactionsFragment().apply {
                    arguments = Bundle().apply {
                        putString("category_id", category.id)
                    }
                }
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, frag)
                    .addToBackStack(null)
                    .commit()

                Log.d("DashboardFragment", "navigated to transactions for ${category.name}") //log navigation
            }

            //add category view to container
            binding.categoryContainer.addView(view)
        }
    }


    //set icon for a category card
    private fun setIconForCategory(category: Category, view: View) {
        val iconView = view.findViewById<ImageView>(R.id.categoryIcon)
        try {
            val icon = GoogleMaterial.Icon.valueOf(category.iconId)
            val drawable = IconicsDrawable(requireContext(), icon)
            iconView.setImageDrawable(drawable)
        } catch (e: Exception) {
            iconView.setImageResource(R.drawable.ic_lock)
            Log.d("DashboardFragment", "failed to set icon for ${category.name}: ${e.message}") //log fallback
        }
    }
    /**
     * Attribution:
     * Website: Class IconicsDrawable

     *  Author: mikepenz
     *  URL: https://www.javadoc.io/doc/com.mikepenz/iconics-core/2.8.5/com/mikepenz/iconics/IconicsDrawable.html
     *  Accessed on: 2025-06-07
    -        */

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
