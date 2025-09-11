package com.kilovativedesigns.parkaware.ui.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.kilovativedesigns.parkaware.R
import com.kilovativedesigns.parkaware.data.Prefs
import com.kilovativedesigns.parkaware.databinding.FragmentOnboardingBinding
import kotlinx.coroutines.launch

class OnboardingFragment : Fragment() {

    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!

    // --- Permission launchers ------------------------------------------------

    // Android 13+ notifications
    private val notifPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            // Optional UX: nudge if denied
            if (!granted && Build.VERSION.SDK_INT >= 33) {
                Snackbar.make(binding.root,
                    getString(R.string.notif_perm_denied_hint),
                    Snackbar.LENGTH_LONG
                ).show()
            }
            // Continue to location step next
            requestLocationPermissions()
        }

    // Coarse + Fine location in one go
    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            val fine = results[Manifest.permission.ACCESS_FINE_LOCATION] == true
            val coarse = results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (!fine && !coarse) {
                Snackbar.make(
                    binding.root,
                    getString(R.string.location_perm_denied_hint),
                    Snackbar.LENGTH_LONG
                ).show()
            }
            // Done with onboarding
            completeOnboardingAndNavigate()
        }

    // --- Lifecycle -----------------------------------------------------------

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        binding.buttonContinue.setOnClickListener {
            // Kick off the permission flow: Notifications (if needed) -> Location
            requestNotificationsThenLocation()
        }
    }

    // --- Flow helpers --------------------------------------------------------

    private fun requestNotificationsThenLocation() {
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        // Below Android 13, or already granted → go straight to location
        requestLocationPermissions()
    }

    private fun requestLocationPermissions() {
        val fineGranted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            // Already have at least coarse
            completeOnboardingAndNavigate()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }
    }

    private fun completeOnboardingAndNavigate() {
        viewLifecycleOwner.lifecycleScope.launch {
            Prefs.setOnboardingDone(requireContext(), true)
            // Navigate and clear back to decider so we don't return to onboarding
            findNavController().navigate(R.id.action_onboarding_to_warning)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}