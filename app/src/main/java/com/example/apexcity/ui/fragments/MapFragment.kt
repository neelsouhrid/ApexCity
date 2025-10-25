package com.example.apexcity.ui.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.apexcity.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MapFragment : Fragment(), OnMapReadyCallback {
    
    private var googleMap: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var selectedLocation: LatLng? = null
    
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true -> {
                enableMyLocation()
            }
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                enableMyLocation()
            }
            else -> {
                Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
        
        view.findViewById<FloatingActionButton>(R.id.fabReportIssue)?.setOnClickListener {
            selectedLocation?.let { location ->
                val bundle = Bundle().apply {
                    putDouble("latitude", location.latitude)
                    putDouble("longitude", location.longitude)
                }
                
                val fragment = ReportIssueSelectionFragment().apply {
                    arguments = bundle
                }
                
                parentFragmentManager.beginTransaction()
                    .replace(R.id.navHostFragment, fragment)
                    .addToBackStack(null)
                    .commit()
            } ?: run {
                Toast.makeText(requireContext(), "Please select a location on the map", Toast.LENGTH_SHORT).show()
            }
        }
        
        view.findViewById<FloatingActionButton>(R.id.fabMyLocation)?.setOnClickListener {
            getCurrentLocation()
        }
    }
    
    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        
        // Set map style and UI settings
        googleMap?.apply {
            uiSettings.isZoomControlsEnabled = true
            uiSettings.isMyLocationButtonEnabled = false
            uiSettings.isCompassEnabled = true
            uiSettings.isMapToolbarEnabled = true
        }
        
        // Check location permission
        if (hasLocationPermission()) {
            enableMyLocation()
        } else {
            requestLocationPermission()
        }
        
        // Set map click listener to select location
        googleMap?.setOnMapClickListener { latLng ->
            selectedLocation = latLng
            googleMap?.clear()
            googleMap?.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title("Selected Location")
            )
        }
        
        // Default location (India)
        val defaultLocation = LatLng(20.5937, 78.9629)
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 5f))
    }
    
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun requestLocationPermission() {
        locationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }
    
    private fun enableMyLocation() {
        if (hasLocationPermission()) {
            try {
                googleMap?.isMyLocationEnabled = true
                getCurrentLocation()
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }
    
    private fun getCurrentLocation() {
        if (!hasLocationPermission()) {
            requestLocationPermission()
            return
        }
        
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    val currentLatLng = LatLng(it.latitude, it.longitude)
                    googleMap?.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f)
                    )
                    selectedLocation = currentLatLng
                    googleMap?.clear()
                    googleMap?.addMarker(
                        MarkerOptions()
                            .position(currentLatLng)
                            .title("Current Location")
                    )
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}