package com.mindorks.ridesharing.ui.maps

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.mindorks.ridesharing.R
import com.mindorks.ridesharing.data.network.NetworkService
import com.mindorks.ridesharing.utils.MapUtils
import com.mindorks.ridesharing.utils.PermissionUtils
import com.mindorks.ridesharing.utils.ViewUtils

class MapsActivity : AppCompatActivity(), MapsView, OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    lateinit var presenter: MapsPresenter
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private lateinit var locationCallback: LocationCallback
    private var currentLatLng: LatLng? = null
    private var nearByCabsMarkerList = arrayListOf<Marker>()

    companion object {
        private val LOCATION_PERMISSION_REQUEST_CODE = 999
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        ViewUtils.enableTransparentStatusBar(window)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        presenter = MapsPresenter(NetworkService())
        presenter.onAttach(this)
    }

    private fun moveCamera(latLng: LatLng?) {
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng))
    }

    private fun animateCamera(latLng: LatLng?) {
        val cameraPosition = CameraPosition.builder().target(latLng).zoom(15.5f).build()
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
    }

    private fun enableMyLocationOnMap() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            return
        }
        googleMap.setPadding(0, ViewUtils.dpToPx(48f), 0, 0)
        googleMap.isMyLocationEnabled = true
    }

    private fun setUpLocationListener() {
        fusedLocationProviderClient = FusedLocationProviderClient(this)
        val locationRequest = LocationRequest().setInterval(2000).setFastestInterval(2000)
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                if (currentLatLng == null) {
                    for (location in locationResult.locations) {
                        if (currentLatLng == null) {
                            currentLatLng = LatLng(location.latitude, location.longitude)
                            enableMyLocationOnMap()
                            moveCamera(currentLatLng)
                            animateCamera(currentLatLng)
                            presenter.requestNearByCabs(currentLatLng!!)
                        }
                    }
                }
            }
        }
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationProviderClient!!.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.myLooper()
        )
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
    }

    override fun onStart() {
        super.onStart()
        when {
            PermissionUtils.isAccessFineLocationGranted(this) -> {
                when {
                    PermissionUtils.isLocationEnabled(this) -> {
                        //We can now fetch Location
                        setUpLocationListener()
                    }
                    else -> {
                        PermissionUtils.showGpsNotEnabledDialog(this)
                    }
                }
            }
            else ->
                PermissionUtils.requestAccessFineLocationPermission(
                    this,
                    LOCATION_PERMISSION_REQUEST_CODE
                )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            when (requestCode) {
                LOCATION_PERMISSION_REQUEST_CODE -> {
                    when {
                        PermissionUtils.isLocationEnabled(this) -> {
                            //We can now fetch Location
                            setUpLocationListener()

                        }
                        else -> {
                            PermissionUtils.showGpsNotEnabledDialog(this)
                        }
                    }
                }
            }
        } else {
            Toast.makeText(this, "Please Grant Location Permission", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onDetach()
    }

    fun addCarMarkerAndGet(latLng: LatLng): Marker {
        val bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(MapUtils.getCarBitMap(this))
        return googleMap.addMarker(
            MarkerOptions().position(latLng).icon(bitmapDescriptor).flat(true)
        )
    }

    override fun showNearByCabs(latLngList: List<LatLng>) {
        nearByCabsMarkerList.clear()
        for (latLng in latLngList) {
            nearByCabsMarkerList.add(addCarMarkerAndGet(latLng))
        }
    }
}
