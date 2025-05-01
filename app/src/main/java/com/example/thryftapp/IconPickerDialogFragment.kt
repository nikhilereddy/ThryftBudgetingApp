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

    private lateinit var iconAdapter: IconGridAdapter
    private lateinit var allIcons: List<GoogleMaterial.Icon>

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_icon_picker)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog.window?.setBackgroundDrawableResource(android.R.color.white)

        val searchEditText = dialog.findViewById<EditText>(R.id.iconSearchEditText)
        val gridView = dialog.findViewById<GridView>(R.id.iconGridView)

        allIcons = GoogleMaterial.Icon.values().toList()
        iconAdapter = IconGridAdapter(requireContext(), allIcons)
        gridView.adapter = iconAdapter

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim().lowercase()
                val filtered = allIcons.filter { it.name.lowercase().contains(query) }
                iconAdapter.updateIcons(filtered)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        gridView.setOnItemClickListener { _, _, position, _ ->
            val selectedIcon = iconAdapter.getItem(position)

            // ✅ Send result back to parent via FragmentResult API
            parentFragmentManager.setFragmentResult(
                "icon_picker_result",
                Bundle().apply { putString("selected_icon_id", selectedIcon.name) }
            )
            dismiss()
        }

        return dialog
    }

    class IconGridAdapter(private val context: android.content.Context, private var icons: List<GoogleMaterial.Icon>) : BaseAdapter() {

        fun updateIcons(newIcons: List<GoogleMaterial.Icon>) {
            icons = newIcons
            notifyDataSetChanged()
        }

        override fun getCount(): Int = icons.size
        override fun getItem(position: Int): GoogleMaterial.Icon = icons[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val imageView = convertView as? ImageView ?: ImageView(context)
            imageView.layoutParams = AbsListView.LayoutParams(96, 96)
            imageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
            imageView.setPadding(8, 8, 8, 8)

            val drawable = IconicsDrawable(context, icons[position])
            imageView.setImageDrawable(drawable)

            return imageView
        }
    }
}
