package com.kilovativedesigns.parkaware.sightings

import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.kilovativedesigns.parkaware.data.model.Report
import com.kilovativedesigns.parkaware.data.model.SightingsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Date
import java.util.Locale
import kotlin.math.*

class SightingsViewModel : ViewModel() {

    companion object {
        private const val COLLECTION = "reports"
        private const val TIME_FIELD = "timestamp" // may be Firestore Timestamp OR Long
        private const val TAG = "SightingsVM"

        private const val MAX_KM = 20.0
        private const val EARTH_R_KM = 6371.0
    }

    private val db = FirebaseFirestore.getInstance()
    private var reg: ListenerRegistration? = null

    private val _uiState = MutableStateFlow(SightingsUiState())
    val uiState: StateFlow<SightingsUiState> = _uiState

    /** Keep the most recent unfiltered batch so we can re-filter when user location changes. */
    private var lastRawReports: List<Report> = emptyList()

    init { observe() }

    private fun observe() {
        _uiState.value = _uiState.value.copy(loading = true, error = null)

        reg?.remove()
        reg = db.collection(COLLECTION)
            .orderBy(TIME_FIELD, Query.Direction.DESCENDING)
            .limit(200) // fetch more; we'll trim by distance later
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.e(TAG, "snapshot error", err)
                    _uiState.value = _uiState.value.copy(loading = false, error = err.message)
                    return@addSnapshotListener
                }

                Log.d(TAG, "docs=${snap?.size() ?: 0} first=${snap?.documents?.firstOrNull()?.data}")

                lastRawReports = snap?.documents?.map(::snapToReport).orEmpty()

                val filtered = filterByUserLocation(lastRawReports, _uiState.value.userLocation)
                _uiState.value = _uiState.value.copy(
                    reports = filtered,
                    loading = false,
                    error = null
                )
            }
    }

    // ---- tolerant readers --------------------------------------------------

    /** Coerce any of several possible time fields/types to epoch millis. */
    private fun DocumentSnapshot.readMillis(vararg keys: String): Long {
        for (k in keys) {
            val v = get(k) ?: continue
            when (v) {
                is Timestamp -> return v.toDate().time
                is Date      -> return v.time
                is Number    -> return v.toLong()                 // Long/Int/Double
                is String    -> v.toLongOrNull()?.let { return it }
            }
        }
        return 0L
    }

    /** Strong, forgiving read of a double with multiple candidate keys. */
    private fun DocumentSnapshot.readDouble(vararg keys: String): Double? {
        for (k in keys) {
            val any = get(k) ?: continue
            when (any) {
                is Number -> return any.toDouble()
                is String -> any.toDoubleOrNull()?.let { return it }
            }
        }
        return null
    }

    // --- Normalize one doc into our Report model ----------------------------
    private fun snapToReport(d: DocumentSnapshot): Report {
        // time: try new field first, then legacy fallbacks
        val timeMs = d.readMillis(
            "timeReported",    // new (millis)
            TIME_FIELD,        // legacy could be Firestore Timestamp OR Long
            "createdAt",       // sometimes a Timestamp
            "time"             // any other historical field
        )

        val lat = d.readDouble("lat", "latitude")

        // Accept any of the common longitude keys (new/old/mistyped)
        val anyLon = d.readDouble("longitude", "lon", "lng")

        // rangerType may be "rangerType" or "type"
        val rangerType = (d.getString("rangerType") ?: d.getString("type"))
            ?.trim()
            ?.ifBlank { null }
            ?.let { t ->
                if (t.isNotEmpty() && t[0].isLowerCase())
                    t.replaceFirstChar { it.titlecase(Locale.getDefault()) }
                else t
            }

        // Use the parameter names your Report constructor actually has.
        // (No 'longitude' named arg — set both 'lng' and 'lon' to be safe.)
        return Report(
            id = d.id,
            reporterID = d.getString("reporterID"),
            lat = lat,
            lng = anyLon,
            lon = anyLon,
            rangerType = rangerType,
            timeReported = timeMs
        )
    }

    /** Filter reports to within MAX_KM of the given location; if no location, return as-is. */
    private fun filterByUserLocation(source: List<Report>, loc: Location?): List<Report> {
        if (loc == null) return source.take(50) // sane limit if we lack a location
        val ulat = loc.latitude
        val ulng = loc.longitude

        // quick prefilter by bounding box (~cheap) before exact Haversine
        val latDelta = Math.toDegrees(MAX_KM / EARTH_R_KM)
        val lngDelta = Math.toDegrees(MAX_KM / (EARTH_R_KM * cos(Math.toRadians(ulat))))

        val minLat = ulat - latDelta
        val maxLat = ulat + latDelta
        val minLng = ulng - lngDelta
        val maxLng = ulng + lngDelta

        return source
            .asSequence()
            .filter { r ->
                val rlat = r.lat
                val rlng = r.longitude // your Map/adapter read this property
                if (rlat == null || rlng == null) return@filter false
                rlat in minLat..maxLat && rlng in minLng..maxLng
            }
            .filter { r ->
                val rlat = r.lat!!
                val rlng = r.longitude!!
                haversineKm(ulat, ulng, rlat, rlng) <= MAX_KM
            }
            .sortedByDescending { it.timeReported }
            .take(50)
            .toList()
    }

    /** Haversine distance in KM. */
    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_R_KM * c
    }

    /** Optimistically add/replace a report in the current list (newest first). */
    fun addReport(report: Report) {
        // merge into raw list first so future re-filters keep it
        val mergedRaw = lastRawReports.toMutableList().apply {
            val existingIdx = indexOfFirst { it.id == report.id }
            if (existingIdx >= 0) removeAt(existingIdx)
            add(0, report)
        }
        lastRawReports = mergedRaw

        val filtered = filterByUserLocation(mergedRaw, _uiState.value.userLocation)
        _uiState.value = _uiState.value.copy(reports = filtered)
    }

    fun upsertReports(vararg reports: Report) {
        val byId = lastRawReports.associateBy { it.id }.toMutableMap()
        for (r in reports) byId[r.id] = r
        lastRawReports = byId.values.sortedByDescending { it.timeReported }

        val filtered = filterByUserLocation(lastRawReports, _uiState.value.userLocation)
        _uiState.value = _uiState.value.copy(reports = filtered)
    }

    fun setUserLocation(loc: Location?) {
        _uiState.value = _uiState.value.copy(userLocation = loc)
        val filtered = filterByUserLocation(lastRawReports, loc)
        _uiState.value = _uiState.value.copy(reports = filtered)
    }

    override fun onCleared() {
        reg?.remove()
        super.onCleared()
    }
}