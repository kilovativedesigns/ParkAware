package com.kilovativedesigns.parkaware.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
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
import com.kilovativedesigns.parkaware.sightings.SightingsViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.appcompat.content.res.AppCompatResources // <-- existing

// ---- NEW IMPORTS (for dark-mode map styling) -------------------------------
import android.content.res.Configuration
import com.google.android.gms.maps.model.MapStyleOptions
// ---------------------------------------------------------------------------

class MapFragment : Fragment() {

    private var _b: FragmentMapBinding? = null
    private val b get() = _b!!
    private var gmap: GoogleMap? = null

    // shared with the sightings list
    private val vm: SightingsViewModel by activityViewModels()

    private val markerMap = mutableMapOf<String, Marker>()
    private val timeFmt = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())

    // Keep our two FABs above the map surface
    private fun liftFabs() {
        val z = 2000f
        b.fabMyLocation.elevation = z
        b.fabSetReminder.elevation = z + 1
        b.fabMyLocation.bringToFront()
        b.fabSetReminder.bringToFront()
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
        // Host/attach the Google Map
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

            // (Call applyMapStyle(map) here if you want, but I did not change logic.)

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

            // draw any existing reports immediately (no camera movement)
            val s0 = vm.uiState.value
            if (s0.reports.isNotEmpty()) updateMarkers(s0.reports)

            // keep fabs above the map surface
            liftFabs()
        }

        // redraw markers when the reports flow updates
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.uiState.collect { state -> updateMarkers(state.reports) }
            }
        }

        // My Location
        b.fabMyLocation.setOnClickListener { enableMyLocationAndCenter() }

        // REPORT: open the bottom sheet with icons
        b.fabSetReminder.setOnClickListener { showReportSheet() }

        // Also lift after first layout to be safe
        b.mapRoot.post { liftFabs() }
    }

    // ------------------------------------------------------------------------
    // Bottom sheet for reporting (with icons + themed colors)
    // ------------------------------------------------------------------------
    private fun showReportSheet() {
        val ctx = requireContext()
        val sheet = BottomSheetDialog(ctx)
        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(16))
        }

        // helper to create one row with custom color + icon  (UPDATED)
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

                // keep original icon artwork/colors and make it large enough
                icon = AppCompatResources.getDrawable(ctx, iconRes)
                iconTint = null
                iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
                iconSize = dp(48)
                iconPadding = dp(14)

                // Themed background + contrasting text
                backgroundTintList = ContextCompat.getColorStateList(ctx, bgColor)
                setTextColor(ContextCompat.getColor(ctx, android.R.color.white))

                insetTop = dp(8)
                insetBottom = dp(8)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(10) }

                setOnClickListener {
                    sheet.dismiss()
                    onClick()
                }
            }
        }

        // Officer button (Warm Coral)
        container.addView(
            row(
                getString(R.string.report_officer),
                R.drawable.ic_pin_officer,
                R.color.warm_coral
            ) { handleReportTap("officer") }
        )

        // Chalk button (Sunset Orange)
        container.addView(
            row(
                getString(R.string.report_chalk),
                R.drawable.ic_pin_chalk,
                R.color.sunset_orange
            ) { handleReportTap("chalk") }
        )

        // Fine button (Dark Teal)
        container.addView(
            row(
                getString(R.string.report_fine),
                R.drawable.ic_pin_fine,
                R.color.dark_teal
            ) { handleReportTap("fine") }
        )

        sheet.setContentView(container)
        sheet.show()
    }

    // ---- NEW HELPERS (dark-mode map styling; not called anywhere yet) ------
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
    // ------------------------------------------------------------------------

    // helper dp conversion (single definition)
    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    // ------------------------------------------------------------------------
    // Permissions + My Location
    // ------------------------------------------------------------------------
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
                map.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 16f)
                )
            }
        }
    }

    // ------------------------------------------------------------------------
    // Reporting (keeps cross-platform Firestore shape)
    // ------------------------------------------------------------------------
    private fun handleReportTap(kind: String) {
        if (!hasLocationPermission()) {
            Snackbar.make(b.mapRoot, "Location permission required to post a sighting.", Snackbar.LENGTH_LONG).show()
            return
        }

        val fused = LocationServices.getFusedLocationProviderClient(requireActivity())

        // 1) Try last known
        fused.lastLocation.addOnSuccessListener { last ->
            if (last != null) {
                postReport(kind, last); return@addOnSuccessListener
            }

            // 2) Fresh current
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
        val nowMs = System.currentTimeMillis() // optional client ms for Android-only uses

        // Write in the SAME shape iOS uses, plus a few compat fields
        val report = hashMapOf(
            // canonical fields used by iOS
            "type" to kind,                  // e.g., "officer" | "chalk" | "fine"
            "lat" to loc.latitude,
            "lon" to loc.longitude,          // iOS uses 'lon'
            "userId" to uid,
            "sent" to true,
            "timestamp" to nowTs,            // Firestore Timestamp
            "createdAt" to nowTs,

            // compatibility duplicates for older readers
            "rangerType" to kind,
            "lng" to loc.longitude,
            "longitude" to loc.longitude,
            "timeReported" to nowMs          // old Android code may look at this
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

    // ------------------------------------------------------------------------
    // Markers
    // ------------------------------------------------------------------------
    private fun updateMarkers(reports: List<Report>) {
        val map = gmap ?: return

        // remove markers for items no longer present
        val ids = reports.map { it.id }.toSet()
        val toRemove = markerMap.keys.filter { it !in ids }
        toRemove.forEach { id -> markerMap.remove(id)?.remove() }

        // add/update current markers
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
}