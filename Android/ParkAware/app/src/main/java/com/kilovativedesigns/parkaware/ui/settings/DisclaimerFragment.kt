package com.kilovativedesigns.parkaware.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.kilovativedesigns.parkaware.databinding.FragmentDisclaimerBinding

class DisclaimerFragment : Fragment() {
    private var _b: FragmentDisclaimerBinding? = null
    private val b get() = _b!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _b = FragmentDisclaimerBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onDestroyView() {
        _b = null
        super.onDestroyView()
    }
}