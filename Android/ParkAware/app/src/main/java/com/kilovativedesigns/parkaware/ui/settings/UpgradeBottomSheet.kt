package com.kilovativedesigns.parkaware.ui.settings

import android.os.Bundle
import android.view.*
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kilovativedesigns.parkaware.databinding.SheetUpgradeBinding
import com.kilovativedesigns.parkaware.subscription.SubscriptionManager

class UpgradeBottomSheet : BottomSheetDialogFragment() {
    private var _b: SheetUpgradeBinding? = null
    private val b get() = _b!!

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = SheetUpgradeBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        b.btnclose.setOnClickListener { dismiss() }
        b.btnsubscribe.setOnClickListener {
            // TODO: integrate Billing; demo unlock
            SubscriptionManager.isSubscribed = true
            dismiss()
        }
    }

    override fun onDestroyView() { _b = null; super.onDestroyView() }
}