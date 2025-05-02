package com.example.thryftapp

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.googlematerial.GoogleMaterial

class IconPickerDialogFragment : DialogFragment() {

    private lateinit var iconAdapter: IconGridAdapter //adapter for icons
    private lateinit var allIcons: List<GoogleMaterial.Icon> //list of all icons

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext()) //create dialog
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE) //remove title
        dialog.setContentView(R.layout.dialog_icon_picker) //set layout
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT) //fullscreen
        dialog.window?.setBackgroundDrawableResource(android.R.color.white) //white background

        val searchEditText = dialog.findViewById<EditText>(R.id.iconSearchEditText) //search input
        val gridView = dialog.findViewById<GridView>(R.id.iconGridView) //grid for icons

        allIcons = GoogleMaterial.Icon.values().toList() //get all icons
        iconAdapter = IconGridAdapter(requireContext(), allIcons) //init adapter
        gridView.adapter = iconAdapter //set adapter

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim().lowercase() //get search query
                val filtered = allIcons.filter { it.name.lowercase().contains(query) } //filter icons
                iconAdapter.updateIcons(filtered) //update list
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        gridView.setOnItemClickListener { _, _, position, _ ->
            val selectedIcon = iconAdapter.getItem(position)

            //send result back to parent via fragment result api
            parentFragmentManager.setFragmentResult(
                "icon_picker_result",
                Bundle().apply { putString("selected_icon_id", selectedIcon.name) }
            )
            dismiss() //close dialog
        }

        return dialog
    }

    class IconGridAdapter(private val context: android.content.Context, private var icons: List<GoogleMaterial.Icon>) : BaseAdapter() {

        fun updateIcons(newIcons: List<GoogleMaterial.Icon>) {
            icons = newIcons //update icon list
            notifyDataSetChanged() //refresh view
        }

        override fun getCount(): Int = icons.size //return count
        override fun getItem(position: Int): GoogleMaterial.Icon = icons[position] //get icon at position
        override fun getItemId(position: Int): Long = position.toLong() //return position as id

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val imageView = convertView as? ImageView ?: ImageView(context) //reuse or create imageview
            imageView.layoutParams = AbsListView.LayoutParams(96, 96) //set size
            imageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
            imageView.setPadding(8, 8, 8, 8)

            val drawable = IconicsDrawable(context, icons[position]) //create drawable
            imageView.setImageDrawable(drawable) //set icon

            return imageView //return view
        }
    }
}
