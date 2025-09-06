package com.kilovativedesigns.parkaware.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.kilovativedesigns.parkaware.databinding.FragmentTermsBinding

class TermsFragment : Fragment() {
    private var _b: FragmentTermsBinding? = null
    private val b get() = _b!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = FragmentTermsBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        b.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
    }

    override fun onDestroyView() { _b = null; super.onDestroyView() }
}