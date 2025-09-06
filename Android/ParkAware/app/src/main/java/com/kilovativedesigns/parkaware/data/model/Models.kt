package com.kilovativedesigns.parkaware.data.model

import android.location.Location

data class Report(
    val id: String = "",
    val reporterID: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,   // new schema
    val lon: Double? = null,   // old schema
    val rangerType: String? = null,
    val timeReported: Long = 0L,
    // ...other fields
) {
    val longitude: Double? get() = lng ?: lon
}

data class SightingsUiState(
    val reports: List<Report> = emptyList(),
    val userLocation: Location? = null,
    val loading: Boolean = true,
    val error: String? = null
)