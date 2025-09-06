package com.kilovativedesigns.parkaware.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.kilovativedesigns.parkaware.R
import com.kilovativedesigns.parkaware.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val b get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Child NavHost is declared in XML; just grab it.
        val childHost = childFragmentManager
            .findFragmentById(R.id.tabs_nav_host) as NavHostFragment

        // Wire bottom nav to child nav controller
        b.bottomNav.setupWithNavController(childHost.navController)

        // Optional: apply system bar insets to keep BNAV above gesture area
        ViewCompat.setOnApplyWindowInsetsListener(b.bottomNav) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(bottom = bars.bottom)
            insets
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}