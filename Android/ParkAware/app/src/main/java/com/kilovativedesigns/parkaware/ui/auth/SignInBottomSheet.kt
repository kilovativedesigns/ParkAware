package com.kilovativedesigns.parkaware.ui.auth

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.kilovativedesigns.parkaware.R
import com.kilovativedesigns.parkaware.databinding.SheetSignInBinding
import com.kilovativedesigns.parkaware.push.FcmTokenManager

/**
 * Bottom sheet with sign-in options.
 * On success it posts a fragment result (RESULT_KEY) and dismisses.
 */
class SignInBottomSheet : BottomSheetDialogFragment() {

    private var _b: SheetSignInBinding? = null
    private val b get() = _b!!
    private val auth by lazy { FirebaseAuth.getInstance() }

    private fun setBusy(busy: Boolean) {
        b.progress.isVisible = busy
        b.btnGoogle.isEnabled = !busy
        b.btnGuest.isEnabled = !busy
        if (!busy) b.error.isVisible = false
    }

    private val googleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // User cancelled
        if (result.resultCode != Activity.RESULT_OK || result.data == null) {
            setBusy(false); return@registerForActivityResult
        }

        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val acct = task.getResult(ApiException::class.java)
            val token = acct.idToken ?: throw IllegalStateException("No ID token")
            val cred = GoogleAuthProvider.getCredential(token, null)

            auth.signInWithCredential(cred)
                .addOnSuccessListener {
                    // Only persist token for real (non-anon) users
                    FcmTokenManager.storeCurrentTokenIfSignedIn()
                    parentFragmentManager.setFragmentResult(
                        RESULT_KEY, bundleOf(RESULT_OK to true)
                    )
                    dismissAllowingStateLoss()
                }
                .addOnFailureListener {
                    setBusy(false)
                    showError(getString(R.string.signin_failed_google))
                }
        } catch (_: Exception) {
            setBusy(false)
            showError(getString(R.string.signin_failed_google))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _b = SheetSignInBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        b.btnGuest.setOnClickListener {
            setBusy(true)
            auth.signInAnonymously()
                .addOnSuccessListener {
                    // Guest session; we still notify so caller can refresh UI
                    parentFragmentManager.setFragmentResult(
                        RESULT_KEY, bundleOf(RESULT_OK to true)
                    )
                    dismissAllowingStateLoss()
                }
                .addOnFailureListener {
                    setBusy(false)
                    showError(getString(R.string.signin_failed_anon))
                }
        }

        b.btnGoogle.setOnClickListener {
            val webClientId = getString(R.string.default_web_client_id)
            if (webClientId.isBlank() || webClientId == "YOUR_WEB_CLIENT_ID") {
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
    }

    private fun showError(msg: String) {
        b.error.text = msg
        b.error.isVisible = true
    }

    override fun onDestroyView() {
        _b = null
        super.onDestroyView()
    }

    companion object {
        const val RESULT_KEY = "auth_result"
        const val RESULT_OK = "ok" // bool extra in the result bundle
    }
}