package com.kilovativedesigns.parkaware.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kilovativedesigns.parkaware.R
import com.kilovativedesigns.parkaware.data.model.Report
import com.kilovativedesigns.parkaware.databinding.FragmentMapBinding
import com.kilovativedesigns.parkaware.reminders.ReminderManager
import com.kilovativedesigns.parkaware.sightings.SightingsViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.res.Configuration
import com.kilovativedesigns.parkaware.ui.map.FilterBottomSheet.Companion.K_DISTANCE_KM
import com.kilovativedesigns.parkaware.ui.map.FilterBottomSheet.Companion.K_SHOW_CHALK
import com.kilovativedesigns.parkaware.ui.map.FilterBottomSheet.Companion.K_SHOW_FINES
import com.kilovativedesigns.parkaware.ui.map.FilterBottomSheet.Companion.K_SHOW_OFFICER
import com.kilovativedesigns.parkaware.ui.map.FilterBottomSheet.Companion.K_TIME_SECS
import com.kilovativedesigns.parkaware.ui.map.FilterBottomSheet.Companion.RESULT_KEY
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

class MapFragment : Fragment() {

    private var _b: FragmentMapBinding? = null
    private val b get() = _b!!
    private var gmap: GoogleMap? = null

    // shared with the sightings list
    private val vm: SightingsViewModel by activityViewModels()

    private val markerMap = mutableMapOf<String, Marker>()
    private val timeFmt = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())

    // keep user location for filter distance
    private var lastUserLoc: Location? = null

    // current filter (defaults: 3h, 10km, all types)
    private var filter = FilterState(
        timeSeconds = 60L * 60L * 3L,
        distanceKm = 10,
        showOfficer = true,
        showChalk = true,
        showFines = true
    )

    data class FilterState(
        val timeSeconds: Long,
        val distanceKm: Int,
        val showOfficer: Boolean,
        val showChalk: Boolean,
        val showFines: Boolean
    )

    private fun liftFabs() {
        val z = 2000f
        b.fabMyLocation.elevation = z
        b.fabReport.elevation = z + 1
        b.fabSetReminder.elevation = z + 2
        b.fabFilter.elevation = z + 3

        b.fabMyLocation.bringToFront()
        b.fabReport.bringToFront()
        b.fabSetReminder.bringToFront()
        b.fabFilter.bringToFront()
    }

    private val requestPerms = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grant ->
        val fine = grant[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = grant[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fine || coarse) enableMyLocationAndCenter() else {
            Snackbar.make(
                b.mapRoot,
                "Location permission is required to report sightings.",
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        _b = FragmentMapBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val existing = childFragmentManager.findFragmentById(b.mapContainer.id)
        val mapFrag = if (existing is SupportMapFragment) existing else {
            val f = SupportMapFragment.newInstance()
            childFragmentManager.beginTransaction()
                .replace(b.mapContainer.id, f)
                .commitNow()
            f
        }

        mapFrag.getMapAsync { map ->
            gmap = map
            map.uiSettings.isMyLocationButtonEnabled = false
            map.uiSettings.isCompassEnabled = true
            map.uiSettings.isMapToolbarEnabled = false

            // applyMapStyle(map) // optional

            if (hasLocationPermission()) {
                enableMyLocationAndCenter()
            } else {
                requestPerms.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }

            val s0 = vm.uiState.value
            if (s0.reports.isNotEmpty()) applyFiltersAndUpdateMarkers(s0.reports)

            liftFabs()
        }

        setFragmentResultListener(RESULT_KEY) { _, bundle ->
            val t = bundle.getLong(K_TIME_SECS, filter.timeSeconds)
            val d = bundle.getInt(K_DISTANCE_KM, filter.distanceKm)
            val so = bundle.getBoolean(K_SHOW_OFFICER, filter.showOfficer)
            val sc = bundle.getBoolean(K_SHOW_CHALK, filter.showChalk)
            val sf = bundle.getBoolean(K_SHOW_FINES, filter.showFines)
            filter = filter.copy(
                timeSeconds = t,
                distanceKm = d,
                showOfficer = so,
                showChalk = sc,
                showFines = sf
            )
            applyFiltersAndUpdateMarkers(vm.uiState.value.reports)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.uiState.collect { state -> applyFiltersAndUpdateMarkers(state.reports) }
            }
        }

        b.fabMyLocation.setOnClickListener { enableMyLocationAndCenter() }
        b.fabReport.setOnClickListener { showReportSheet() }
        b.fabSetReminder.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            handleReminderFabTap()
        }
        b.fabFilter.setOnClickListener { openFilterScreen() }

        b.mapRoot.post { liftFabs() }
    }

    private fun handleReminderFabTap() {
        val enabled = LocalPrefs.loadEnabled(requireContext())
        val minutes = LocalPrefs.loadMinutes(requireContext())

        if (!enabled) {
            Snackbar.make(b.mapRoot, "Parking reminders are off.", Snackbar.LENGTH_LONG)
                .setAction("Configure") { findNavController().navigate(R.id.remindersFragment) }
                .show()
            return
        }
        if (minutes <= 0) {
            Snackbar.make(b.mapRoot, "Set a valid reminder time.", Snackbar.LENGTH_LONG)
                .setAction("Configure") { findNavController().navigate(R.id.remindersFragment) }
                .show()
            return
        }

        ReminderManager.schedule(requireContext(), minutes * 60L)
        Snackbar.make(
            b.mapRoot,
            getString(R.string.rem_scheduled_fmt, minutes),
            Snackbar.LENGTH_SHORT
        ).show()
    }

    private fun isNightMode(): Boolean {
        val mode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return mode == Configuration.UI_MODE_NIGHT_YES
    }

    private fun applyMapStyle(map: GoogleMap) {
        val styleRes = if (isNightMode()) R.raw.map_style_dark else R.raw.map_style_light
        runCatching {
            map.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), styleRes))
        }.onFailure {
            android.util.Log.w("MapFragment", "Map style load failed", it)
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun hasLocationPermission(): Boolean {
        val ctx = requireContext()
        val fine = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocationAndCenter() {
        val map = gmap ?: return
        if (!hasLocationPermission()) return
        map.isMyLocationEnabled = true

        val client = LocationServices.getFusedLocationProviderClient(requireActivity())
        client.lastLocation.addOnSuccessListener { loc: Location? ->
            loc?.let {
                lastUserLoc = it
                map.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 16f)
                )
                applyFiltersAndUpdateMarkers(vm.uiState.value.reports)
            }
        }
    }

    private fun showReportSheet() {
        val ctx = requireContext()
        val sheet = BottomSheetDialog(ctx)
        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(16))
        }

        fun row(
            text: String,
            iconRes: Int,
            bgColor: Int,
            onClick: () -> Unit
        ): MaterialButton {
            return MaterialButton(
                ctx, null, com.google.android.material.R.attr.materialButtonOutlinedStyle
            ).apply {
                this.text = text
                icon = AppCompatResources.getDrawable(ctx, iconRes)
                iconTint = null
                iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
                iconSize = dp(48)
                iconPadding = dp(14)
                backgroundTintList = ContextCompat.getColorStateList(ctx, bgColor)
                setTextColor(ContextCompat.getColor(ctx, android.R.color.white))
                insetTop = dp(8)
                insetBottom = dp(8)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(10) }
                setOnClickListener {
                    sheet.dismiss(); onClick()
                }
            }
        }

        container.addView(
            row(getString(R.string.report_officer), R.drawable.ic_pin_officer, R.color.warm_coral) {
                handleReportTap("officer")
            }
        )
        container.addView(
            row(getString(R.string.report_chalk), R.drawable.ic_pin_chalk, R.color.sunset_orange) {
                handleReportTap("chalk")
            }
        )
        container.addView(
            row(getString(R.string.report_fine), R.drawable.ic_pin_fine, R.color.dark_teal) {
                handleReportTap("fine")
            }
        )

        sheet.setContentView(container)
        sheet.show()
    }

    private fun handleReportTap(kind: String) {
        if (!hasLocationPermission()) {
            Snackbar.make(b.mapRoot, "Location permission required to post a sighting.", Snackbar.LENGTH_LONG).show()
            return
        }

        val fused = LocationServices.getFusedLocationProviderClient(requireActivity())

        fused.lastLocation.addOnSuccessListener { last ->
            if (last != null) {
                postReport(kind, last); return@addOnSuccessListener
            }
            val cts = CancellationTokenSource()
            fused.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                .addOnSuccessListener { current ->
                    if (current != null) {
                        postReport(kind, current)
                    } else {
                        Snackbar.make(b.mapRoot, "Couldn't get a current location.", Snackbar.LENGTH_LONG).show()
                    }
                }
                .addOnFailureListener { e ->
                    Snackbar.make(b.mapRoot, "Location error: ${e.message}", Snackbar.LENGTH_LONG).show()
                }
                .addOnCanceledListener {
                    Snackbar.make(b.mapRoot, "Location request was cancelled.", Snackbar.LENGTH_LONG).show()
                }
        }.addOnFailureListener { e ->
            Snackbar.make(b.mapRoot, "Location error: ${e.message}", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun postReport(kind: String, loc: Location) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val nowTs = Timestamp.now()
        val nowMs = System.currentTimeMillis()   // <-- fixed

        val report = hashMapOf(
            "type" to kind,
            "lat" to loc.latitude,
            "lon" to loc.longitude,
            "userId" to uid,
            "sent" to true,
            "timestamp" to nowTs,
            "createdAt" to nowTs,

            // compatibility duplicates
            "rangerType" to kind,
            "lng" to loc.longitude,
            "longitude" to loc.longitude,
            "timeReported" to nowMs
        )

        FirebaseFirestore.getInstance()
            .collection("reports")
            .add(report)
            .addOnSuccessListener { ref ->
                android.util.Log.d("MapFragment", "Report posted docId=${ref.id}")
                Snackbar.make(b.mapRoot, "Thanks for letting the Community know", Snackbar.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                android.util.Log.e("MapFragment", "Report post FAILED", e)
                Snackbar.make(b.mapRoot, "Failed to post: ${e.message}", Snackbar.LENGTH_LONG).show()
            }
    }

    private fun openFilterScreen() {
        FilterBottomSheet().show(parentFragmentManager, "filters")
    }

    private fun applyFiltersAndUpdateMarkers(source: List<Report>) {
        val now = System.currentTimeMillis()
        val cutoff = now - filter.timeSeconds * 1000L
        val loc = lastUserLoc

        val filtered = source.asSequence()
            .filter { r -> r.timeReported == 0L || r.timeReported >= cutoff }
            .filter { r ->
                when (r.rangerType?.lowercase(Locale.getDefault())?.trim()) {
                    "officer", "ranger", "inspector" -> filter.showOfficer
                    "chalk", "chalking", "tyre", "tire" -> filter.showChalk
                    "fine", "ticket" -> filter.showFines
                    else -> true
                }
            }
            .filter { r ->
                if (loc == null) return@filter true
                val lat = r.lat ?: return@filter false
                val lng = r.longitude ?: return@filter false
                haversineKm(loc.latitude, loc.longitude, lat, lng) <= filter.distanceKm
            }
            .toList()

        updateMarkers(filtered)
    }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) *
                cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return R * c
    }

    private fun updateMarkers(reports: List<Report>) {
        val map = gmap ?: return

        val ids = reports.map { it.id }.toSet()
        val toRemove = markerMap.keys.filter { it !in ids }
        toRemove.forEach { id -> markerMap.remove(id)?.remove() }

        for (r in reports) {
            val lat = r.lat ?: continue
            val lng = r.longitude ?: continue
            val pos = LatLng(lat, lng)

            val title = r.rangerType?.ifBlank { "Sighting" } ?: "Sighting"
            val snippet = if (r.timeReported > 0L) timeFmt.format(Date(r.timeReported)) else ""
            val icon = pinForType(r.rangerType)

            val existing = markerMap[r.id]
            if (existing == null) {
                map.addMarker(
                    MarkerOptions().position(pos).title(title).snippet(snippet).icon(icon)
                )?.also { markerMap[r.id] = it }
            } else {
                existing.position = pos
                existing.title = title
                existing.snippet = snippet
                existing.setIcon(icon)
            }
        }
    }

    private fun pinForType(typeRaw: String?): BitmapDescriptor {
        val type = typeRaw?.lowercase(Locale.getDefault())?.trim()
        val res = when (type) {
            "officer", "ranger", "inspector" -> R.drawable.ic_pin_officer
            "chalk", "chalking", "tyre", "tire" -> R.drawable.ic_pin_chalk
            "fine", "ticket" -> R.drawable.ic_pin_fine
            else -> R.drawable.ic_pin_officer
        }
        return vectorToDescriptor(res)
    }

    private fun vectorToDescriptor(resId: Int): BitmapDescriptor {
        return try {
            val d = ContextCompat.getDrawable(requireContext(), resId)
                ?: return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
            val size = resources.getDimensionPixelSize(R.dimen.pin_size)
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            d.setBounds(0, 0, c.width, c.height)
            d.draw(c)
            BitmapDescriptorFactory.fromBitmap(bmp)
        } catch (t: Throwable) {
            android.util.Log.w("MapFragment", "vectorToDescriptor failed; using default", t)
            BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
        }
    }

    override fun onDestroyView() {
        _b = null
        super.onDestroyView()
    }

    private object LocalPrefs {
        private const val FILE = "reminders_prefs"
        private const val K_ENABLED = "enabled"
        private const val K_MINUTES = "minutes"

        fun loadEnabled(ctx: android.content.Context) =
            ctx.getSharedPreferences(FILE, android.content.Context.MODE_PRIVATE)
                .getBoolean(K_ENABLED, false)

        fun loadMinutes(ctx: android.content.Context) =
            ctx.getSharedPreferences(FILE, android.content.Context.MODE_PRIVATE)
                .getInt(K_MINUTES, 30)
    }
}