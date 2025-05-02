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

    private var _binding: FragmentHomeBinding? = null //binding reference
    private val binding get() = _binding!! //non-null binding
    private lateinit var db: AppDatabase //database reference
    private val decimalFormat = DecimalFormat("#,##0.00") //format currency

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false) //inflate view binding
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = AppDatabase.getDatabase(requireContext()) //init db
        val prefs = requireContext().getSharedPreferences("thryft_session", 0) //get prefs
        val userId = prefs.getInt("user_id", -1) //get user id

        //menu icon click opens menu fragment
        binding.menuIcon.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, MenuFragment())
                .addToBackStack(null)
                .commit()
        }

        //arrow icon click opens transactions fragment
        binding.arrow.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, TransactionFragment())
                .addToBackStack(null)
                .commit()
        }

        //load data from db
        lifecycleScope.launch(Dispatchers.IO) {
            val transactions = db.transactionDao().getAllTransactions(userId) //get transactions
            val categories = db.categoryDao().getAllCategories(userId) //get categories

            val income = transactions.filter { it.type == "INCOME" }.sumOf { it.amount } //sum income
            val expense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount } //sum expenses
            val balance = income - expense //calculate balance

            launch(Dispatchers.Main) {
                binding.totalBalanceText.text = "R ${decimalFormat.format(balance)}" //set balance text
                binding.incomeText.text = "Income\nR ${decimalFormat.format(income)}" //set income text
                binding.expenseText.text = "Expense\nR ${decimalFormat.format(expense)}" //set expense text

                val progress = if (income > 0) (expense / income * 100).toInt().coerceIn(0, 100) else 0 //calculate progress
                binding.balanceProgressBar.progress = progress //set progress bar

                binding.categoryContainer.removeAllViews() //clear container

                categories.forEach { category ->
                    val categoryView = layoutInflater.inflate(R.layout.item_home_category, binding.categoryContainer, false) //inflate category view

                    val categoryTransactions = transactions.filter { it.categoryId == category.id } //filter category transactions
                    val totalSpent = categoryTransactions.sumOf { it.amount } //sum spent
                    val min = category.minBudget ?: 0.0
                    val max = category.maxBudget ?: 0.0

                    categoryView.findViewById<TextView>(R.id.categoryName).text = category.name //set category name

                    val status = when {
                        totalSpent > max -> "Exceeded by R${decimalFormat.format(totalSpent - max)}"
                        totalSpent < min -> "Underspent by R${decimalFormat.format(min - totalSpent)}"
                        else -> "Left R${decimalFormat.format(max - totalSpent)}"
                    }

                    categoryView.findViewById<TextView>(R.id.budgetStatus).text = status //set budget status

                    val progressPercent = if (max > 0) (totalSpent / max * 100).toInt().coerceIn(0, 100) else 0 //calculate progress
                    categoryView.findViewById<ProgressBar>(R.id.progressBar).progress = progressPercent //set progress bar

                    val lines = categoryTransactions.take(2).map {
                        "${it.description} - R${decimalFormat.format(it.amount)}"
                    }

                    categoryView.findViewById<TextView>(R.id.transactionLine1).text = lines.getOrNull(0) ?: "" //first transaction line
                    categoryView.findViewById<TextView>(R.id.transactionLine2).text = lines.getOrNull(1) ?: "" //second transaction line

                    setIconForCategory(category, categoryView) //set icon for category

                    categoryView.findViewById<TextView>(R.id.viewAllBtn).setOnClickListener {
                        val categoryId = category.id

                        val categoryListFragment = CategoryTransactionsFragment().apply {
                            arguments = Bundle().apply {
                                putInt("category_id", categoryId) //pass category id
                            }
                        }

                        requireActivity().supportFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, categoryListFragment)
                            .addToBackStack(null)
                            .commit()
                    }

                    binding.categoryContainer.addView(categoryView) //add to container
                }
            }
        }
    }

    //function to set icon for category
    private fun setIconForCategory(category: Category, categoryView: View) {
        val iconImageView = categoryView.findViewById<ImageView>(R.id.categoryIcon)

        try {
            val iconEnum = GoogleMaterial.Icon.valueOf(category.iconId)
            val drawable = IconicsDrawable(requireContext(), iconEnum)
            iconImageView.setImageDrawable(drawable)
        } catch (e: Exception) {
            iconImageView.setImageResource(R.drawable.ic_lock) //fallback icon
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null //clear binding
    }
}
