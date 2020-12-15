package com.mikail.runtracker.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import com.mikail.runtracker.R
import com.mikail.runtracker.Utils.Constants.ACTION_START_OR_RESUME_SERVICE
import com.mikail.runtracker.Utils.Constants.MAP_VIEW_BUNDLE_KEY
import com.mikail.runtracker.Utils.Constants.MAP_ZOOM
import com.mikail.runtracker.Utils.Constants.POLYLINE_COLOR
import com.mikail.runtracker.Utils.Constants.POLYLINE_WIDTH
import com.mikail.runtracker.Utils.TrackingUtility
import com.mikail.services.TrackingService
import kotlinx.android.synthetic.main.fragment_tracking.*
import timber.log.Timber


class TrackingFragment : Fragment(R.layout.fragment_tracking) {


    private var map: GoogleMap? = null

    private var isTracking = false
    private var curTimeInMillis = 0L
    private var pathPoints = mutableListOf<MutableList<LatLng>>()

//    private val viewModel: MainViewModel by viewModels()

    private var menu: Menu? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mapViewBundle = savedInstanceState?.getBundle(MAP_VIEW_BUNDLE_KEY)
        mapView.onCreate(mapViewBundle)


        // restore dialog instance
        if(savedInstanceState != null) {
            val cancelRunDialog = parentFragmentManager.findFragmentByTag(CANCEL_DIALOG_TAG) as CancelRunDialog?
            cancelRunDialog?.setYesListener {
                stopRun()
            }
        }

    btnToggleRun.setOnClickListener {
        toggleRun()
    }



    mapView.getMapAsync {
            map = it
            addAllPolylines()
        }

    subscribeToObservers()
    }


    /**
     * Adds all polylines to the pathPoints list to display them after screen rotations
     */
    private fun addAllPolylines() {
        for (polyline in pathPoints) {
            val polylineOptions = PolylineOptions()
                .color(POLYLINE_COLOR)
                .width(POLYLINE_WIDTH)
                .addAll(polyline)
            map?.addPolyline(polylineOptions)
        }
    }

    /**
     * Draws a polyline between the two latest points.
     */
    private fun addLatestPolyline() {
        // only add polyline if we have at least two elements in the last polyline
        if (pathPoints.isNotEmpty() && pathPoints.last().size > 1) {
            val preLastLatLng = pathPoints.last()[pathPoints.last().size - 2]
            val lastLatLng = pathPoints.last().last()
            val polylineOptions = PolylineOptions()
                .color(POLYLINE_COLOR)
                .width(POLYLINE_WIDTH)
                .add(preLastLatLng)
                .add(lastLatLng)

            map?.addPolyline(polylineOptions)
        }
    }
    /**
     * Starts the tracking service or resumes it if it is currently paused.
     */
    private fun startOrResumeTrackingService() =
        Intent(requireContext(), TrackingService::class.java).also {
            it.action = ACTION_START_OR_RESUME_SERVICE
            requireContext().startService(it)
        }


    /**
     * Subscribes to changes of LiveData objects
     */
    private fun subscribeToObservers() {
        TrackingService.isTracking.observe(viewLifecycleOwner, Observer {
            updateTracking(it)
        })

        TrackingService.pathPoints.observe(viewLifecycleOwner, Observer {
            pathPoints = it
            addLatestPolyline()
            moveCameraToUser()
        })

        TrackingService.timeRunInMillis.observe(viewLifecycleOwner, Observer {
            curTimeInMillis = it
            val formattedTime = TrackingUtility.getFormattedStopWatchTime(it, true)
            tvTimer.text = formattedTime
        })
    }


    /**
     * Updates the tracking variable and the UI accordingly
     */
    private fun updateTracking(isTracking: Boolean) {
        this.isTracking = isTracking
        if (!isTracking && curTimeInMillis > 0L) {
            btnToggleRun.text = getString(R.string.start_text)
            btnFinishRun.visibility = View.VISIBLE
        } else if (isTracking) {
            btnToggleRun.text = "STOP RUN"
//            menu?.getItem(0)?.isVisible = true
            btnFinishRun.visibility = View.GONE
        }
    }


    /**
     * Toggles the tracking state
     */
    @SuppressLint("MissingPermission")
    private fun toggleRun() {
        if (isTracking) {
//            menu?.getItem(0)?.isVisible = true
//            pauseTrackingService()
        } else {
            startOrResumeTrackingService()
//            Timber.d("Started service")
        }
    }


    /**
     * Will move the camera to the user's location.
     */
    private fun moveCameraToUser() {
        if (pathPoints.isNotEmpty() && pathPoints.last().isNotEmpty()) {
            map?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    pathPoints.last().last(),
                    MAP_ZOOM
                )
            )
        }
    }

    /**
     * Finishes the tracking.
     */
    private fun stopRun() {
        Timber.d("STOPPING RUN")
        tvTimer.text = "00:00:00:00"
        stopTrackingService()
        findNavController().navigate(R.id.action_trackingFragment_to_runFragment2)
    }




    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.miCancelTracking -> {
                showCancelTrackingDialog()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Shows a dialog to cancel the current run.
     */
    private fun showCancelTrackingDialog() {
        CancelRunDialog().apply {
            setYesListener {
                stopRun()
            }
        }.show(parentFragmentManager, CANCEL_DIALOG_TAG)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.toolbar_menu_tracking, menu)
        this.menu = menu
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        // just checking for isTracking doesnt trigger this when rotating the device
        // in paused mode
        if (curTimeInMillis > 0L) {
            this.menu?.getItem(0)?.isVisible = true
        }
    }

    override fun onResume() {
        mapView.onResume()
        super.onResume()
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }




}