package com.example.thryftapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

class CategoryTransactionsFragment : Fragment() {

    private lateinit var db: AppDatabase
    private val formatter = DecimalFormat("#,##0.00")
    private var categoryId: Int = -1
    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            categoryId = it.getInt("category_id", -1)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_category_transactions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ImageView>(R.id.backButton)?.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        val header = view.findViewById<TextView>(R.id.categoryHeader)
        val listView = view.findViewById<ListView>(R.id.transactionListView)

        val prefs = requireContext().getSharedPreferences("thryft_session", 0)
        userId = prefs.getInt("user_id", -1)

        if (categoryId == -1 || userId == -1) {
            Toast.makeText(requireContext(), "Invalid user or category", Toast.LENGTH_SHORT).show()
            return
        }

        db = AppDatabase.getDatabase(requireContext())

        lifecycleScope.launch(Dispatchers.IO) {
            val category = db.categoryDao().getCategoryById(categoryId)
            val transactions = db.transactionDao().getTransactionsByCategory(categoryId, userId)

            withContext(Dispatchers.Main) {
                if (category != null) {
                    header.text = "Transactions for ${category.name.uppercase(Locale.getDefault())}"
                }

                if (transactions.isEmpty()) {
                    listView.adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_list_item_1,
                        listOf("No transactions available")
                    )
                } else {
                    val items = transactions.map {
                        " Description: ${it.description} \n Amt: R${formatter.format(it.amount)} \n Date:${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(it.date)} \n"
                    }

                    listView.adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_list_item_1,
                        items
                    )
                }
            }
        }
    }

    companion object {
        fun newInstance(categoryId: Int): CategoryTransactionsFragment {
            val fragment = CategoryTransactionsFragment()
            val args = Bundle()
            args.putInt("category_id", categoryId)
            fragment.arguments = args
            return fragment
        }
    }
}
