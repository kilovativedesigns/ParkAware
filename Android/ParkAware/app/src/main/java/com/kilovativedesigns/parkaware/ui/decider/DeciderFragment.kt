package com.kilovativedesigns.parkaware.ui.decider

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.kilovativedesigns.parkaware.R
import com.kilovativedesigns.parkaware.data.Prefs
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DeciderFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Tiny empty view; we immediately navigate away.
        return View(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            val authed = FirebaseAuth.getInstance().currentUser != null

            if (!authed) {
                // Not signed in → go to Sign In first
                findNavController().navigate(R.id.signInFragment)
                return@launch
            }

            // Signed in → decide Warning vs Home using DataStore flag
            val seen = Prefs.warningSeenFlow(requireContext()).first()
            val dest = if (seen) R.id.homeFragment else R.id.warningFragment
            findNavController().navigate(dest)
        }
    }
}