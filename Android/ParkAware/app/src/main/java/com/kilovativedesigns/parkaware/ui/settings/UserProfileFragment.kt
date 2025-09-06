package com.kilovativedesigns.parkaware.ui.settings

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.kilovativedesigns.parkaware.R
import com.kilovativedesigns.parkaware.databinding.FragmentUserProfileBinding
import java.io.File
import java.io.FileOutputStream

class UserProfileFragment : Fragment() {

    private var _b: FragmentUserProfileBinding? = null
    private val b get() = _b!!

    // pick from gallery
    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { loadAvatarFromUri(it) } }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        _b = FragmentUserProfileBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Top app bar back arrow
        b.topAppBar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        // System back -> behave the same as toolbar back
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            findNavController().navigateUp()
        }

        // clicks
        b.btnChangeAvatar.setOnClickListener { pickImage.launch("image/*") }
        b.btnSignOut.setOnClickListener { signOut() }
        b.btnDeleteAccount.setOnClickListener { confirmDelete() }
        b.btnUpgrade.setOnClickListener {
            // TODO: start Google Play Billing purchase flow
        }
        b.btnRestore.setOnClickListener {
            // TODO: run your BillingClient restore flow
        }

        // simple name validation
        b.etUserName.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val name = b.etUserName.text?.toString()?.trim().orEmpty()
                b.etUserName.error = if (name.length < 2) "Name must be at least 2 characters" else null
            }
        }

        loadUserState()
    }

    // --- state ---------------------------------------------------------------

    private fun loadUserState() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            b.etUserId.setText("Not signed in")
            b.tvPlan.text = "Subscription Status: Free"
            b.etUserName.setText("")
            b.imgAvatar.setImageResource(R.drawable.ic_person_24)
            return
        }

        val id = when {
            user.isAnonymous -> "Guest"
            !user.email.isNullOrBlank() -> user.email!!
            else -> user.uid
        }
        b.etUserId.setText(id)
        b.etUserName.setText(user.displayName ?: "")
        b.tvPlan.text = "Subscription Status: Free" // or Pro
        loadAvatarFromDisk()
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
        val bmp: Bitmap? = requireContext().contentResolver.openInputStream(uri)?.use {
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
        // reset UI
        b.etUserName.setText("")
        b.etUserId.setText("Not signed in")
        b.tvPlan.text = "Subscription Status: Free"
        b.imgAvatar.setImageResource(R.drawable.ic_person_24)

        // OPTIONAL: go back after sign-out
        findNavController().navigateUp()
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
                signOut() // also navigates up
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