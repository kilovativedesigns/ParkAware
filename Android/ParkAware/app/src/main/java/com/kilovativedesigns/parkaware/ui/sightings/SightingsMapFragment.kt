package com.kilovativedesigns.parkaware.ui.sightings

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.kilovativedesigns.parkaware.R
import com.kilovativedesigns.parkaware.data.model.Report
import com.kilovativedesigns.parkaware.sightings.SightingsViewModel
import com.kilovativedesigns.parkaware.util.collectWithLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SightingsMapFragment : Fragment() {

    private val vm: SightingsViewModel by activityViewModels()

    private var gmap: GoogleMap? = null
    private val markerMap = mutableMapOf<String, Marker>()
    private val timeFmt = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_map_container, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var mapFrag = childFragmentManager.findFragmentById(R.id.mapContainer) as? SupportMapFragment
        if (mapFrag == null) {
            mapFrag = SupportMapFragment.newInstance()
            childFragmentManager.beginTransaction()
                .replace(R.id.mapContainer, mapFrag)
                .commitNow()
        }

        mapFrag.getMapAsync { map ->
            gmap = map
            enableMyLocationIfAllowed(map)
            val s0 = vm.uiState.value
            if (s0.reports.isNotEmpty()) updateMarkers(s0.reports, s0.userLocation != null)
        }

        vm.uiState.collectWithLifecycle(this) { state ->
            updateMarkers(state.reports, state.userLocation != null)
        }
    }

    private fun enableMyLocationIfAllowed(map: GoogleMap) {
        val ctx = requireContext()
        val fine = ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
            map.isMyLocationEnabled = true
        }
    }

    private fun updateMarkers(reports: List<Report>, haveUserLoc: Boolean) {
        val map = gmap ?: return

        val ids = reports.map { it.id }.toSet()
        val toRemove = markerMap.keys.filter { it !in ids }
        toRemove.forEach { id -> markerMap.remove(id)?.remove() }

        for (r in reports) {
            val lat = r.lat ?: continue
            val lng = r.lng ?: continue
            val pos = LatLng(lat, lng)

            val title = r.rangerType?.ifBlank { "Sighting" } ?: "Sighting"
            val snippet = if (r.timeReported > 0L) timeFmt.format(Date(r.timeReported)) else ""
            val icon = pinForType(r.rangerType)

            val existing = markerMap[r.id]
            if (existing == null) {
                val m = map.addMarker(
                    MarkerOptions()
                        .position(pos)
                        .title(title)
                        .snippet(snippet)
                        .icon(icon)
                )
                if (m != null) markerMap[r.id] = m
            } else {
                existing.position = pos
                existing.title = title
                existing.snippet = snippet
                existing.setIcon(icon)
            }
        }

        if (markerMap.isNotEmpty()) {
            val b = LatLngBounds.Builder()
            markerMap.values.forEach { b.include(it.position) }
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(b.build(), 64))
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
        val d = ContextCompat.getDrawable(requireContext(), resId)!!
        val w = d.intrinsicWidth
        val h = d.intrinsicHeight
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        d.setBounds(0, 0, c.width, c.height)
        d.draw(c)
        return BitmapDescriptorFactory.fromBitmap(bmp)
    }
}

