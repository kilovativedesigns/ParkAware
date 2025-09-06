package com.kilovativedesigns.parkaware.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private const val REFERRAL_FRIEND_DAYS = 7
private const val REFERRAL_REFERRER_DAYS = 7
private const val REFERRAL_MONTHLY_CAP = 3  // not used yet, but ready

data class SocialUiState(
    val referralCode: String? = null,
    val referralUrl: String? = null,   // plain string URL
    val error: String? = null,
) {
    val subtitle: String =
        "Give $REFERRAL_FRIEND_DAYS days free. Get $REFERRAL_REFERRER_DAYS days free when they subscribe."
}

class SocialSettingsViewModel : ViewModel() {

    private val _ui = MutableStateFlow(SocialUiState())
    val ui: StateFlow<SocialUiState> = _ui

    fun load() {
        viewModelScope.launch {
            // Simulate network fetch
            try {
                delay(350)
                // TODO: Replace with your backend values
                _ui.value = _ui.value.copy(
                    referralCode = "PA-7FREE",
                    referralUrl = "https://parkaware.app/r/PA-7FREE"
                )
            } catch (t: Throwable) {
                _ui.value = _ui.value.copy(error = t.message ?: "Failed to load referral")
            }
        }
    }

    fun clearError() {
        _ui.value = _ui.value.copy(error = null)
    }

    fun redeem(code: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                delay(400)
                // TODO: call backend; simulate success if non-blank
                val ok = code.isNotBlank()
                onResult(ok, if (ok) null else "Invalid code")
            } catch (t: Throwable) {
                onResult(false, t.message ?: "Redeem failed")
            }
        }
    }
}