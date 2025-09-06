package com.kilovativedesigns.parkaware.ui.onboarding

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.kilovativedesigns.parkaware.data.Prefs
import com.kilovativedesigns.parkaware.databinding.FragmentOnboardingBinding
import kotlinx.coroutines.launch

class OnboardingFragment : Fragment() {
    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.buttonContinue.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                Prefs.setOnboardingDone(requireContext(), true)
                // Navigate and clear back to decider so we don't return to onboarding
                findNavController().navigate(
                    com.kilovativedesigns.parkaware.R.id.action_onboarding_to_warning
                )
            }
        }
    }

    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}

