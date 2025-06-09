package com.example.thryftapp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Locale

class TransactionAdapter(context: Context, private val transactions: List<Any>) :
    ArrayAdapter<Any>(context, 0, transactions) {

    private val currencyFormat = DecimalFormat("#,##0.00")

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val item = getItem(position)

        return when (item) {
            is String -> { // Date header
                val view = convertView ?: LayoutInflater.from(context)
                    .inflate(R.layout.list_item_date_header, parent, false)
                view.findViewById<TextView>(R.id.dateHeader).text = item
                view
            }

            is Transaction -> {
                val view = convertView ?: LayoutInflater.from(context)
                    .inflate(R.layout.list_item_transaction, parent, false)

                val name = view.findViewById<TextView>(R.id.transactionName)
                val date = view.findViewById<TextView>(R.id.transactionDate)
                val amount = view.findViewById<TextView>(R.id.transactionAmount)

                name.text = item.description ?: "No description"

                val formattedDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(item.date)
                date.text = formattedDate

                val absAmount = currencyFormat.format(kotlin.math.abs(item.amount))
                amount.text = if (item.amount >= 0) "+R$absAmount" else "-R$absAmount"

                val colorRes = if (item.amount >= 0) R.color.green else R.color.red
                amount.setTextColor(ContextCompat.getColor(context, colorRes))

                view
            }

            else -> super.getView(position, convertView, parent)
        }
    }
}
