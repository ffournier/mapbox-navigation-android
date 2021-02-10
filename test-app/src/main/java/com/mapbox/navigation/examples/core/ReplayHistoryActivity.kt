package com.mapbox.navigation.examples.core

import android.annotation.SuppressLint
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapLoadError
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.getCameraAnimationsPlugin
import com.mapbox.maps.plugin.delegates.listeners.OnMapLoadErrorListener
import com.mapbox.maps.plugin.location.LocationComponentActivationOptions
import com.mapbox.maps.plugin.location.LocationUpdate
import com.mapbox.maps.plugin.location.getLocationPlugin
import com.mapbox.maps.plugin.location.modes.CameraMode
import com.mapbox.maps.plugin.location.modes.RenderMode
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.history.ReplayEventBase
import com.mapbox.navigation.core.replay.history.ReplayEventsObserver
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.examples.core.replay.HistoryFileLoader
import com.mapbox.navigation.examples.core.replay.HistoryFilesActivity
import com.mapbox.navigation.examples.util.Utils
import com.mapbox.navigation.ui.utils.internal.ifNonNull
import kotlinx.android.synthetic.main.activity_replay_history_layout.*
import kotlinx.android.synthetic.main.layout_activity_signboard.mapView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class ReplayHistoryActivity : AppCompatActivity() {

    private var loadNavigationJob: Job? = null
    private val mapboxReplayer = MapboxReplayer()
    private val historyFileLoader = HistoryFileLoader()
    private val replayProgressObserver = ReplayProgressObserver(mapboxReplayer)
    private lateinit var mapboxNavigation: MapboxNavigation
    private val locationEngineCallback = MyLocationEngineCallback(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_replay_history_layout)
        handleHistoryFileSelected()
        initNavigation()
        initMapStyle()

        selectHistoryButton.setOnClickListener {
            val activityIntent = Intent(this, HistoryFilesActivity::class.java)
            startActivityForResult(activityIntent, HistoryFilesActivity.REQUEST_CODE)
        }
        setupReplayControls()
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
        if (::mapboxNavigation.isInitialized) {
//            mapboxNavigation.registerRoutesObserver(routesObserver)
            mapboxNavigation.registerLocationObserver(locationObserver)
//            mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
            mapboxNavigation.registerRouteProgressObserver(replayProgressObserver)
        }
    }

    override fun onStop() {
        super.onStop()
        if (::mapboxNavigation.isInitialized) {
//            mapboxNavigation.unregisterRoutesObserver(routesObserver)
            mapboxNavigation.unregisterLocationObserver(locationObserver)
//            mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
            mapboxNavigation.unregisterRouteProgressObserver(replayProgressObserver)
        }
        mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
        mapboxNavigation.onDestroy()
    }

    @SuppressLint("MissingPermission")
    private fun initMapStyle() {
        mapView.getMapboxMap().loadStyleUri(
            Style.MAPBOX_STREETS,
            { style: Style ->
                mapboxNavigation.navigationOptions.locationEngine.getLastLocation(
                    locationEngineCallback
                )
                initializeLocationComponent(style)
            },
            object : OnMapLoadErrorListener {
                override fun onMapLoadError(mapViewLoadError: MapLoadError, msg: String) {
                    // intentionally blank
                }
            }
        )
    }

    private fun initializeLocationComponent(style: Style) {
        val activationOptions = LocationComponentActivationOptions
            .builder(this, style)
            .useDefaultLocationEngine(false)
            .build()
        mapView.getLocationPlugin().let {
            it.activateLocationComponent(activationOptions)
            it.enabled = true
            it.renderMode = RenderMode.GPS
            it.cameraMode = CameraMode.TRACKING
        }
    }

    @SuppressLint("MissingPermission")
    private fun initNavigation() {
        mapboxNavigation = MapboxNavigation(
            NavigationOptions.Builder(this)
                .accessToken(Utils.getMapboxAccessToken(this))
                .locationEngine(ReplayLocationEngine(mapboxReplayer))
                .build()
        )
    }

    private val locationObserver = object : LocationObserver {
        override fun onRawLocationChanged(rawLocation: Location) {}
        override fun onEnhancedLocationChanged(
            enhancedLocation: Location,
            keyPoints: List<Location>
        ) {
            if (keyPoints.isEmpty()) {
                updateLocation(enhancedLocation)
            } else {
                updateLocation(keyPoints)
            }
        }
    }

    private fun updateLocation(location: Location) {
        updateLocation(listOf(location))
    }

    private fun updateLocation(locations: List<Location>) {
        val location = locations[0]
        mapView.getLocationPlugin().forceLocationUpdate(LocationUpdate(location, null, null))

        val mapAnimationOptionsBuilder = MapAnimationOptions.Builder()
        mapAnimationOptionsBuilder.duration = 1500L
        mapView.getCameraAnimationsPlugin().easeTo(
            CameraOptions.Builder()
                .center(Point.fromLngLat(location.longitude, location.latitude))
                .bearing(location.bearing.toDouble())
                .pitch(45.0)
                .zoom(17.0)
                .padding(EdgeInsets(1000.0, 0.0, 0.0, 0.0))
                .build(),
            mapAnimationOptionsBuilder.build()
        )
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == HistoryFilesActivity.REQUEST_CODE) {
            handleHistoryFileSelected()
        }
    }

    @SuppressLint("MissingPermission")
    private fun handleHistoryFileSelected() {
        loadNavigationJob = CoroutineScope(Dispatchers.Main).launch {
            val events = historyFileLoader
                .loadReplayHistory(this@ReplayHistoryActivity)
            mapboxReplayer.clearEvents()
            mapboxReplayer.pushEvents(events)
            playReplay.visibility = View.VISIBLE
            mapboxNavigation.resetTripSession()
            mapboxReplayer.playFirstLocation()
            mapboxNavigation.navigationOptions.locationEngine.getLastLocation(
                locationEngineCallback
            )
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateReplayStatus(playbackEvents: List<ReplayEventBase>) {
        playbackEvents.lastOrNull()?.eventTimestamp?.let {
            val currentSecond = mapboxReplayer.eventSeconds(it).toInt()
            val durationSecond = mapboxReplayer.durationSeconds().toInt()
            playerStatus.text = "$currentSecond:$durationSecond"
        }
    }

    @SuppressLint("MissingPermission")
    private fun setupReplayControls() {
        seekBar.max = 8
        seekBar.progress = 1
        seekBarText.text = getString(R.string.replay_playback_speed_seekbar, seekBar.progress)
        seekBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    mapboxReplayer.playbackSpeed(progress.toDouble())
                    seekBarText.text = getString(R.string.replay_playback_speed_seekbar, progress)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {}
                override fun onStopTrackingTouch(seekBar: SeekBar) {}
            }
        )

        playReplay.setOnClickListener {
            mapboxReplayer.play()
            mapboxNavigation.startTripSession()
            playReplay.visibility = View.GONE
        }

        mapboxReplayer.registerObserver(
            object : ReplayEventsObserver {
                override fun replayEvents(events: List<ReplayEventBase>) {
                    updateReplayStatus(events)
                }
            }
        )
    }

    private class MyLocationEngineCallback constructor(
        activity: ReplayHistoryActivity
    ) : LocationEngineCallback<LocationEngineResult> {

        private val activityRef: WeakReference<ReplayHistoryActivity> = WeakReference(activity)

        override fun onSuccess(result: LocationEngineResult) {
            ifNonNull(result.lastLocation, activityRef.get()) { loc, act ->
                val point = Point.fromLngLat(loc.longitude, loc.latitude)
                val cameraOptions = CameraOptions.Builder()
                    .center(point)
                    .zoom(13.0)
                    .build()
                act.mapView.getMapboxMap().jumpTo(cameraOptions)
                act.mapView.getLocationPlugin().forceLocationUpdate(LocationUpdate(loc))
            }
        }

        override fun onFailure(exception: Exception) {}
    }
}
