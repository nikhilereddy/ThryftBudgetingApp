package com.example.thryftapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.thryftapp.databinding.FragmentHomeBinding
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.googlematerial.GoogleMaterial
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.DecimalFormat

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: AppDatabase
    private val decimalFormat = DecimalFormat("#,##0.00")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = AppDatabase.getDatabase(requireContext())
        val prefs = requireContext().getSharedPreferences("thryft_session", 0)
        val userId = prefs.getInt("user_id", -1)

        // ✅ Hamburger Menu Click Handler
        binding.menuIcon.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, MenuFragment())
                .addToBackStack(null)
                .commit()
        }

        // 🔄 Load transaction and category data
        lifecycleScope.launch(Dispatchers.IO) {
            val transactions = db.transactionDao().getAllTransactions(userId)
            val categories = db.categoryDao().getAllCategories(userId)

            val income = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
            val expense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
            val balance = income - expense

            launch(Dispatchers.Main) {
                binding.totalBalanceText.text = "R ${decimalFormat.format(balance)}"
                binding.incomeText.text = "Income\nR ${decimalFormat.format(income)}"
                binding.expenseText.text = "Expense\nR ${decimalFormat.format(expense)}"

                val progress = if (income > 0) (expense / income * 100).toInt().coerceIn(0, 100) else 0
                binding.balanceProgressBar.progress = progress

                binding.categoryContainer.removeAllViews()

                categories.forEach { category ->
                    val categoryView = layoutInflater.inflate(R.layout.item_home_category, binding.categoryContainer, false)

                    val categoryTransactions = transactions.filter { it.categoryId == category.id }
                    val totalSpent = categoryTransactions.sumOf { it.amount }
                    val min = category.minBudget ?: 0.0
                    val max = category.maxBudget ?: 0.0

                    // Set category name
                    categoryView.findViewById<TextView>(R.id.categoryName).text = category.name

                    // Set budget status
                    val status = when {
                        totalSpent > max -> "Exceeded by R${decimalFormat.format(totalSpent - max)}"
                        totalSpent < min -> "Underspent by R${decimalFormat.format(min - totalSpent)}"
                        else -> "Left R${decimalFormat.format(max - totalSpent)}"
                    }

                    categoryView.findViewById<TextView>(R.id.budgetStatus).text = status

                    // Set progress bar
                    val progressPercent = if (max > 0) (totalSpent / max * 100).toInt().coerceIn(0, 100) else 0
                    categoryView.findViewById<ProgressBar>(R.id.progressBar).progress = progressPercent

                    // Set transaction lines
                    val lines = categoryTransactions.take(2).map {
                        "${it.description} - R${decimalFormat.format(it.amount)}"
                    }

                    categoryView.findViewById<TextView>(R.id.transactionLine1).text = lines.getOrNull(0) ?: ""
                    categoryView.findViewById<TextView>(R.id.transactionLine2).text = lines.getOrNull(1) ?: ""

                    // Set category icon dynamically
                    setIconForCategory(category, categoryView)

                    // Set the View All button listener
                    categoryView.findViewById<TextView>(R.id.viewAllBtn).setOnClickListener {
                        val intent = Intent(requireContext(), ViewCategoryTransactionsActivity::class.java)
                        intent.putExtra("category_id", category.id)
                        startActivity(intent)
                    }

                    binding.categoryContainer.addView(categoryView)
                }
            }
        }
    }

    // Function to set the icon dynamically for each category
    private fun setIconForCategory(category: Category, categoryView: View) {
        val iconImageView = categoryView.findViewById<ImageView>(R.id.categoryIcon)

        try {
            val iconEnum = GoogleMaterial.Icon.valueOf(category.iconId)
            val drawable = IconicsDrawable(requireContext(), iconEnum)
            iconImageView.setImageDrawable(drawable)
        } catch (e: Exception) {
            iconImageView.setImageResource(R.drawable.ic_lock)  // Fallback icon if invalid icon ID
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
