package com.japps.findme.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

// Repository responsible for getting GPS location updates from the device.
class LocationRepository(
    context: Context
) {

    // LocationManager is used to access the device's location providers.
    private val locationManager =
        context.getSystemService(
            Context.LOCATION_SERVICE
        ) as LocationManager

    // Suppress the permission warning because location permission
    // is expected to be checked before calling this function.
    @SuppressLint("MissingPermission")
    fun getGpsLocationUpdates(): Flow<Location> = callbackFlow {

        // Listener that receives location changes from the GPS provider.
        val gpsListener = object : LocationListener {

            // Called whenever a new GPS location is received.
            override fun onLocationChanged(location: Location) {
                trySend(location)
            }

            // Called when the GPS provider becomes enabled.
            override fun onProviderEnabled(provider: String) {
            }

            // Called when the GPS provider becomes disabled.
            override fun onProviderDisabled(provider: String) {
            }
        }

        // Check whether GPS is enabled before requesting location updates.
        if (!locationManager.isProviderEnabled(
                LocationManager.GPS_PROVIDER
            )
        ) {

            // Close the Flow and notify the caller that GPS is disabled.
            close(
                IllegalStateException(
                    "GPS is disabled"
                )
            )

            return@callbackFlow
        }

        // Start receiving GPS location updates.
        // 1000L = request an update approximately every 1 second.
        // 1f = request an update when the device moves approximately 1 meter.
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            1000L,
            1f,
            gpsListener
        )

        // Remove the location listener when the Flow is cancelled
        // or the collector stops collecting.
        awaitClose {
            locationManager.removeUpdates(gpsListener)
        }
    }
}