package com.kilovativedesigns.parkaware.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.kilovativedesigns.parkaware.databinding.FragmentSocialSettingsBinding

class SocialSettingsFragment : Fragment() {

    private var _b: FragmentSocialSettingsBinding? = null
    private val b get() = _b!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _b = FragmentSocialSettingsBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Back arrow → navigate up
        b.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        // Copy referral code
        b.btnCopy.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            copyReferralCode()
        }

        // Share referral/app link
        b.btnShare.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            shareApp()
        }

        // Open Instagram
        b.btnInstagram.setOnClickListener {
            openInstagram()
        }

        // Open Play Store / App Store
        b.btnRate.setOnClickListener {
            openAppStore()
        }
    }

    private fun copyReferralCode() {
        val code = b.tvReferralCode.text?.toString().orEmpty()
        if (code.isBlank()) {
            Toast.makeText(requireContext(), "No referral code available.", Toast.LENGTH_SHORT).show()
            return
        }

        val cm = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("ParkAware referral code", code))

        Toast.makeText(requireContext(), "Referral code copied.", Toast.LENGTH_SHORT).show()
    }

    private fun shareApp() {
        val appPackage = requireContext().packageName
        val shareMessage = "I'm using ParkAware to park smarter and avoid parking fines. Check it out now!"
        val uri = "https://play.google.com/store/apps/details?id=$appPackage"

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "ParkAware")
            putExtra(Intent.EXTRA_TEXT, "$shareMessage\n$uri")
        }
        startActivity(Intent.createChooser(intent, "Share ParkAware"))
    }

    private fun openInstagram() {
        val url = "https://www.instagram.com/parkaware.app?igsh=c24zaTN4ZTFyOTJh&utm_source=qr"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    private fun openAppStore() {
        val appPackage = requireContext().packageName
        val uri = Uri.parse("https://play.google.com/store/apps/details?id=$appPackage")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        startActivity(intent)
    }

    override fun onDestroyView() {
        _b = null
        super.onDestroyView()
    }
}