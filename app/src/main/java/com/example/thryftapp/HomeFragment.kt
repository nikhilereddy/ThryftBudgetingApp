package com.example.thryftapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.thryftapp.databinding.FragmentHomeBinding
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
        db = AppDatabase.getDatabase(requireContext())
        val prefs = requireContext().getSharedPreferences("thryft_session", 0)

        val userId = prefs.getInt("user_id", -1)
        val userName = prefs.getString("user_name", "User") ?: "User"
        binding.welcomeTextView.text = "Welcome, $userName!"

        lifecycleScope.launch(Dispatchers.IO) {
            val transactions = db.transactionDao().getAllTransactions(userId)

            val income = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
            val expense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
            val balance = income - expense

            launch(Dispatchers.Main) {
                binding.incomeTextView.text = "Income: $${decimalFormat.format(income)}"
                binding.expenseTextView.text = "Expense: $${decimalFormat.format(expense)}"
                binding.balanceTextView.text = "Balance: $${decimalFormat.format(balance)}"
            }
        }

        binding.addTransactionButton.setOnClickListener {
            startActivity(Intent(requireContext(), TransactionActivity::class.java))
        }

        binding.manageCategoriesButton.setOnClickListener {
            startActivity(Intent(requireContext(), CategoryActivity::class.java))
        }

        binding.viewGraphButton.setOnClickListener {
            startActivity(Intent(requireContext(), GraphActivity::class.java))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
