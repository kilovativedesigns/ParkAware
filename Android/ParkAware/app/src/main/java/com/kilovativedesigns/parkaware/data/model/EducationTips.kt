package com.kilovativedesigns.parkaware.models

data class EducationTips(
    val NSW: StateTips? = null,
    val VIC: StateTips? = null,
    val QLD: StateTips? = null,
    val SA: StateTips? = null,
    val WA: StateTips? = null,
    val TAS: StateTips? = null,
    val NT: StateTips? = null,
    val ACT: StateTips? = null
)

data class StateTips(
    val onStreet: List<String>? = null,
    val carParks: List<String>? = null,
    val disabledParking: List<String>? = null,
    val schoolZones: List<String>? = null,
    val topFines: List<String>? = null,
    val dealWithFines: List<String>? = null
)