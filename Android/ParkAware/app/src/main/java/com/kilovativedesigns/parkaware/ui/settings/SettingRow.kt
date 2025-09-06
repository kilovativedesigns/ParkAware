package com.kilovativedesigns.parkaware.ui.settings

import androidx.annotation.DrawableRes

data class SettingRow(
    val title: String,
    @DrawableRes val icon: Int,
    val destinationId: Int?,      // nav graph dest id; null = no navigation
    val gated: Boolean = false
)