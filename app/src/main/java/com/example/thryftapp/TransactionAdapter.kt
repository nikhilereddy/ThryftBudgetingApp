package com.example.thryftapp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Locale

class TransactionAdapter(context: Context, private val transactions: List<Any>) : ArrayAdapter<Any>(context, 0, transactions) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val item = getItem(position)

        return if (item is String) {  // This is a date header
            val dateHeaderView = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_date_header, parent, false)
            val dateHeaderText = dateHeaderView.findViewById<TextView>(R.id.dateHeader)
            dateHeaderText.text = item  // Set the date header text
            dateHeaderView
        } else if (item is Transaction) {  // This is a transaction item
            val transactionView = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_transaction, parent, false)
            val name = transactionView.findViewById<TextView>(R.id.transactionName)
            val date = transactionView.findViewById<TextView>(R.id.transactionDate) // Updated to 'transactionDate'
            val amount = transactionView.findViewById<TextView>(R.id.transactionAmount)

            name.text = item.description

            // Format the date and set it to the TextView
            val formattedDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(item.date)
            date.text = formattedDate // Set the formatted date

            amount.text = if (item.amount > 0) "+R${item.amount}" else "R${item.amount}"
            // Style the amount text based on whether it’s income or expense
            if (item.amount < 0) {
                amount.setTextColor(ContextCompat.getColor(context, R.color.red)) // Set red for negative amounts (expenses)
            } else {
                amount.setTextColor(ContextCompat.getColor(context, R.color.green)) // Set green for positive amounts (income)
            }

            transactionView
        } else {
            super.getView(position, convertView, parent)
        }
    }
}
//correct version