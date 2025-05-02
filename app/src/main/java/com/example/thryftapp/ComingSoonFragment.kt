package com.example.thryftapp

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.Fragment

class ComingSoonFragment : Fragment(R.layout.fragment_coming_soon) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ImageView>(R.id.backButton)?.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
    }
}
