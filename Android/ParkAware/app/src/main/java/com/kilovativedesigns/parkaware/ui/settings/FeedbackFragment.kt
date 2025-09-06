package com.kilovativedesigns.parkaware.ui.settings

import android.content.ActivityNotFoundException
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
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.kilovativedesigns.parkaware.databinding.FragmentFeedbackBinding
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class FeedbackFragment : Fragment() {

    private var _b: FragmentFeedbackBinding? = null
    private val b get() = _b!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _b = FragmentFeedbackBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Back arrow
        b.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        // Enable/disable Send button based on message content
        val updateEnabled: () -> Unit = {
            b.btnSend.isEnabled = b.etMessage.text?.toString()?.isNotBlank() == true
        }
        b.etMessage.addTextChangedListener { updateEnabled() }
        b.etSubject.addTextChangedListener { /* keep for future validation */ }
        updateEnabled()

        // IME next from subject -> message
        b.etSubject.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                b.etMessage.requestFocus(); true
            } else false
        }

        b.btnSend.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            sendFeedback()
        }
    }

    private fun sendFeedback() {
        val email = "admin@parkaware.app"
        val subject = b.etSubject.text?.toString().orEmpty()
        val message = b.etMessage.text?.toString().orEmpty()

        // Build a mailto: URI with encoded subject/body (broad client support)
        val enc = { s: String -> URLEncoder.encode(s, StandardCharsets.UTF_8.name()) }
        val mailto = "mailto:$email?subject=${enc(subject)}&body=${enc(message)}"

        // Target only email apps
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse(mailto)
            // Extras are optional; some clients read them, some ignore in favor of the URI.
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, message)
        }

        try {
            // Verify there is at least one handler and launch
            val pm = requireContext().packageManager
            if (intent.resolveActivity(pm) != null) {
                startActivity(Intent.createChooser(intent, "Send email"))
                Toast.makeText(requireContext(), "Opening email app…", Toast.LENGTH_SHORT).show()
                b.etMessage.clearFocus()
                return
            }
        } catch (_: ActivityNotFoundException) {
            // fall through to clipboard fallback
        }

        // Fallback: copy content to clipboard
        val cm = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val fallbackText = buildString {
            appendLine("To: $email")
            if (subject.isNotBlank()) appendLine("Subject: $subject")
            appendLine()
            append(message)
        }
        cm.setPrimaryClip(ClipData.newPlainText("ParkAware feedback", fallbackText))

        Toast.makeText(
            requireContext(),
            "No email app found. Feedback copied to clipboard.",
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onDestroyView() {
        _b = null
        super.onDestroyView()
    }
}