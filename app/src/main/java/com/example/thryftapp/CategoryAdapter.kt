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
        categories.clear() //clear existing list
        categories.addAll(newList) //add new items
        notifyDataSetChanged() //refresh adapter
    }

    override fun getCount(): Int = categories.size //return list size

    override fun getItem(position: Int): Any = categories[position] //get item at position

   // override fun getItemId(position: Int): Long = categories[position].id.toLong() //return item id
   override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val category = categories[position] //get current category
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_category_card, parent, false) //inflate layout if needed

        val nameText = view.findViewById<TextView>(R.id.categoryNameText) //find name text
        val typeText = view.findViewById<TextView>(R.id.categoryTypeText) //find type text
        val iconImage = view.findViewById<ImageView>(R.id.categoryIcon) //find icon image

        nameText.text = category.name //set name
        typeText.text = category.type //set type

        //set icon from iconId
        try {
            val iconEnum = GoogleMaterial.Icon.valueOf(category.iconId) //get icon enum
            val drawable = IconicsDrawable(context, iconEnum) //create drawable

            iconImage.setImageDrawable(drawable) //set icon
        } catch (e: Exception) {
            iconImage.setImageResource(R.drawable.ic_lock) //fallback icon
        }

        return view //return vieww
    }
}
