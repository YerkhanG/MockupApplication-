package com.example.maketapplication

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.PointF
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.example.maketapplication.databinding.ActivityMainBinding
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.directions.driving.DrivingRoute
import com.yandex.mapkit.directions.driving.DrivingSession
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.layers.ObjectEvent
import com.yandex.mapkit.location.FilteringMode
import com.yandex.mapkit.location.LocationListener
import com.yandex.mapkit.map.CameraListener
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.CameraUpdateReason
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.VisibleRegionUtils
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.search.Response
import com.yandex.mapkit.search.SearchFactory
import com.yandex.mapkit.search.SearchManager
import com.yandex.mapkit.search.SearchManagerType
import com.yandex.mapkit.search.SearchOptions
import com.yandex.mapkit.search.Session
import com.yandex.mapkit.user_location.UserLocationLayer
import com.yandex.mapkit.user_location.UserLocationObjectListener
import com.yandex.mapkit.user_location.UserLocationView
import com.yandex.runtime.Error
import com.yandex.runtime.image.ImageProvider
import com.yandex.runtime.network.NetworkError
import com.yandex.runtime.network.RemoteError

class MainActivity : AppCompatActivity(), UserLocationObjectListener, DrivingSession.DrivingRouteListener, Session.SearchListener,
    CameraListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var loc: UserLocationLayer
    private lateinit var searchManager : SearchManager
    private lateinit var searchSession: Session
    private lateinit var mapView : MapView
    private val START_LOCATION = Point(43.226667, 76.845202)
    private val END_LOCATION = Point(43.254418, 76.902173)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setApiKey(savedInstanceState)

        MapKitFactory.initialize(this)



        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        requestLocationPermission()
        var mapkit = MapKitFactory.getInstance()
        mapkit.resetLocationManagerToDefault()
        mapView = binding.mapview
        mapView.mapWindow.map.move(START_POSITION)
        loc = mapkit.createUserLocationLayer(mapView.mapWindow)
        loc.isVisible = true
        loc.isHeadingEnabled= true
        loc.setObjectListener(this)


        SearchFactory.initialize(this)
        searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED)


        mapView.map.addCameraListener(this)

        binding.searchEdit.setOnEditorActionListener{ y, actionId , event ->
            if(actionId == EditorInfo.IME_ACTION_SEARCH){
                submitQuery(binding.searchEdit.text.toString())
            }
            false
        }
    }

    private fun requestLocationPermission(){
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION),0)
            return
        }

    }

    private fun submitQuery(query : String ){
        searchSession = searchManager.submit(query,VisibleRegionUtils.toPolygon(mapView.map.visibleRegion) , SearchOptions() , this )
    }


    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        mapView.onStart()
    }

    override fun onStop() {
        mapView.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }
    private fun setApiKey(savedInstanceState: Bundle?) {
        MapKitFactory.setApiKey(MAPKIT_API_KEY)
    }
    companion object {
        private val START_POSITION = CameraPosition(Point(43.2380, 76.8829), 13.0f, 0f, 0f)

        private val DEFAULT_POINTS = listOf(
            Point(59.929576, 30.291737),
            Point(59.954093, 30.305770),
        )
        const val MAPKIT_API_KEY = "498f109c-3ad2-4315-9d18-f2a361edbbfc"
    }

    override fun onObjectAdded(userLocationView: UserLocationView) {
//        loc.setAnchor(PointF((mapView.width * 0.5).toFloat(),(mapView.height * 0.5).toFloat()),
//            PointF((mapView.width * 0.5).toFloat(),(mapView.height * 0.83).toFloat()))
//        loc.isAutoZoomEnabled = true
    }

    override fun onObjectRemoved(p0: UserLocationView) {

    }

    override fun onObjectUpdated(p0: UserLocationView, p1: ObjectEvent) {

    }

    override fun onDrivingRoutes(p0: MutableList<DrivingRoute>) {
        TODO("Not yet implemented")
    }

    override fun onDrivingRoutesError(p0: Error) {
        TODO("Not yet implemented")
    }

    override fun onSearchResponse(response: Response) {
        val mapObjects:MapObjectCollection = mapView.map.mapObjects
        mapObjects.clear()
        for(searchResult in response.collection.children){
            val resultLocation  = searchResult.obj!!.geometry[0].point!!
            if(response!=null){
                mapObjects.addPlacemark(resultLocation , ImageProvider.fromResource(this, R.drawable.img) )
            }
        }
    }

    override fun onSearchError(error: Error) {
        var errorMessage = "Something went wrong"
        if(error is RemoteError){
            errorMessage = "Something went wrong"
        }else if(error is NetworkError){
            errorMessage = "Something went wrong"
        }
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT)
    }

    override fun onCameraPositionChanged(
        map: Map,
        cameraPosition: CameraPosition,
        cameraUpdateReason: CameraUpdateReason,
        finished: Boolean
    ) {
        if(finished){
            submitQuery(binding.searchEdit.text.toString())
        }
    }
}

