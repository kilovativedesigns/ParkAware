package com.kilovativedesigns.parkaware.ui.settings

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.kilovativedesigns.parkaware.R
import com.kilovativedesigns.parkaware.databinding.FragmentUserProfileBinding
import java.io.File
import java.io.FileOutputStream

class UserProfileFragment : Fragment() {

    private var _b: FragmentUserProfileBinding? = null
    private val b get() = _b!!

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { loadAvatarFromUri(it) } }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        _b = FragmentUserProfileBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // System back -> up
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            findNavController().navigateUp()
        }

        // Change avatar
        b.btnChangeAvatar.setOnClickListener { pickImage.launch("image/*") }

        // Single toggle: Sign In when signed out/anonymous, Sign Out when fully signed in
        b.btnAuth.setOnClickListener {
            val user = FirebaseAuth.getInstance().currentUser
            val needsSignIn = user == null || user.isAnonymous
            if (needsSignIn) {
                navigateToSignIn()
            } else {
                signOut()
                updateAuthUi()
            }
        }

        b.btnDeleteAccount.setOnClickListener { confirmDelete() }
        b.btnUpgrade.setOnClickListener { /* TODO billing */ }
        b.btnRestore.setOnClickListener { /* TODO restore */ }

        // Name validation
        b.etUserName.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val name = b.etUserName.text?.toString()?.trim().orEmpty()
                b.etUserName.error = if (name.length < 2) "Name must be at least 2 characters" else null
            }
        }

        loadUserState()
    }

    // ---- nav to Sign In on the ROOT controller (the one in MainActivity) ---
    private fun navigateToSignIn() {
        val rootNav: NavController = (
                requireActivity()
                    .supportFragmentManager
                    .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                ).navController

        when {
            rootNav.graph.findNode(R.id.signInFragment) != null -> {
                rootNav.navigate(
                    R.id.signInFragment,
                    null,
                    navOptions {
                        // Keep the back stack sane (optional)
                        popUpTo(R.id.homeFragment) { inclusive = false }
                    }
                )
            }
            rootNav.graph.findNode(R.id.deciderFragment) != null -> {
                // Fallback path: Decider will route to SignIn if needed
                rootNav.navigate(
                    R.id.deciderFragment,
                    null,
                    navOptions { popUpTo(R.id.homeFragment) { inclusive = false } }
                )
            }
            else -> {
                Toast.makeText(requireContext(), "Sign-in flow not found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- state ---------------------------------------------------------------
    private fun loadUserState() {
        val user = FirebaseAuth.getInstance().currentUser

        if (user == null || user.isAnonymous) {
            b.etUserId.setText(if (user?.isAnonymous == true) "Guest" else "Not signed in")
            b.tvPlan.text = "Subscription Status: Free"
            b.etUserName.setText("")
            b.imgAvatar.setImageResource(R.drawable.ic_person_24)
            updateAuthUi()
            return
        }

        val id = user.email?.takeIf { it.isNotBlank() } ?: user.uid
        b.etUserId.setText(id)
        b.etUserName.setText(user.displayName ?: "")
        b.tvPlan.text = "Subscription Status: Free"
        loadAvatarFromDisk()
        updateAuthUi()
    }

    private fun updateAuthUi() {
        val user = FirebaseAuth.getInstance().currentUser
        val signedIn = user != null && !user.isAnonymous
        b.btnAuth.text = if (signedIn) "Sign Out" else "Sign In"
        b.btnDeleteAccount.isEnabled = signedIn
        b.btnDeleteAccount.alpha = if (signedIn) 1f else 0.5f
    }

    // --- avatar i/o ----------------------------------------------------------
    private fun avatarFile(): File {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "guest"
        return File(requireContext().filesDir, "$uid.avatar.jpg")
    }

    private fun loadAvatarFromDisk() {
        val f = avatarFile()
        if (f.exists()) {
            BitmapFactory.decodeFile(f.absolutePath)?.let { b.imgAvatar.setImageBitmap(it) }
        } else {
            b.imgAvatar.setImageResource(R.drawable.ic_person_24)
        }
    }

    private fun loadAvatarFromUri(uri: Uri) {
        val bmp = requireContext().contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it)
        }
        if (bmp != null) {
            b.imgAvatar.setImageBitmap(bmp)
            FileOutputStream(avatarFile()).use { out ->
                bmp.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
        }
    }

    // --- auth actions --------------------------------------------------------
    private fun signOut() {
        FirebaseAuth.getInstance().signOut()
        b.etUserName.setText("")
        b.etUserId.setText("Not signed in")
        b.tvPlan.text = "Subscription Status: Free"
        b.imgAvatar.setImageResource(R.drawable.ic_person_24)
    }

    private fun confirmDelete() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Account?")
            .setMessage("This action is permanent and will delete all your data and preferences.")
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton("Delete") { _, _ -> deleteAccount() }
            .show()
    }

    private fun deleteAccount() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        b.btnDeleteAccount.isEnabled = false
        user.delete()
            .addOnSuccessListener {
                avatarFile().delete()
                signOut()
                updateAuthUi()
            }
            .addOnFailureListener {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Action requires recent login")
                    .setMessage("Please sign in again and retry account deletion.")
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            }
            .addOnCompleteListener { b.btnDeleteAccount.isEnabled = true }
    }

    override fun onDestroyView() {
        _b = null
        super.onDestroyView()
    }
}