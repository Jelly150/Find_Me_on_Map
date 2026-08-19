package com.japps.findme.ui_.map

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.japps.findme.data.DeviceStatus
import com.japps.findme.data.LocationRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/*
 * ViewModel responsible for managing the map screen's location data
 * and communicating between the UI and LocationRepository.
 */
class MapViewModel(
    application: Application
) : AndroidViewModel(application) {

    /*
     * Repository responsible for receiving GPS location updates
     * from the device.
     */
    private val repository =
        LocationRepository(application.applicationContext)

    /*
     * Used to check whether Location/GPS and Internet services
     * are available on the device.
     */
    private val deviceStatus =
        DeviceStatus(application.applicationContext)

    /*
     * Private MutableStateFlow containing the user's current location.
     *
     * null means that a location has not been received yet.
     */
    private val _userLocation =
        MutableStateFlow<LatLng?>(null)

    /*
     * Public read-only StateFlow exposed to the UI.
     *
     * The UI can observe the location but cannot modify it directly.
     */
    val userLocation: StateFlow<LatLng?> =
        _userLocation.asStateFlow()

    /*
     * Keeps track of the coroutine collecting location updates.
     *
     * Used to prevent multiple location update collectors
     * from running at the same time.
     */
    private var locationJob: Job? = null


    /*
     * Check whether Location/GPS is enabled on the device.
     */
    fun isLocationEnabled(): Boolean {
        return deviceStatus.isLocationEnabled()
    }


    /*
     * Check whether the device has a working Internet connection.
     */
    fun isInternetAvailable(): Boolean {
        return deviceStatus.isInternetAvailable()
    }


    /*
     * Start receiving continuous GPS location updates.
     */
    fun startLocationUpdates() {

        /*
         * Prevent starting another location collection
         * if one is already running.
         */
        if (locationJob != null) {
            return
        }

        /*
         * Launch the location collection inside viewModelScope.
         *
         * viewModelScope is automatically cancelled when
         * the ViewModel is destroyed.
         */
        locationJob = viewModelScope.launch {

            /*
             * Collect GPS location updates from the repository.
             */
            repository
                .getGpsLocationUpdates()
                .collect { location ->

                    /*
                     * Convert Android Location into Google's
                     * LatLng format used by Google Maps Compose.
                     */
                    _userLocation.value = LatLng(
                        location.latitude,
                        location.longitude
                    )
                }
        }
    }


    /*
     * Stop receiving GPS location updates.
     */
    fun stopLocationUpdates() {

        // Cancel the active location collection.
        locationJob?.cancel()

        // Reset the Job reference so updates can be started again.
        locationJob = null
    }


    /*
     * Called automatically when the ViewModel is destroyed.
     */
    override fun onCleared() {

        // Stop GPS updates to prevent unnecessary work
        // and release the location listener.
        stopLocationUpdates()

        super.onCleared()
    }
}