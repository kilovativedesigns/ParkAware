package com.kilovativedesigns.parkaware.ui.auth

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.kilovativedesigns.parkaware.R
import com.kilovativedesigns.parkaware.databinding.FragmentSignInBinding
import com.kilovativedesigns.parkaware.util.FcmTokenManager   // ← added

class SignInFragment : Fragment() {

    private var _b: FragmentSignInBinding? = null
    private val b get() = _b!!
    private val auth by lazy { FirebaseAuth.getInstance() }

    private fun setBusy(busy: Boolean) {
        b.progress.isVisible = busy
        b.btnGoogle.isEnabled = !busy
        b.btnContinueGuest.isEnabled = !busy
        if (!busy) b.error.isVisible = false
    }

    private val googleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK || result.data == null) {
            setBusy(false)
            return@registerForActivityResult
        }

        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account: GoogleSignInAccount = task.getResult(ApiException::class.java)
            val token = account.idToken ?: error("No ID token from Google")
            val cred = GoogleAuthProvider.getCredential(token, null)

            auth.signInWithCredential(cred)
                .addOnSuccessListener { goNext() }
                .addOnFailureListener {
                    setBusy(false)
                    showError(getString(R.string.signin_failed_google))
                }
        } catch (_: Exception) {
            setBusy(false)
            showError(getString(R.string.signin_failed_google))
        }
    }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentSignInBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Already signed in? Continue.
        auth.currentUser?.let { goNext(); return }

        b.btnContinueGuest.setOnClickListener {
            setBusy(true)
            auth.signInAnonymously()
                .addOnSuccessListener { goNext() }
                .addOnFailureListener {
                    setBusy(false)
                    showError(getString(R.string.signin_failed_anon))
                }
        }

        b.btnGoogle.setOnClickListener {
            val webClientId = getString(R.string.default_web_client_id)
            if (webClientId.isNullOrBlank() || webClientId == "YOUR_WEB_CLIENT_ID") {
                showError("Missing default_web_client_id. Check Firebase setup.")
                return@setOnClickListener
            }

            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build()
            val client = GoogleSignIn.getClient(requireActivity(), gso)
            setBusy(true)
            googleLauncher.launch(client.signInIntent)
        }

        b.privacyLink.setOnClickListener { openUrl("https://parkaware.app/privacy-policy-1") }
        b.eulaLink.setOnClickListener {
            openUrl("https://www.apple.com/legal/internet-services/itunes/dev/stdeula/")
        }
    }

    private fun goNext() {
        setBusy(false)
        // save token for this signed-in user (safe to call; it checks if user exists)
        FcmTokenManager.storeCurrentTokenIfSignedIn()
        findNavController().navigate(R.id.action_signIn_to_warning)
    }

    private fun showError(msg: String) {
        b.error.text = msg
        b.error.isVisible = true
    }

    private fun openUrl(url: String) {
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    override fun onDestroyView() {
        _b = null
        super.onDestroyView()
    }
}