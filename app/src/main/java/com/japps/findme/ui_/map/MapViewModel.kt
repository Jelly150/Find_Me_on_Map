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

class MapViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository =
        LocationRepository(application.applicationContext)

    private val deviceStatus =
        DeviceStatus(application.applicationContext)

    private val _userLocation =
        MutableStateFlow<LatLng?>(null)

    val userLocation: StateFlow<LatLng?> =
        _userLocation.asStateFlow()

    private var locationJob: Job? = null

    fun isLocationEnabled(): Boolean {
        return deviceStatus.isLocationEnabled()
    }

    fun isInternetAvailable(): Boolean {
        return deviceStatus.isInternetAvailable()
    }

    fun startLocationUpdates() {

        if (locationJob != null) {
            return
        }

        locationJob = viewModelScope.launch {

            repository
                .getGpsLocationUpdates()
                .collect { location ->

                    _userLocation.value = LatLng(
                        location.latitude,
                        location.longitude
                    )
                }
        }
    }

    fun stopLocationUpdates() {

        locationJob?.cancel()
        locationJob = null
    }

    override fun onCleared() {
        stopLocationUpdates()
        super.onCleared()
    }
}