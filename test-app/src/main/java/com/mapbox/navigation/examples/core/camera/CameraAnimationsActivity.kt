package com.mapbox.navigation.examples.core.camera

import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapLoadError
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.Style.Companion.MAPBOX_STREETS
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.CircleLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.getCameraAnimationsPlugin
import com.mapbox.maps.plugin.delegates.listeners.OnCameraChangeListener
import com.mapbox.maps.plugin.delegates.listeners.OnMapLoadErrorListener
import com.mapbox.maps.plugin.gestures.GesturesPlugin
import com.mapbox.maps.plugin.gestures.OnMapLongClickListener
import com.mapbox.maps.plugin.gestures.getGesturesPlugin
import com.mapbox.maps.plugin.location.LocationComponentActivationOptions
import com.mapbox.maps.plugin.location.LocationPluginImpl
import com.mapbox.maps.plugin.location.LocationUpdate
import com.mapbox.maps.plugin.location.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.location.getLocationPlugin
import com.mapbox.maps.plugin.location.modes.RenderMode
import com.mapbox.navigation.base.internal.extensions.applyDefaultParams
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.MapMatcherResult
import com.mapbox.navigation.core.trip.session.MapMatcherResultObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.examples.core.R
import com.mapbox.navigation.examples.core.camera.AnimationAdapter.OnAnimationButtonClicked
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSourceOptions
import com.mapbox.navigation.ui.maps.camera.lifecycle.NavigationScaleGestureActionListener
import com.mapbox.navigation.ui.maps.camera.lifecycle.NavigationScaleGestureHandler
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine
import com.mapbox.navigation.utils.internal.ifNonNull
import com.mapbox.turf.TurfMeasurement
import kotlinx.android.synthetic.main.layout_camera_animations.*

class CameraAnimationsActivity :
    AppCompatActivity(),
    PermissionsListener,
    OnAnimationButtonClicked,
    OnMapLongClickListener {

    private val permissionsManager = PermissionsManager(this)
    private var locationComponent: LocationPluginImpl? = null
    private lateinit var mapboxMap: MapboxMap
    private lateinit var mapboxNavigation: MapboxNavigation
    private val replayRouteMapper = ReplayRouteMapper()
    private val mapboxReplayer = MapboxReplayer()

    private var routeLineAPI: MapboxRouteLineApi? = null
    private val routeArrowAPI: MapboxRouteArrowApi = MapboxRouteArrowApi()
    private var routeLineView: MapboxRouteLineView? = null
    private var routeArrowView: MapboxRouteArrowView? = null

    private lateinit var navigationCamera: NavigationCamera
    private lateinit var viewportDataSource: MapboxNavigationViewportDataSource

    private val pixelDensity = Resources.getSystem().displayMetrics.density
    private val overviewEdgeInsets: EdgeInsets by lazy {
        EdgeInsets(
            10.0 * pixelDensity,
            10.0 * pixelDensity,
            10.0 * pixelDensity,
            10.0 * pixelDensity
        )
    }
    private val followingEdgeInsets: EdgeInsets by lazy {
        EdgeInsets(
            mapboxMap.getSize().height.toDouble() * 2.0 / 3.0,
            0.0 * pixelDensity,
            0.0 * pixelDensity,
            0.0 * pixelDensity
        )
    }

    private var lookAtPoint: Point? = null
        set(value) {
            field = value
            if (value != null) {
                poiSource.geometry(value)
            } else {
                poiSource.featureCollection(FeatureCollection.fromFeatures(emptyList()))
            }
        }
    private val poiLayer = CircleLayer("circle_layer", "circle_source")
        .circleColor(Color.RED)
        .circleRadius(10.0)
    private val poiSource = GeoJsonSource(
        GeoJsonSource.Builder("circle_source").data("")
    )

    private val mapMatcherResultObserver = object : MapMatcherResultObserver {
        override fun onNewMapMatcherResult(mapMatcherResult: MapMatcherResult) {
            val locationUpdate = LocationUpdate(
                location = mapMatcherResult.enhancedLocation,
                intermediatePoints = null, // fixme mapMatcherResult.keyPoints.dropLast(1),
                animationDuration = 1000L
            )
            locationComponent?.forceLocationUpdate(locationUpdate)
            viewportDataSource.onLocationChanged(mapMatcherResult.enhancedLocation)

            lookAtPoint?.run {
                val point = Point.fromLngLat(
                    mapMatcherResult.enhancedLocation.longitude,
                    mapMatcherResult.enhancedLocation.latitude,
                )
                val bearing = TurfMeasurement.bearing(point, this)
                viewportDataSource.followingBearingPropertyOverride(bearing)
            }

            viewportDataSource.evaluate()
            if (mapMatcherResult.isTeleport) {
                navigationCamera.resetFrame()
            }
        }
    }

    private val routeProgressObserver = object : RouteProgressObserver {
        override fun onRouteProgressChanged(routeProgress: RouteProgress) {
            viewportDataSource.onRouteProgressChanged(routeProgress)
            viewportDataSource.evaluate()

            routeArrowAPI.updateUpcomingManeuverArrow(routeProgress).apply {
                ifNonNull(routeArrowView, mapboxMap.getStyle()) { view, style ->
                    view.render(style, this)
                }
            }
        }
    }

    private val routesObserver = object : RoutesObserver {
        override fun onRoutesChanged(routes: List<DirectionsRoute>) {
            if (routes.isNotEmpty()) {
                routeLineAPI?.setRoutes(listOf(RouteLine(routes[0], null)))?.apply {
                    ifNonNull(routeLineView, mapboxMap.getStyle()) { view, style ->
                        view.render(style, this)
                    }
                }
                startSimulation(routes[0])
                viewportDataSource.onRouteChanged(routes.first())
                viewportDataSource.overviewPaddingPropertyOverride(overviewEdgeInsets)
                viewportDataSource.evaluate()
                navigationCamera.requestNavigationCameraToOverview()
            } else {
                navigationCamera.requestNavigationCameraToIdle()
            }
        }
    }

    private val onIndicatorPositionChangedListener = object : OnIndicatorPositionChangedListener {
        override fun onIndicatorPositionChanged(point: Point) {
            routeLineAPI?.updateTraveledRouteLine(point)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_camera_animations)
        mapboxMap = mapView.getMapboxMap()

        initNavigation()

        viewportDataSource = MapboxNavigationViewportDataSource(
            MapboxNavigationViewportDataSourceOptions.Builder().build(),
            mapView.getMapboxMap()
        )
        navigationCamera = NavigationCamera(
            mapView.getMapboxMap(),
            mapView.getCameraAnimationsPlugin(),
            viewportDataSource
        )
        /* Alternative to the NavigationScaleGestureHandler
        mapView.getCameraAnimationsPlugin().addCameraAnimationsLifecycleListener(
            NavigationBasicGesturesHandler(navigationCamera)
        )*/
        mapView.getCameraAnimationsPlugin().addCameraAnimationsLifecycleListener(
            NavigationScaleGestureHandler(
                this,
                navigationCamera,
                mapboxMap,
                getGesturesPlugin(),
                getLocationComponent(),
                object : NavigationScaleGestureActionListener {
                    override fun onNavigationScaleGestureAction() {
                        viewportDataSource.followingZoomUpdatesAllowed = false
                    }
                }
            ).apply { initialize() }
        )

        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            init()
        } else {
            permissionsManager.requestLocationPermissions(this)
        }
    }

    @SuppressLint("MissingPermission")
    private fun init() {
        initRouteLine()
        initAnimations()
        initStyle()
        initCameraListeners()
        initButtons()
        mapboxNavigation.startTripSession()
    }

    private fun initRouteLine() {
        val mapboxRouteLineOptions = MapboxRouteLineOptions.Builder(this).build()
        routeLineAPI = MapboxRouteLineApi(mapboxRouteLineOptions)
        routeLineView = MapboxRouteLineView(mapboxRouteLineOptions)

        val routeArrowOptions = RouteArrowOptions.Builder(this).build()
        routeArrowView = MapboxRouteArrowView(routeArrowOptions)
    }

    private fun initButtons() {
        gravitate_left.setOnClickListener {
            mapboxMap.getCameraOptions(null).padding?.let {
                val padding = EdgeInsets(
                    it.top,
                    0.0,
                    it.bottom,
                    120.0 * pixelDensity
                )
                viewportDataSource.followingPaddingPropertyOverride(padding)
                viewportDataSource.evaluate()
            }
        }

        gravitate_right.setOnClickListener {
            mapboxMap.getCameraOptions(null).padding?.let {
                val padding = EdgeInsets(
                    it.top,
                    120.0 * pixelDensity,
                    it.bottom,
                    0.0
                )
                viewportDataSource.followingPaddingPropertyOverride(padding)
                viewportDataSource.evaluate()
            }
        }

        gravitate_top.setOnClickListener {
            mapboxMap.getCameraOptions(null).padding?.let {
                val padding = EdgeInsets(
                    0.0,
                    it.left,
                    120.0 * pixelDensity,
                    it.right
                )
                viewportDataSource.followingPaddingPropertyOverride(padding)
                viewportDataSource.evaluate()
            }
        }

        gravitate_bottom.setOnClickListener {
            mapboxMap.getCameraOptions(null).padding?.let {
                val padding = EdgeInsets(
                    120.0 * pixelDensity,
                    it.left,
                    0.0,
                    it.right
                )
                viewportDataSource.followingPaddingPropertyOverride(padding)
                viewportDataSource.evaluate()
            }
        }
    }

    private fun initCameraListeners() {
        mapboxMap.addOnCameraChangeListener(
            object : OnCameraChangeListener {
                override fun onCameraChanged() {
                    updateCameraChangeView()
                }
            }
        )
    }

    private fun initNavigation() {
        mapboxNavigation = MapboxNavigation(
            NavigationOptions.Builder(this)
                .accessToken(getMapboxAccessTokenFromResources())
                .locationEngine(ReplayLocationEngine(mapboxReplayer))
                .build()
        ).apply {
            registerLocationObserver(
                object : LocationObserver {

                    override fun onRawLocationChanged(rawLocation: Location) {
                        navigationCamera.requestNavigationCameraToIdle()
                        val point = Point.fromLngLat(rawLocation.longitude, rawLocation.latitude)
                        val cameraOptions = CameraOptions.Builder()
                            .center(point)
                            .zoom(13.0)
                            .build()
                        mapboxMap.jumpTo(cameraOptions)
                        locationComponent?.forceLocationUpdate(rawLocation)
                        mapboxNavigation.unregisterLocationObserver(this)
                    }

                    override fun onEnhancedLocationChanged(
                        enhancedLocation: Location,
                        keyPoints: List<Location>
                    ) {
                        // no impl
                    }
                }
            )
            registerRouteProgressObserver(routeProgressObserver)
            registerRoutesObserver(routesObserver)
            registerMapMatcherResultObserver(mapMatcherResultObserver)
        }

        mapboxReplayer.pushRealLocation(this, 0.0)
        mapboxReplayer.playbackSpeed(1.0)
        mapboxReplayer.play()
    }

    private fun startSimulation(route: DirectionsRoute) {
        mapboxReplayer.stop()
        mapboxReplayer.clearEvents()
        mapboxReplayer.pushRealLocation(this, 0.0)
        val replayEvents = replayRouteMapper.mapDirectionsRouteGeometry(route)
        mapboxReplayer.pushEvents(replayEvents)
        mapboxReplayer.seekTo(replayEvents.first())
        mapboxReplayer.play()
    }

    private fun initStyle() {
        mapboxMap.loadStyleUri(
            MAPBOX_STREETS,
            object : Style.OnStyleLoaded {
                override fun onStyleLoaded(style: Style) {
                    initializeLocationComponent(style)
                    getGesturesPlugin().addOnMapLongClickListener(
                        this@CameraAnimationsActivity
                    )
                    style.addSource(poiSource)
                    style.addLayer(poiLayer)
                }
            },
            object : OnMapLoadErrorListener {
                override fun onMapLoadError(mapViewLoadError: MapLoadError, msg: String) {
                    Log.e(
                        "CameraAnimationsAct",
                        "Error loading map: %s".format(mapViewLoadError.name)
                    )
                }
            }
        )
    }

    private fun initAnimations() {
        val adapter = AnimationAdapter(this, this)
        val manager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        animationsList.layoutManager = manager
        animationsList.adapter = adapter
    }

    @SuppressLint("SetTextI18n")
    private fun updateCameraChangeView() {
        mapboxMap.getCameraOptions(null).let { currentMapCamera ->
            cameraChangeView_state.text = "state: ${navigationCamera.state}"
            cameraChangeView_lng.text = "lng: " +
                currentMapCamera.center?.longitude().formatNumber()
            cameraChangeView_lat.text = "lat: ${currentMapCamera.center?.latitude().formatNumber()}"
            cameraChangeView_zoom.text = "zoom: ${currentMapCamera.zoom.formatNumber()}"
            cameraChangeView_bearing.text = "bearing: ${currentMapCamera.bearing.formatNumber()}"
            cameraChangeView_pitch.text = "pitch: ${currentMapCamera.pitch.formatNumber()}"
            cameraChangeView_padding.text =
                """
                    |padding:
                    |  top: ${currentMapCamera.padding?.top.formatNumber()}
                    |  left: ${currentMapCamera.padding?.left.formatNumber()}
                    |  bottom: ${currentMapCamera.padding?.bottom.formatNumber()}
                    |  right: ${currentMapCamera.padding?.right.formatNumber()}
               """.trimMargin()
        }
    }

    @SuppressLint("MissingPermission")
    override fun onButtonClicked(animationType: AnimationType) {
        when (animationType) {
            AnimationType.Following -> {
                viewportDataSource.followingZoomUpdatesAllowed = true
                viewportDataSource.followingPaddingPropertyOverride(followingEdgeInsets)
                viewportDataSource.evaluate()
                navigationCamera.requestNavigationCameraToFollowing()
            }
            AnimationType.Overview -> {
                viewportDataSource.overviewPaddingPropertyOverride(overviewEdgeInsets)
                viewportDataSource.evaluate()
                navigationCamera.requestNavigationCameraToOverview()
            }
            AnimationType.ToPOI -> {
                locationComponent?.lastKnownLocation?.let {
                    val center = Point.fromLngLat(
                        it.longitude + 0.0123,
                        it.latitude + 0.0123
                    )
                    mapView.getCameraAnimationsPlugin().flyTo(
                        CameraOptions.Builder()
                            .center(center)
                            .bearing(0.0)
                            .zoom(14.0)
                            .pitch(0.0)
                            .build(),
                        MapAnimationOptions.mapAnimationOptions {
                            duration = 1500
                        }
                    )
                }
            }
            AnimationType.LookAtPOIWhenFollowing -> {
                if (lookAtPoint == null) {
                    val center = mapboxMap.getCameraOptions(null).center
                        ?: Point.fromLngLat(0.0, 0.0)
                    lookAtPoint = Point.fromLngLat(
                        (center.longitude()) + 0.003,
                        (center.latitude()) + 0.003
                    ).also {
                        viewportDataSource.additionalPointsToFrameForFollowing(listOf(it))
                        viewportDataSource.followingBearingPropertyOverride(
                            TurfMeasurement.bearing(center, it)
                        )
                        viewportDataSource.evaluate()
                    }
                } else {
                    lookAtPoint = null
                    viewportDataSource.additionalPointsToFrameForFollowing(emptyList())
                    viewportDataSource.followingBearingPropertyOverride(null)
                    viewportDataSource.evaluate()
                }
            }
        }
    }

    private fun findRoute(origin: Point, destination: Point) {
        val routeOptions: RouteOptions = RouteOptions.builder()
            .applyDefaultParams()
            .accessToken(getMapboxAccessTokenFromResources())
            .coordinates(listOf(origin, destination))
            .alternatives(true)
            .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
            .overview(DirectionsCriteria.OVERVIEW_FULL)
            .annotationsList(
                listOf(
                    DirectionsCriteria.ANNOTATION_SPEED,
                    DirectionsCriteria.ANNOTATION_DISTANCE,
                    DirectionsCriteria.ANNOTATION_CONGESTION
                )
            )
            .build()

        mapboxNavigation.requestRoutes(routeOptions)
    }

    override fun onMapLongClick(point: Point): Boolean {
        locationComponent?.let { locComp ->
            val currentLocation = locComp.lastKnownLocation
            if (currentLocation != null) {
                val originPoint = Point.fromLngLat(
                    currentLocation.longitude,
                    currentLocation.latitude
                )
                findRoute(originPoint, point)
            }
        }
        return false
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
        navigationCamera.resetFrame()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
        mapboxNavigation.onDestroy()
    }

    private fun initializeLocationComponent(style: Style) {
        locationComponent = getLocationComponent()
        val activationOptions = LocationComponentActivationOptions.builder(this, style)
            .useDefaultLocationEngine(false) // SBNOTE: I think this should be false eventually
            .build()
        locationComponent?.let {
            it.activateLocationComponent(activationOptions)
            it.enabled = true
            it.renderMode = RenderMode.GPS
            it.addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        }
    }

    private fun getMapboxAccessTokenFromResources(): String {
        return getString(this.resources.getIdentifier("mapbox_access_token", "string", packageName))
    }

    private fun getLocationComponent(): LocationPluginImpl {
        return mapView.getLocationPlugin()
    }

    private fun getGesturesPlugin(): GesturesPlugin {
        return mapView.getGesturesPlugin()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        Toast.makeText(
            this,
            "This app needs location and storage permissions in order to show its functionality.",
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            init()
        } else {
            Toast.makeText(
                this,
                "You didn't grant location permissions.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun Number?.formatNumber() = "%.8f".format(this)
}
