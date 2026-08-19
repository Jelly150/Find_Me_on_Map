package com.japps.findme.ui_.map

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MapScreen(
    viewModel: MapViewModel = viewModel()
) {

    // Get the current Android Context.
    val context = LocalContext.current

    // Coroutine scope used for background operations,
    // such as converting GPS coordinates into an address.
    val scope = rememberCoroutineScope()

    // Get the lifecycle owner of the current Compose screen.
    val lifecycleOwner = LocalLifecycleOwner.current

    // Observe the latest location provided by the ViewModel.
    val userLocation by
    viewModel.userLocation.collectAsState()

    // Controls whether the Location disabled dialog is visible.
    var showLocationDialog by
    remember { mutableStateOf(false) }

    // Controls whether the Internet unavailable dialog is visible.
    var showInternetDialog by
    remember { mutableStateOf(false) }


    /*
     * Check all required device conditions:
     *
     * 1. Location permission
     * 2. Location/GPS service
     * 3. Internet connection
     *
     * If everything is available, start receiving GPS updates.
     */
    fun checkLocationAndInternet() {

        // Check whether precise location permission is granted.
        val finePermission =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        // Check whether approximate location permission is granted.
        val coarsePermission =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED


        /*
         * Location permission has not been granted.
         *
         * The permission launcher will handle requesting it.
         */
        if (!finePermission && !coarsePermission) {
            return
        }


        /*
         * Location service is disabled.
         *
         * Show a dialog asking the user to enable Location.
         */
        if (!viewModel.isLocationEnabled()) {

            showLocationDialog = true

            return
        }


        /*
         * Internet connection is unavailable.
         *
         * Show a dialog asking the user to enable
         * Wi-Fi or mobile data.
         */
        if (!viewModel.isInternetAvailable()) {

            showInternetDialog = true

            return
        }


        /*
         * All required conditions are satisfied.
         *
         * Hide any previously displayed dialogs
         * and start receiving GPS location updates.
         */
        showLocationDialog = false
        showInternetDialog = false

        viewModel.startLocationUpdates()
    }


    /*
     * Location permission launcher.
     *
     * This launcher requests both Fine and Coarse
     * location permissions from the user.
     */
    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            // Check whether Fine Location was granted.
            val fineLocation =
                permissions[
                    Manifest.permission.ACCESS_FINE_LOCATION
                ] == true

            // Check whether Coarse Location was granted.
            val coarseLocation =
                permissions[
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ] == true


            // Continue checking device status if
            // either location permission was granted.
            if (fineLocation || coarseLocation) {

                checkLocationAndInternet()
            }
        }


    /*
     * Check location permission when the screen
     * is first created.
     */
    LaunchedEffect(Unit) {

        // Check whether Fine Location permission is already granted.
        val finePermission =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        // Check whether Coarse Location permission is already granted.
        val coarsePermission =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED


        // If neither permission has been granted,
        // request both permissions.
        if (!finePermission && !coarsePermission) {

            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )

        } else {

            // Permission already exists, so check
            // Location and Internet status.
            checkLocationAndInternet()
        }
    }


    /*
     * Observe the screen lifecycle.
     *
     * When the user returns to the app after opening
     * Location or Internet settings, check the device
     * status again.
     */
    DisposableEffect(
        lifecycleOwner
    ) {

        val observer =
            LifecycleEventObserver { _, event ->

                // ON_RESUME is triggered when the screen
                // becomes active again.
                if (event == Lifecycle.Event.ON_RESUME) {

                    checkLocationAndInternet()
                }
            }

        // Start observing lifecycle events.
        lifecycleOwner.lifecycle.addObserver(observer)

        // Remove the observer when this screen is destroyed.
        onDispose {

            lifecycleOwner.lifecycle.removeObserver(
                observer
            )
        }
    }


    /*
     * Location disabled dialog.
     *
     * Shown when the device's Location service is turned off.
     */
    if (showLocationDialog) {

        AlertDialog(

            // Close the dialog when the user dismisses it.
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

            // Button used to open Android Location settings.
            confirmButton = {

                TextButton(

                    onClick = {

                        showLocationDialog = false

                        // Open the device's Location settings.
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

            // Button used to close the dialog.
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
     * Internet unavailable dialog.
     *
     * Shown when the device does not have
     * a usable internet connection.
     */
    if (showInternetDialog) {

        AlertDialog(

            // Close the dialog.
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

            // Button used to open Android's Internet settings.
            confirmButton = {

                TextButton(

                    onClick = {

                        showInternetDialog = false


                        /*
                         * Android 10 (API 29) and above:
                         * Open the Internet connectivity panel.
                         */
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

                            /*
                             * Older Android versions:
                             * Open the Wireless settings screen.
                             */
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

            // Button used to close the Internet dialog.
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
     * Show a loading screen while the first
     * location is being received.
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

                // Loading indicator while waiting for GPS.
                CircularProgressIndicator()

                Text("Getting your location...")
            }
        }

    } else {

        // State used to control the Google Maps camera.
        val cameraPositionState =
            rememberCameraPositionState()

        // Tracks whether Google Maps has finished loading.
        var mapLoaded by remember {
            mutableStateOf(false)
        }


        /*
         * Function for sharing the current GPS location.
         *
         * NOTE:
         * The actual Share Location button below contains
         * the newer implementation that also converts the
         * coordinates into a full address.
         */
        fun shareLocation() {

            userLocation?.let { location ->

                val latitude = location.latitude
                val longitude = location.longitude

                // Create a Google Maps URL using latitude
                // and longitude.
                val locationUrl =
                    "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"

                // Text that will be shared.
                val shareText =
                    "My current location:\n$locationUrl"

                // Create Android's standard share Intent.
                val shareIntent =
                    Intent(Intent.ACTION_SEND).apply {

                        type = "text/plain"

                        putExtra(
                            Intent.EXTRA_TEXT,
                            shareText
                        )
                    }

                // Open Android's share sheet.
                context.startActivity(
                    Intent.createChooser(
                        shareIntent,
                        "Share my location"
                    )
                )
            }
        }


        /*
         * Main map container.
         *
         * The Google Map fills the entire screen and
         * the Share Location button is placed on top.
         */
        Box(
            modifier = Modifier.fillMaxSize()
        ) {

            /*
             * Google Map.
             */
            GoogleMap(

                modifier = Modifier.fillMaxSize(),

                // Controls the position and zoom level of the camera.
                cameraPositionState =
                    cameraPositionState,

                // Configure Google Map properties.
                properties =
                    MapProperties(
                        // We use our own Marker instead of
                        // Google's default My Location indicator.
                        isMyLocationEnabled = false
                    ),

                // Configure Google Map UI controls.
                uiSettings =
                    MapUiSettings(
                        // Hide Google's default location button.
                        myLocationButtonEnabled = false,

                        // Hide zoom controls.
                        zoomControlsEnabled = false
                    ),

                // Called when Google Maps has completely loaded.
                onMapLoaded = {

                    mapLoaded = true
                }

            ) {

                /*
                 * Add a marker at the user's current location.
                 */
                userLocation?.let { location ->

                    Marker(

                        // Set marker position to current GPS location.
                        state =
                            MarkerState(
                                position = location
                            ),

                        title = "You are here"
                    )
                }
            }


            /*
             * Share Location Button.
             *
             * This button shares:
             * - Full address
             * - Google Maps location link
             */
            Button(

                onClick = {

                    userLocation?.let { location ->

                        // Run the address lookup in a coroutine.
                        scope.launch {

                            /*
                             * Convert latitude and longitude
                             * into a readable postal address.
                             *
                             * Geocoder performs the operation
                             * on the IO dispatcher so it does not
                             * block the main UI thread.
                             */
                            val address = withContext(Dispatchers.IO) {

                                try {

                                    // Create a Geocoder using the
                                    // device's default locale.
                                    val geocoder =
                                        android.location.Geocoder(
                                            context,
                                            java.util.Locale.getDefault()
                                        )

                                    // Convert GPS coordinates into
                                    // an address.
                                    val addresses =
                                        geocoder.getFromLocation(
                                            location.latitude,
                                            location.longitude,
                                            1
                                        )

                                    // Use the first returned address.
                                    if (!addresses.isNullOrEmpty()) {

                                        addresses[0].getAddressLine(0)

                                    } else {

                                        // No address was found.
                                        "Address unavailable"
                                    }

                                } catch (e: Exception) {

                                    // Handle Geocoder failures safely.
                                    "Address unavailable"
                                }
                            }


                            /*
                             * Create a Google Maps link using
                             * the current latitude and longitude.
                             */
                            val locationUrl =
                                "https://www.google.com/maps/search/?api=1&query=" +
                                        "${location.latitude},${location.longitude}"


                            /*
                             * Create the message that will be
                             * shared with other apps.
                             */
                            val shareText =
                                """
                    📍 My current location
                    
                    $address
                    
                    🗺️ Open in Maps:
                    $locationUrl
                    """.trimIndent()


                            /*
                             * Create Android's standard text
                             * sharing Intent.
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
                             * Open Android's share sheet so the
                             * user can choose WhatsApp, Messages,
                             * Gmail, etc.
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

                // Disable the button until a location is available.
                enabled = userLocation != null,

                // Position the button at the bottom center of the map.
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 50.dp)

            ) {

                Text("Share Location")
            }
        }


        /*
         * Move the Google Maps camera to the user's location
         * after both the location and map are ready.
         */
        LaunchedEffect(
            userLocation,
            mapLoaded
        ) {

            if (mapLoaded) {

                userLocation?.let { location ->

                    // Animate the camera to the current location.
                    // 17f provides a fairly close street-level zoom.
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