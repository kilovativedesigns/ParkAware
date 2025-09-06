package com.kilovativedesigns.parkaware.ui.education

import android.app.Application
import android.telephony.TelephonyManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class EducationViewModel(app: Application) : AndroidViewModel(app) {

    private val _stateCode = MutableStateFlow(defaultState())
    val stateCode: StateFlow<String> = _stateCode

    private val _tipsByCategory = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val tipsByCategory: StateFlow<Map<String, List<String>>> = _tipsByCategory

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    /** Call from fragment (once) to ensure remote is loaded. */
    fun ensureLoaded() {
        if (_tipsByCategory.value.isNotEmpty() || _loading.value) return
        loadForState(_stateCode.value)
    }

    /** Allow user to switch state later if needed. */
    fun setState(newState: String) {
        if (newState.equals(_stateCode.value, ignoreCase = true)) return
        _stateCode.value = newState
        loadForState(newState)
    }

    // --- Internal -----------------------------------------------------------

    private fun loadForState(state: String) {
        _loading.value = true
        viewModelScope.launch {
            try {
                _tipsByCategory.value = EducationTipsRepository.getState(state)
            } catch (_: Throwable) {
                _tipsByCategory.value = emptyMap() // fragments will fallback to bundled strings
            } finally {
                _loading.value = false
            }
        }
    }

    private fun defaultState(): String {
        // Heuristic: AU users → try locale or SIM; else NSW as default
        val country = Locale.getDefault().country.uppercase(Locale.getDefault())
        if (country == "AU") {
            // could try TelephonyManager networkCountryIso too
            return "NSW"
        }
        return "NSW"
    }
}