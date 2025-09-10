package com.kilovativedesigns.parkaware.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.kilovativedesigns.parkaware.databinding.FragmentAboutInfoBinding

class AboutInfoFragment : Fragment() {
    private var _b: FragmentAboutInfoBinding? = null
    private val b get() = _b!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _b = FragmentAboutInfoBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // No toolbar handling here; MainActivity manages the top app bar
    }

    override fun onDestroyView() {
        _b = null
        super.onDestroyView()
    }
}