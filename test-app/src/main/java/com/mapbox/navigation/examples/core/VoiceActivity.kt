package com.mapbox.navigation.examples.core

import android.annotation.SuppressLint
import android.content.res.Resources
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapLoadError
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.Style.Companion.MAPBOX_STREETS
import com.mapbox.maps.plugin.animation.getCameraAnimationsPlugin
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
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigation.Companion.defaultNavigationOptionsBuilder
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.MapMatcherResult
import com.mapbox.navigation.core.trip.session.MapMatcherResultObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver
import com.mapbox.navigation.ui.base.api.voice.VoiceCallback
import com.mapbox.navigation.ui.base.model.voice.VoiceState
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
import com.mapbox.navigation.ui.voice.api.MapboxVoiceApi
import com.mapbox.navigation.utils.internal.ifNonNull
import kotlinx.android.synthetic.main.layout_camera_animations.*
import java.util.Locale

class VoiceActivity :
    AppCompatActivity(),
    PermissionsListener,
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
    private var voiceAPI: MapboxVoiceApi? = null

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

    private val mapMatcherResultObserver = object : MapMatcherResultObserver {
        override fun onNewMapMatcherResult(mapMatcherResult: MapMatcherResult) {
            val locationUpdate = LocationUpdate(
                location = mapMatcherResult.enhancedLocation,
                intermediatePoints = null, // fixme mapMatcherResult.keyPoints.dropLast(1),
                animationDuration = 1000L
            )
            locationComponent?.forceLocationUpdate(locationUpdate)
            viewportDataSource.onLocationChanged(mapMatcherResult.enhancedLocation)

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

    private val voiceInstructionsObserver = object : VoiceInstructionsObserver {
        override fun onNewVoiceInstructions(voiceInstructions: VoiceInstructions) {
            voiceAPI?.retrieveVoiceFile(
                voiceInstructions,
                object : VoiceCallback {
                    override fun onVoice(instructionFile: VoiceState.VoiceFile) {
                        Log.d(
                            "VoiceActivity",
                            "DEBUG onVoice($instructionFile)"
                        )
                    }

                    override fun onFailure(error: VoiceState.VoiceFailure) {
                        Log.d(
                            "VoiceActivity",
                            "DEBUG onFailure(error: $error)"
                        )
                    }
                }
            )
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
        setContentView(R.layout.layout_voice)
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
        initStyle()
        mapboxNavigation.startTripSession()
    }

    private fun initRouteLine() {
        val mapboxRouteLineOptions = MapboxRouteLineOptions.Builder(this).build()
        routeLineAPI = MapboxRouteLineApi(mapboxRouteLineOptions)
        routeLineView = MapboxRouteLineView(mapboxRouteLineOptions)

        val routeArrowOptions = RouteArrowOptions.Builder(this).build()
        routeArrowView = MapboxRouteArrowView(routeArrowOptions)
        voiceAPI = MapboxVoiceApi(this, getMapboxAccessTokenFromResources(), Locale.US.language)
    }

    private fun initNavigation() {
        val navigationOptions = defaultNavigationOptionsBuilder(
            this,
            getMapboxAccessTokenFromResources()
        )
            .locationEngine(ReplayLocationEngine(mapboxReplayer))
            .build()
        mapboxNavigation = MapboxNavigation(navigationOptions).apply {
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
            registerVoiceInstructionsObserver(voiceInstructionsObserver)
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
                        this@VoiceActivity
                    )
                }
            },
            object : OnMapLoadErrorListener {
                override fun onMapLoadError(mapViewLoadError: MapLoadError, msg: String) {
                    Log.e(
                        "VoiceActivity",
                        "Error loading map: %s".format(mapViewLoadError.name)
                    )
                }
            }
        )
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
}
