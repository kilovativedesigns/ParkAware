package com.kilovativedesigns.parkaware.ui.settings

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import com.kilovativedesigns.parkaware.databinding.FragmentSimpleBinding

class NotificationLocationFragment : Fragment() {
    private var _b: FragmentSimpleBinding? = null
    private val b get() = _b!!

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentSimpleBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        b.title.text = "Sighting Settings"
        b.subtitle.text = "Stub screen"
    }

    override fun onDestroyView() { _b = null; super.onDestroyView() }
}