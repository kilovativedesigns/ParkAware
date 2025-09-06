package com.kilovativedesigns.parkaware.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.kilovativedesigns.parkaware.R
import com.kilovativedesigns.parkaware.databinding.FragmentAboutBinding

class AboutFragment : Fragment() {

    private var _b: FragmentAboutBinding? = null
    private val b get() = _b!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _b = FragmentAboutBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Back arrow
        b.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        // Row clicks → nav destinations defined in tabs_nav_graph.xml
        b.rowAbout.setOnClickListener {
            findNavController().navigate(R.id.aboutInfoFragment)
        }
        b.rowDisclaimer.setOnClickListener {
            findNavController().navigate(R.id.disclaimerFragment)
        }
        b.rowTerms.setOnClickListener {
            findNavController().navigate(R.id.termsFragment)
        }
        b.rowPrivacy.setOnClickListener {
            findNavController().navigate(R.id.privacyPolicyFragment)
        }
    }

    override fun onDestroyView() {
        _b = null
        super.onDestroyView()
    }
}