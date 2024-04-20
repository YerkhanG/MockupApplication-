package com.example.maketapplication

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.example.maketapplication.databinding.ActivityMainBinding
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.RequestPoint
import com.yandex.mapkit.RequestPointType
import com.yandex.mapkit.directions.DirectionsFactory
import com.yandex.mapkit.directions.driving.DrivingOptions
import com.yandex.mapkit.directions.driving.DrivingRoute
import com.yandex.mapkit.directions.driving.DrivingRouter
import com.yandex.mapkit.directions.driving.DrivingSession
import com.yandex.mapkit.directions.driving.VehicleOptions
import com.yandex.mapkit.geometry.Geometry
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.layers.ObjectEvent
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
import java.lang.Integer.min

class MainActivity : AppCompatActivity(), UserLocationObjectListener, DrivingSession.DrivingRouteListener, Session.SearchListener,
    CameraListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var loc: UserLocationLayer
    private lateinit var searchManager : SearchManager
    private lateinit var searchSession: Session
    val checkpointinterval = 3000

    private lateinit var mapView : MapView
    private val START_LOCATION = Point(43.246874, 78.896925)
    private val END_LOCATION = Point(42.98443,  78.46577)
    private val CENTER = Point(
        (START_LOCATION.latitude + END_LOCATION.latitude)/2 ,
        (START_LOCATION.longitude + END_LOCATION.longitude)/2
    )
    private var drivingRouter : DrivingRouter? = null
    private var drivingSession : DrivingSession? = null
    private lateinit var mapObjects : MapObjectCollection
    val maxZoom = 13.0f // Maximum allowed zoom level
    val minZoom = 7.0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setApiKey(savedInstanceState)

        MapKitFactory.initialize(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        requestLocationPermission()
        var mapkit = MapKitFactory.getInstance()
        mapView = binding.mapview

        mapView.mapWindow.map.move(START_POSITION)
        loc = mapkit.createUserLocationLayer(mapView.mapWindow)
        loc.isVisible = false
        loc.isHeadingEnabled= true
        loc.setObjectListener(this)


        SearchFactory.initialize(this)
        searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED)


        mapView.map.addCameraListener(this)
        binding.geo.setOnClickListener{
            mapView.map.move(CameraPosition(START_LOCATION,0f,0f,0f))
        }

        binding.searchEdit.setOnEditorActionListener{ y, actionId , event ->
            if(actionId == EditorInfo.IME_ACTION_SEARCH){
                submitQuery(binding.searchEdit.text.toString())
            }
            false
        }

        drivingRouter = DirectionsFactory.getInstance().createDrivingRouter()
        mapObjects = mapView.map.mapObjects.addCollection()

        submitRequest()
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
//    fun calculateCheckpointCoordinates(routeGeometry : Geometry, totalLength : Double , distance : Double ) : Point {
//        val fraction =  distance / totalLength
//        val numpol = routeGeometry.multiPolygon?.polygons?.size ?:0
//        val polygonIndex = (numpol?.times(fraction))?.toInt()
//        val startPoint = polygonIndex?.let { routeGeometry.multiPolygon?.polygons?.getOrNull(it) }
//    }
    private fun calculateLength(route : DrivingRoute): Double {
        var length = 0.0
        for(section in route.sections){
            length += section.metadata.weight.distance.value
        }
        return length
    }
    override fun onDrivingRoutes(routes: MutableList<DrivingRoute>) {
        val route = routes[0]
        mapView.map.move(CameraPosition(START_LOCATION,0f,0f,0f))
        mapObjects.addPolyline(route.geometry)
        val totalLength = calculateLength(route)
        val numOfCheckpoints = (totalLength/checkpointinterval).toInt()
        for(i in 1..numOfCheckpoints){

        }
    }

    override fun onDrivingRoutesError(p0: Error) {
        var errorMessage = "Something went wrong"
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT)
    }

    override fun onSearchResponse(response: Response) {
        val mapObjects:MapObjectCollection = mapView.map.mapObjects
        for(searchResult in response.collection.children){
            val resultLocation  = searchResult.obj?.geometry?.get(0)?.point
//            if(response!=null){
//                mapObjects.addPlacemark(resultLocation , ImageProvider.fromResource(this, R.drawable.img) )
//            }
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
        finished: Boolean) {
        if (cameraPosition.zoom > maxZoom) {
            mapView.map.move(CameraPosition(cameraPosition.target, maxZoom, cameraPosition.azimuth, cameraPosition.tilt))
        } else if (cameraPosition.zoom < minZoom) {
            mapView.map.move(CameraPosition(cameraPosition.target, minZoom, cameraPosition.azimuth, cameraPosition.tilt))
        }
        if(finished){
            submitQuery(binding.searchEdit.text.toString())
        }
    }

    private fun submitRequest(){
        val drivingOptions = DrivingOptions()
        val vehicleOptions = VehicleOptions()
        val requestPoints : ArrayList<RequestPoint> = ArrayList()
        requestPoints.add(RequestPoint(START_LOCATION, RequestPointType.WAYPOINT, null))
        requestPoints.add(RequestPoint(END_LOCATION, RequestPointType.WAYPOINT, null))
        drivingSession = drivingRouter?.requestRoutes(requestPoints, drivingOptions, vehicleOptions, this)
        mapView.map.move(
            CameraPosition(requestPoints[0].point, 10.0f, 150.0f, 0f)
        )
    }
}

