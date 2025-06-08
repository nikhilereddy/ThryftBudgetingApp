/*
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

    private lateinit var db: AppDatabase //database reference
    private val formatter = DecimalFormat("#,##0.00") //format for amount
    private var categoryId: Int = -1 //holds category id
    private var userId: Int = -1 //holds user id

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            categoryId = it.getInt("category_id", -1) //get category id from args
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_category_transactions, container, false) //inflate layout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ImageView>(R.id.backButton)?.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack() //go back on click
        }

        val header = view.findViewById<TextView>(R.id.categoryHeader) //header text
        val listView = view.findViewById<ListView>(R.id.transactionListView) //list view for transactions

        val prefs = requireContext().getSharedPreferences("thryft_session", 0) //get shared prefs
        userId = prefs.getInt("user_id", -1) //get user id

        if (categoryId == -1 || userId == -1) {
            Toast.makeText(requireContext(), "Invalid user or category", Toast.LENGTH_SHORT).show() //check for valid input
            return
        }

        db = AppDatabase.getDatabase(requireContext()) //initialize db

        lifecycleScope.launch(Dispatchers.IO) {
            val category = db.categoryDao().getCategoryById(categoryId) //get category
            val transactions = db.transactionDao().getTransactionsByCategory(categoryId, userId) //get transactions

            withContext(Dispatchers.Main) {
                if (category != null) {
                    header.text = "Transactions for ${category.name.uppercase(Locale.getDefault())}" //set header
                }

                if (transactions.isEmpty()) {
                    listView.adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_list_item_1,
                        listOf("No transactions available") //show message if none
                    )
                } else {
                    val items = transactions.map {
                        " Description: ${it.description} \n Amt: R${formatter.format(it.amount)} \n Date:${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(it.date)} \n"
                    }

                    listView.adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_list_item_1,
                        items //set transaction list
                    )
                }
            }
        }
    }

    companion object {
        fun newInstance(categoryId: Int): CategoryTransactionsFragment {
            val fragment = CategoryTransactionsFragment() //create new instance
            val args = Bundle() //bundle args
            args.putInt("category_id", categoryId) //put category id
            fragment.arguments = args //set args
            return fragment
        }
    }
}
*/
package com.example.thryftapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

class CategoryTransactionsFragment : Fragment() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private val formatter = DecimalFormat("#,##0.00")

    private var categoryId: String? = null
    private lateinit var categoryName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            categoryId = it.getString("category_id", null)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_category_transactions, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ImageView>(R.id.backButton)?.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        val header = view.findViewById<TextView>(R.id.categoryHeader)
        val listView = view.findViewById<ListView>(R.id.transactionListView)

        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val currentUser = firebaseAuth.currentUser
        if (currentUser == null || categoryId == null) {
            Toast.makeText(context, "Invalid user or category", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = currentUser.uid

        firestore.collection("users")
            .document(userId)
            .collection("categories")
            .document(categoryId!!)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    Toast.makeText(context, "Category not found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                categoryName = doc.getString("name") ?: "Unknown"

                firestore.collection("transactions")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("categoryId", categoryId)


                    .get()
                    .addOnSuccessListener { result ->
                        val transactions = result.mapNotNull { txn ->
                            val description = txn.getString("description") ?: return@mapNotNull null
                            val amount = txn.getDouble("amount") ?: return@mapNotNull null
                            val date = txn.getDate("date") ?: return@mapNotNull null

                            " Description: $description \n Amt: R${formatter.format(amount)} \n Date: ${
                                SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date)
                            } \n"
                        }

                        header.text = "Transactions for ${categoryName.uppercase(Locale.getDefault())}"

                        val displayList = if (transactions.isEmpty())
                            listOf("No transactions available")
                        else transactions

                        listView.adapter = ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_list_item_1,
                            displayList
                        )
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Error loading transactions", Toast.LENGTH_LONG).show()
                    }

            }
            .addOnFailureListener {
                Toast.makeText(context, "Error fetching category", Toast.LENGTH_LONG).show()
            }
    }

    companion object {
        fun newInstance(categoryId: String): CategoryTransactionsFragment {
            val fragment = CategoryTransactionsFragment()
            val args = Bundle()
            args.putString("category_id", categoryId)
            fragment.arguments = args
            return fragment
        }
    }
}
