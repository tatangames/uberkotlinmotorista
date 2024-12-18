package com.alcaldiasantaananorte.ubermotorista.activities

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.alcaldiasantaananorte.ubermotorista.R
import com.alcaldiasantaananorte.ubermotorista.databinding.ActivityMapBinding
import com.alcaldiasantaananorte.ubermotorista.providers.GeoProvider
import com.example.easywaylocation.EasyWayLocation
import com.example.easywaylocation.Listener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.santaananortemetapan.uberclone.providers.AuthProvider

class MapActivity : AppCompatActivity(), OnMapReadyCallback, Listener {

    private lateinit var binding: ActivityMapBinding
    private var googleMap: GoogleMap? = null
    private var easyWayLocation: EasyWayLocation? = null
    private var myLocationLatLng: LatLng? = null
    private var markerDriver: Marker? = null
    private val geoProvider = GeoProvider()
    private val authProvider = AuthProvider()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.setFlags(
            FLAG_LAYOUT_NO_LIMITS,
            FLAG_LAYOUT_NO_LIMITS
        )

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val locationRequest = LocationRequest.create().apply {
            interval = 0
            fastestInterval = 0
            priority = Priority.PRIORITY_HIGH_ACCURACY
            smallestDisplacement = 1f
        }

        easyWayLocation = EasyWayLocation(this, locationRequest, false, false, this)

        locationPermissions.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))

        binding.btnConnect.setOnClickListener { connectDriver() }
        binding.btnDisconnect.setOnClickListener{ disconnectDriver() }
    }

    val locationPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permission ->
        when {
            permission.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                Log.d("LOCALIZACION", "permiso aceptado")
               // easyWayLocation?.startLocation()
                checkIfDriverIsConnect()
            }
            permission.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                Log.d("LOCALIZACION", "permiso concedido con limitacion")
               // easyWayLocation?.startLocation()
                checkIfDriverIsConnect()
            }
            else -> {
                Log.d("LOCALIZACION", "permiso no aceptado")
            }
        }
    }

    private fun checkIfDriverIsConnect() {
        geoProvider.getLocation(authProvider.getId()).addOnSuccessListener { document ->
            if(document.exists()){
                if(document.contains("l")) { // LATITUD
                   connectDriver()
                }else{
                    showButtonConnect()
                }
            }else{
                showButtonConnect()
            }
        }
    }

    private fun saveLocation(){
        if(myLocationLatLng != null){
            geoProvider.saveLocation(authProvider.getId(), myLocationLatLng!!)
        }
    }

    private fun disconnectDriver(){
        easyWayLocation?.endUpdates()
        if(myLocationLatLng != null){
            geoProvider.removeLocation(authProvider.getId())
            showButtonConnect()
        }
    }

    private fun connectDriver(){
        easyWayLocation?.endUpdates() // otros hilos de ejecucion
        easyWayLocation?.startLocation()
        showButtonDisconnect()
    }

    private fun showButtonConnect(){
        binding.btnDisconnect.visibility = View.GONE
        binding.btnConnect.visibility = View.VISIBLE
    }

    private fun showButtonDisconnect(){
        binding.btnDisconnect.visibility = View.VISIBLE
        binding.btnConnect.visibility = View.GONE
    }


    private fun addMarker(){
        val drawable = ContextCompat.getDrawable(applicationContext, R.drawable.uber_car)
        val markerIcon = getMarkerFromDrawable(drawable!!)
        if(markerDriver != null){
            markerDriver?.remove()
        }
        if(myLocationLatLng != null){
            markerDriver = googleMap?.addMarker(
                MarkerOptions()
                    .position(myLocationLatLng!!)
                    .anchor(0.5f, 0.5f)
                    .flat(true)
                    .icon(markerIcon)
            )
        }
    }

    private fun getMarkerFromDrawable(drawable: Drawable): BitmapDescriptor{
        val canvas = Canvas()
        val bitmap = Bitmap.createBitmap(
            70,
            150,
            Bitmap.Config.ARGB_8888
        )
        canvas.setBitmap(bitmap)
        drawable.setBounds(0,0,70,150)
        drawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }


    override fun onResume() {
        super.onResume()

    }

    override fun onDestroy() {
        super.onDestroy()
        easyWayLocation?.endUpdates()
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.uiSettings?.isZoomControlsEnabled = true
      //  easyWayLocation?.startLocation()

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
        googleMap?.isMyLocationEnabled = false // OCULTAR PUNTO AZUL

    }

    override fun locationOn() {

    }

    override fun currentLocation(location: Location) { // ACTUALIZCION POSICION TIEMPO REAL
        myLocationLatLng = LatLng(location.latitude, location.longitude) // POSICION ACTUAL

        googleMap?.moveCamera(CameraUpdateFactory.newCameraPosition(
            CameraPosition.builder().target(myLocationLatLng!!).zoom(17f).build()
        ))
        addMarker()
        saveLocation()
    }

    override fun locationCancelled() {

    }
}