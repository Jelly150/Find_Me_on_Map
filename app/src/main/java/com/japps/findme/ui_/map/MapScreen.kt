package com.japps.findme.ui_.map

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

import androidx.compose.material3.Button
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
@Composable
fun MapScreen(
    viewModel: MapViewModel = viewModel()
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    val userLocation by
    viewModel.userLocation.collectAsState()

    var showLocationDialog by
    remember { mutableStateOf(false) }

    var showInternetDialog by
    remember { mutableStateOf(false) }


    /*
     * Check everything and start location updates
     */
    fun checkLocationAndInternet() {

        val finePermission =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val coarsePermission =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED


        /*
         * Permission not granted
         */
        if (!finePermission && !coarsePermission) {
            return
        }


        /*
         * Location service disabled
         */
        if (!viewModel.isLocationEnabled()) {

            showLocationDialog = true

            return
        }


        /*
         * Internet unavailable
         */
        if (!viewModel.isInternetAvailable()) {

            showInternetDialog = true

            return
        }


        /*
         * Everything is available
         *
         * Start GPS updates
         */
        showLocationDialog = false
        showInternetDialog = false

        viewModel.startLocationUpdates()
    }


    /*
     * Location permission launcher
     */
    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val fineLocation =
                permissions[
                    Manifest.permission.ACCESS_FINE_LOCATION
                ] == true

            val coarseLocation =
                permissions[
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ] == true


            if (fineLocation || coarseLocation) {

                checkLocationAndInternet()
            }
        }


    /*
     * Check permission when screen starts
     */
    LaunchedEffect(Unit) {

        val finePermission =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val coarsePermission =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED


        if (!finePermission && !coarsePermission) {

            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )

        } else {

            checkLocationAndInternet()
        }
    }


    /*
     * Re-check when user comes back
     * from Location / Internet Settings
     */
    androidx.compose.runtime.DisposableEffect(
        lifecycleOwner
    ) {

        val observer =
            LifecycleEventObserver { _, event ->

                if (event == Lifecycle.Event.ON_RESUME) {

                    checkLocationAndInternet()
                }
            }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {

            lifecycleOwner.lifecycle.removeObserver(
                observer
            )
        }
    }


    /*
     * Location dialog
     */
    if (showLocationDialog) {

        AlertDialog(

            onDismissRequest = {
                showLocationDialog = false
            },

            title = {
                Text("Location is turned off")
            },

            text = {
                Text(
                    "Please turn on Location to use your current location."
                )
            },

            confirmButton = {

                TextButton(

                    onClick = {

                        showLocationDialog = false

                        context.startActivity(
                            Intent(
                                Settings.ACTION_LOCATION_SOURCE_SETTINGS
                            )
                        )
                    }

                ) {

                    Text("Turn On")
                }
            },

            dismissButton = {

                TextButton(

                    onClick = {
                        showLocationDialog = false
                    }

                ) {

                    Text("Cancel")
                }
            }
        )
    }


    /*
     * Internet dialog
     */
    if (showInternetDialog) {

        AlertDialog(

            onDismissRequest = {
                showInternetDialog = false
            },

            title = {
                Text("Internet is unavailable")
            },

            text = {
                Text(
                    "Please turn on Wi-Fi or mobile data to use FindMe."
                )
            },

            confirmButton = {

                TextButton(

                    onClick = {

                        showInternetDialog = false


                        if (
                            Build.VERSION.SDK_INT >=
                            Build.VERSION_CODES.Q
                        ) {

                            context.startActivity(
                                Intent(
                                    Settings.Panel.ACTION_INTERNET_CONNECTIVITY
                                )
                            )

                        } else {

                            context.startActivity(
                                Intent(
                                    Settings.ACTION_WIRELESS_SETTINGS
                                )
                            )
                        }
                    }

                ) {

                    Text("Turn On")
                }
            },

            dismissButton = {

                TextButton(

                    onClick = {
                        showInternetDialog = false
                    }

                ) {

                    Text("Cancel")
                }
            }
        )
    }


    /*
     * =========================================================
     * MAP / LOADING UI
     * =========================================================
     *
     * No default location is used.
     *
     * Until GPS gives us a real location:
     *
     *        CircularProgressIndicator
     *
     * Once location is available:
     *
     *        GoogleMap
     */

    if (userLocation == null) {

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                CircularProgressIndicator()

                Text("Getting your location...")
            }
        }

    } else {

        val cameraPositionState =
            rememberCameraPositionState()

        var mapLoaded by remember {
            mutableStateOf(false)
        }

        /*
         * Share current GPS location
         */
        fun shareLocation() {

            userLocation?.let { location ->

                val latitude = location.latitude
                val longitude = location.longitude

                val locationUrl =
                    "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"

                val shareText =
                    "My current location:\n$locationUrl"

                val shareIntent =
                    Intent(Intent.ACTION_SEND).apply {

                        type = "text/plain"

                        putExtra(
                            Intent.EXTRA_TEXT,
                            shareText
                        )
                    }

                context.startActivity(
                    Intent.createChooser(
                        shareIntent,
                        "Share my location"
                    )
                )
            }
        }


        /*
         * Map + Share Button
         */
        Box(
            modifier = Modifier.fillMaxSize()
        ) {

            /*
             * Google Map
             */
            GoogleMap(

                modifier = Modifier.fillMaxSize(),

                cameraPositionState =
                    cameraPositionState,

                properties =
                    MapProperties(
                        isMyLocationEnabled = false
                    ),

                uiSettings =
                    MapUiSettings(
                        myLocationButtonEnabled = false,
                        zoomControlsEnabled = false
                    ),

                onMapLoaded = {

                    mapLoaded = true
                }

            ) {

                userLocation?.let { location ->

                    Marker(

                        state =
                            MarkerState(
                                position = location
                            ),

                        title = "You are here"
                    )
                }
            }


            /*
             * Share Location Button
             */
            Button(

                onClick = {

                    userLocation?.let { location ->

                        scope.launch {

                            /*
                             * Get full address from GPS coordinates
                             */
                            val address = withContext(Dispatchers.IO) {

                                try {

                                    val geocoder =
                                        android.location.Geocoder(
                                            context,
                                            java.util.Locale.getDefault()
                                        )

                                    val addresses =
                                        geocoder.getFromLocation(
                                            location.latitude,
                                            location.longitude,
                                            1
                                        )

                                    if (!addresses.isNullOrEmpty()) {

                                        addresses[0].getAddressLine(0)

                                    } else {

                                        "Address unavailable"
                                    }

                                } catch (e: Exception) {

                                    "Address unavailable"
                                }
                            }


                            /*
                             * Google Maps link
                             */
                            val locationUrl =
                                "https://www.google.com/maps/search/?api=1&query=" +
                                        "${location.latitude},${location.longitude}"


                            /*
                             * Message to share
                             */
                            val shareText =
                                """
                    📍 My current location
                    
                    $address
                    
                    🗺️ Open in Maps:
                    $locationUrl
                    """.trimIndent()


                            /*
                             * Android Share Intent
                             */
                            val shareIntent =
                                Intent(Intent.ACTION_SEND).apply {

                                    type = "text/plain"

                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        shareText
                                    )
                                }


                            /*
                             * Open Android share sheet
                             */
                            context.startActivity(
                                Intent.createChooser(
                                    shareIntent,
                                    "Share my location"
                                )
                            )
                        }
                    }
                },

                enabled = userLocation != null,

                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 50.dp)

            ) {

                Text("Share Location")
            }
        }


        /*
         * Move camera after Google Map
         * has completely initialized.
         */
        LaunchedEffect(
            userLocation,
            mapLoaded
        ) {

            if (mapLoaded) {

                userLocation?.let { location ->

                    cameraPositionState.animate(

                        CameraUpdateFactory.newLatLngZoom(
                            location,
                            17f
                        )
                    )
                }
            }
        }
    }


}




