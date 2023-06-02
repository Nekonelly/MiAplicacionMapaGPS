package com.example.miaplicacionmapagps

import com.directions.route.RouteException
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.directions.route.*
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.internal.OnConnectionFailedListener
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.snackbar.Snackbar


class MainActivity : AppCompatActivity(), GoogleApiClient.OnConnectionFailedListener, OnConnectionFailedListener,OnMapReadyCallback, OnMyLocationButtonClickListener , GoogleMap.OnMyLocationClickListener,RoutingListener {

    private lateinit var map: GoogleMap
    var miUbicacion: Location? = null
    var Inicio: LatLng? = null
    var Fin: LatLng? = null
    var lineas: MutableList<Polyline>? = null
    companion object{
        const val REQUEST_CODE_LOCATION=0
    }
    @SuppressLint("MissingPermission")
    private fun marcador() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val latLng = LatLng(location.latitude, location.longitude)
                    map.addMarker(MarkerOptions().position(latLng).title("Mi posición actual"))
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18f), 4000, null)
                }
            }
    }
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        marcador()
        map.setOnMyLocationButtonClickListener(this)
        activarLocalizacion()
        map!!.setOnMyLocationChangeListener { location ->
            miUbicacion = location
            val ltlng = LatLng(location.latitude, location.longitude)
            val cameraUpdate = CameraUpdateFactory.newLatLngZoom(
                ltlng, 16f
            )
            map!!.animateCamera(cameraUpdate)
        }
        map!!.setOnMapClickListener { latLng ->
            Fin = latLng
            map!!.clear()
            Inicio = LatLng(miUbicacion!!.latitude, miUbicacion!!.longitude)
            //start route finding
            ruta(Inicio, Fin)
        }
    }
    fun ruta(Start: LatLng?, End: LatLng?) {
        if (Start == null || End == null) {
            Toast.makeText(this, "Ruta no encontrada", Toast.LENGTH_LONG)
                .show()
        } else {
            val routing = Routing.Builder().travelMode(AbstractRouting.TravelMode.DRIVING).withListener(this).alternativeRoutes(true).waypoints(Start, End)
                .key("AIzaSyAdjbp2u_fxax3K-zrXsf6idE10Vk3S9uI").build()
            routing.execute()
        }
    }
    override fun onRoutingSuccess(route: ArrayList<Route>, shortestRouteIndex: Int) {
        if (lineas != null) {
            lineas!!.clear()
        }
        val polyOptions = PolylineOptions()
        var InicioDibujado: LatLng? = null
        var FinDibujado: LatLng? = null
        lineas = ArrayList()
        //add route(s) to the map using polyline
        for (i in route.indices) {
            if (i == shortestRouteIndex) {
                polyOptions.color(R.color.black)
                polyOptions.width(7f)
                polyOptions.addAll(route[shortestRouteIndex].points)
                val polyline = map!!.addPolyline(polyOptions)
                InicioDibujado = polyline.points[0]
                val k = polyline.points.size
                FinDibujado = polyline.points[k - 1]
                (lineas as ArrayList<Polyline>).add(polyline)
            } else {
            }
        }

        val startMarker = MarkerOptions()
        startMarker.position(InicioDibujado!!)
        startMarker.title("Mi Localicacion")
        map!!.addMarker(startMarker)

        val endMarker = MarkerOptions()
        endMarker.position(FinDibujado!!)
        endMarker.title("Destino")
        map!!.addMarker(endMarker)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        createMapFragment()
    }
    private fun createMapFragment() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.fragmentMap) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }
    private fun permisoLocalizacion()=ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    private fun activarLocalizacion(){
        if(!::map.isInitialized)return
        if(permisoLocalizacion()){
            map.isMyLocationEnabled=true
        }else{
            respuestaPermiso()
        }
    }

    private fun respuestaPermiso(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION)){
            Toast.makeText(this,"Ncesitas aceptar los permisos",Toast.LENGTH_SHORT).show()
        }else{
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),REQUEST_CODE_LOCATION)
        }
    }

    @SuppressLint("MissingSuperCall", "MissingPermission")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when(requestCode) {
            REQUEST_CODE_LOCATION -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                map.isMyLocationEnabled = true
            } else {
                Toast.makeText(this, "Para darle permisos a la aplicacion ve a ajustes y acepta la localizacion",Toast.LENGTH_SHORT).show()
            }else->{}
        }
    }

    @SuppressLint("MissingPermission")
    override fun onResumeFragments() {
        super.onResumeFragments()
        if(!::map.isInitialized)
        {
            return
        }
        if(!permisoLocalizacion()){
            map.isMyLocationEnabled=false
            Toast.makeText(this,"Necesitas activar los permisos, ve a ajustes",Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRoutingFailure(e: RouteException) {
        val parentLayout = findViewById<View>(android.R.id.content)
        val snackbar = Snackbar.make(parentLayout, e.toString(), Snackbar.LENGTH_LONG)
        snackbar.show()
    }

    override fun onRoutingStart() {
        Toast.makeText(this, "Buscando la mejor ruta", Toast.LENGTH_LONG).show()
    }
    override fun onMyLocationButtonClick(): Boolean {
        return false
    }

    override fun onMyLocationClick(p0: Location) {
        Toast.makeText(this,"Estas en: ${p0.latitude}, ${p0.longitude}",Toast.LENGTH_SHORT).show()
    }
    override fun onRoutingCancelled() {
        ruta(Inicio, Fin)
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        ruta(Inicio, Fin)
    }

}