package com.japps.findme.data

import android.content.Context
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

// This class is responsible for checking the device status,
// such as Location/GPS and Internet connectivity.
class DeviceStatus(
    private val context: Context
) {

    // Checks whether Location/GPS is enabled on the device.
    fun isLocationEnabled(): Boolean {

        // Get the system LocationManager service.
        val locationManager =
            context.getSystemService(
                Context.LOCATION_SERVICE
            ) as LocationManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {

            // Android 9 (API 28) and above:
            // Directly check whether Location services are enabled.
            locationManager.isLocationEnabled

        } else {

            // Android versions below Android 9:
            // Check whether either GPS or Network location provider is enabled.
            locationManager.isProviderEnabled(
                LocationManager.GPS_PROVIDER
            ) || locationManager.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER
            )
        }
    }

    // Checks whether the device currently has an active
    // and validated internet connection.
    fun isInternetAvailable(): Boolean {

        // Get the system ConnectivityManager service.
        val connectivityManager =
            context.getSystemService(
                Context.CONNECTIVITY_SERVICE
            ) as ConnectivityManager

        // Get the currently active network.
        // If there is no active network, return false.
        val network =
            connectivityManager.activeNetwork
                ?: return false

        // Get information about the capabilities of the active network.
        // If capabilities are unavailable, return false.
        val capabilities =
            connectivityManager.getNetworkCapabilities(network)
                ?: return false

        // Check whether:
        // 1. The network has internet capability.
        // 2. The network has been validated and can actually access the internet.
        return capabilities.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_INTERNET
        ) &&
                capabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_VALIDATED
                )
    }
}