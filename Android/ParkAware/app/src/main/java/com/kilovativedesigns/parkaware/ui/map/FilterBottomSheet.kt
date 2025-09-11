package com.kilovativedesigns.parkaware.ui.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kilovativedesigns.parkaware.databinding.SheetFilterBinding

class FilterBottomSheet : BottomSheetDialogFragment() {

    private var _b: SheetFilterBinding? = null
    private val b get() = _b!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        _b = SheetFilterBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Defaults
        b.chip3h.isChecked = true
        b.sliderDistance.value = 10f
        b.cbOfficer.isChecked = true
        b.cbChalk.isChecked = true
        b.cbFines.isChecked = true
        updateDistanceLabel(b.sliderDistance.value)

        // Time range chip group → seconds
        fun selectedTimeSeconds(): Long = when {
            b.chip1h.isChecked  -> 60L * 60L
            b.chip3h.isChecked  -> 60L * 60L * 3L
            b.chip24h.isChecked -> 60L * 60L * 24L
            b.chip7d.isChecked  -> 60L * 60L * 24L * 7L
            b.chip30d.isChecked -> 60L * 60L * 24L * 30L
            else /* All time */ -> 60L * 60L * 24L * 365L
        }

        b.sliderDistance.addOnChangeListener { _, v, _ -> updateDistanceLabel(v) }

        b.btnDone.setOnClickListener {
            parentFragmentManager.setFragmentResult(
                RESULT_KEY,
                android.os.Bundle().apply {
                    putLong(K_TIME_SECS, selectedTimeSeconds())
                    putInt(K_DISTANCE_KM, b.sliderDistance.value.toInt())
                    putBoolean(K_SHOW_OFFICER, b.cbOfficer.isChecked)
                    putBoolean(K_SHOW_CHALK, b.cbChalk.isChecked)
                    putBoolean(K_SHOW_FINES, b.cbFines.isChecked)
                }
            )
            dismissAllowingStateLoss()
        }
    }

    private fun updateDistanceLabel(v: Float) {
        b.tvDistanceLabel.text = "Within ${v.toInt()} km"
    }

    override fun onDestroyView() {
        _b = null
        super.onDestroyView()
    }

    companion object {
        const val RESULT_KEY = "filter_result"
        const val K_TIME_SECS = "time_secs"
        const val K_DISTANCE_KM = "distance_km"
        const val K_SHOW_OFFICER = "show_officer"
        const val K_SHOW_CHALK = "show_chalk"
        const val K_SHOW_FINES = "show_fines"
    }
}
