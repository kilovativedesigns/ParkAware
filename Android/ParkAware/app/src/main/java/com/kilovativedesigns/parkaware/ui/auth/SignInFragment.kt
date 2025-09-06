package com.kilovativedesigns.parkaware.ui.auth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
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

class SignInFragment : Fragment() {

    private var _b: FragmentSignInBinding? = null
    private val b get() = _b!!
    private val auth by lazy { FirebaseAuth.getInstance() }

    private val googleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        b.progress.isVisible = false
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account: GoogleSignInAccount = task.getResult(ApiException::class.java)
            val token = account.idToken ?: throw IllegalStateException("No ID token")
            val cred = GoogleAuthProvider.getCredential(token, null)
            auth.signInWithCredential(cred)
                .addOnSuccessListener { goNext() }
                .addOnFailureListener { showError(getString(R.string.signin_failed_google)) }
        } catch (e: Exception) {
            showError(getString(R.string.signin_failed_google))
        }
    }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentSignInBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        b.btnContinueGuest.setOnClickListener {
            b.progress.isVisible = true
            auth.signInAnonymously()
                .addOnSuccessListener { goNext() }
                .addOnFailureListener {
                    b.progress.isVisible = false
                    showError(getString(R.string.signin_failed_anon))
                }
        }

        b.btnGoogle.setOnClickListener {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
            val client = GoogleSignIn.getClient(requireActivity(), gso)
            b.progress.isVisible = true
            googleLauncher.launch(client.signInIntent)
        }

        b.privacyLink.setOnClickListener { openUrl("https://parkaware.app/privacy-policy-1") }
        b.eulaLink.setOnClickListener {
            openUrl("https://www.apple.com/legal/internet-services/itunes/dev/stdeula/")
        }
    }

    private fun goNext() {
        findNavController().navigate(R.id.action_signIn_to_warning)
    }

    private fun showError(msg: String) {
        b.error.text = msg
        b.error.isVisible = true
    }

    private fun openUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    override fun onDestroyView() { _b = null; super.onDestroyView() }
}