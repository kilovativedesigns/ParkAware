package com.kilovativedesigns.parkaware.sightings

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.location.FusedLocationProviderClient
import com.kilovativedesigns.parkaware.databinding.FragmentSightingsBinding
import com.kilovativedesigns.parkaware.ui.sightings.ReportAdapter
import com.kilovativedesigns.parkaware.util.collectWithLifecycle

class SightingsFragment : Fragment() {

    private var _b: FragmentSightingsBinding? = null
    private val b get() = _b!!

    private val vm: SightingsViewModel by activityViewModels()

    private lateinit var adapter: ReportAdapter
    private lateinit var fused: FusedLocationProviderClient
    private var lastLocation: Location? = null

    // Balanced accuracy, ~10s, 10m min distance
    private val locationRequest: LocationRequest =
        LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 10_000L)
            .setMinUpdateIntervalMillis(5_000L)
            .setMinUpdateDistanceMeters(10f)
            .build()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val loc = result.lastLocation ?: return
            lastLocation = loc
            // rebind to update distance column
            adapter.submit(vm.uiState.value.reports, lastLocation)
        }
    }

    // Permission launcher
    private val requestPerms = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grant ->
        val fine = grant[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = grant[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fine || coarse) startLocationUpdates()
        // else you can show your "grant location" state here if desired
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        _b = FragmentSightingsBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        fused = LocationServices.getFusedLocationProviderClient(requireActivity())

        // Recycler
        adapter = ReportAdapter(onClick = { /* TODO: open details/map */ })
        b.list.layoutManager = LinearLayoutManager(requireContext())
        b.list.adapter = adapter

        // Observe reports — pass the Fragment, not the LifecycleOwner
        vm.uiState.collectWithLifecycle(this) { state ->
            adapter.submit(state.reports, lastLocation)
            b.list.visibility = View.VISIBLE
        }

        ensurePermissionThenStart()
    }

    private fun ensurePermissionThenStart() {
        if (hasLocationPermission()) {
            startLocationUpdates()
        } else {
            requestPerms.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun hasLocationPermission(): Boolean {
        val ctx = requireContext()
        val fine = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    private fun startLocationUpdates() {
        try {
            fused.requestLocationUpdates(locationRequest, locationCallback, requireActivity().mainLooper)
            // Seed with last known location for immediate distances
            fused.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    lastLocation = loc
                    adapter.submit(vm.uiState.value.reports, lastLocation)
                }
            }
        } catch (_: SecurityException) {
            // Permissions not granted yet; request flow covers this
        }
    }

    private fun stopLocationUpdates() {
        if (this::fused.isInitialized) {
            fused.removeLocationUpdates(locationCallback)
        }
    }

    override fun onStart() {
        super.onStart()
        if (hasLocationPermission()) startLocationUpdates()
    }

    override fun onStop() {
        stopLocationUpdates()
        super.onStop()
    }

    override fun onDestroyView() {
        _b = null
        super.onDestroyView()
    }
}