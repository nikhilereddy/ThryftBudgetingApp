package com.example.thryftapp

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.googlematerial.GoogleMaterial
import com.mikepenz.iconics.utils.color
import com.mikepenz.iconics.utils.sizeDp

class CategoryAdapter(
    private val context: Context,
    private var categories: MutableList<Category>
) : BaseAdapter() {

    fun updateList(newList: List<Category>) {
        categories.clear()
        categories.addAll(newList)
        notifyDataSetChanged()
    }

    override fun getCount(): Int = categories.size

    override fun getItem(position: Int): Any = categories[position]

    override fun getItemId(position: Int): Long = categories[position].id.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val category = categories[position]
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_category_card, parent, false)

        val nameText = view.findViewById<TextView>(R.id.categoryNameText)
        val typeText = view.findViewById<TextView>(R.id.categoryTypeText)
        val iconImage = view.findViewById<ImageView>(R.id.categoryIcon)

        nameText.text = category.name
        typeText.text = category.type

        // 🔥 Set icon using Android-Iconics (from iconId stored as a string like "gmd_home")
        try {
            val iconEnum = GoogleMaterial.Icon.valueOf(category.iconId)
            val drawable = IconicsDrawable(context, iconEnum)

            iconImage.setImageDrawable(drawable)
        } catch (e: Exception) {
            iconImage.setImageResource(R.drawable.ic_lock) // fallback if invalid icon
        }

        return view
    }
}
