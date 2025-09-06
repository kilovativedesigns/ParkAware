package com.kilovativedesigns.parkaware.sightings

import androidx.annotation.DrawableRes
import com.kilovativedesigns.parkaware.R

enum class ReportType(val displayName: String, @DrawableRes val icon: Int) {
    Officer("Parking Officer / Ranger", R.drawable.ic_pin_officer),
    Chalk("Chalk Tyre", R.drawable.ic_pin_chalk),
    Fine("Fine Issued", R.drawable.ic_pin_fine),
}