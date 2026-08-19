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

class LocationRepository(
    context: Context
) {

    private val locationManager =
        context.getSystemService(
            Context.LOCATION_SERVICE
        ) as LocationManager

    @SuppressLint("MissingPermission")
    fun getGpsLocationUpdates(): Flow<Location> = callbackFlow {

        val gpsListener = object : LocationListener {

            override fun onLocationChanged(location: Location) {
                trySend(location)
            }

            override fun onProviderEnabled(provider: String) {
            }

            override fun onProviderDisabled(provider: String) {
            }
        }

        if (!locationManager.isProviderEnabled(
                LocationManager.GPS_PROVIDER
            )
        ) {
            close(
                IllegalStateException(
                    "GPS is disabled"
                )
            )
            return@callbackFlow
        }

        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            1000L,
            1f,
            gpsListener
        )

        awaitClose {
            locationManager.removeUpdates(gpsListener)
        }
    }
}