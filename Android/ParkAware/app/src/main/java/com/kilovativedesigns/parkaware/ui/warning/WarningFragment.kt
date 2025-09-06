package com.kilovativedesigns.parkaware.ui.warning

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.kilovativedesigns.parkaware.R
import com.kilovativedesigns.parkaware.data.Prefs
import com.kilovativedesigns.parkaware.databinding.FragmentWarningBinding
import kotlinx.coroutines.launch

class WarningFragment : Fragment() {

    private var _binding: FragmentWarningBinding? = null
    private val binding get() = _binding!!

    private val handler = Handler(Looper.getMainLooper())
    private var tips: List<String> = emptyList()
    private var tipIndex = 0
    private val tipIntervalMs = 6000L

    private val rotateTip = object : Runnable {
        override fun run() {
            if (!isAdded || tips.isEmpty()) return
            tipIndex = (tipIndex + 1) % tips.size
            binding.tipText.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    binding.tipText.text = tips[tipIndex]
                    binding.tipText.animate().alpha(1f).setDuration(200).start()
                }
                .start()
            handler.postDelayed(this, tipIntervalMs)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWarningBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Load tips from resources
        tips = resources.getStringArray(R.array.warning_tips).toList()
        binding.tipsBox.isVisible = tips.isNotEmpty()
        if (tips.isNotEmpty()) {
            binding.tipText.text = tips[0]
            handler.postDelayed(rotateTip, tipIntervalMs)
        }

        // Links
        binding.privacyLink.setOnClickListener {
            openUrl("https://parkaware.app/privacy-policy-1")
        }
        binding.eulaLink.setOnClickListener {
            openUrl("https://www.apple.com/legal/internet-services/itunes/dev/stdeula/")
        }

        // CTA → mark seen and go to Home
        binding.cta.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                Prefs.setWarningSeen(requireContext(), true)
                findNavController().navigate(R.id.homeFragment)
            }
        }

        // Entry animation (fade/slide in)
        binding.card.translationY = 60f
        binding.card.alpha = 0f
        binding.card.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(500)
            .start()
    }

    private fun openUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    override fun onDestroyView() {
        handler.removeCallbacksAndMessages(null)
        _binding = null
        super.onDestroyView()
    }
}