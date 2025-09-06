
package com.kilovativedesigns.parkaware.education

import androidx.annotation.DrawableRes
import com.kilovativedesigns.parkaware.R

enum class EducationCategory(val title: String, @DrawableRes val icon: Int) {
    OnStreet("On‑street", R.drawable.ic_local_parking),     // SF: parkingsign.circle
    CarParks("Car parks", R.drawable.ic_directions_car),    // SF: car.2.fill
    SchoolZones("School zones", R.drawable.ic_school),      // SF: graduationcap.fill
    DisabledParking("Disabled parking", R.drawable.ic_accessible), // SF: car.fill (alt)
    Fines("Fines", R.drawable.ic_payments),                 // SF: dollarsign.circle
    Help("Help", R.drawable.ic_help)                        // SF: questionmark.circle
}