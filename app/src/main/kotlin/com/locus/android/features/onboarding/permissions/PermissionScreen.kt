package com.locus.android.features.onboarding.permissions

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionScreen(onPermissionsGranted: () -> Unit) {
    val context = LocalContext.current

    val openAppSettings = {
        val intent =
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
        context.startActivity(intent)
    }

    // Stage 1: Foreground
    val foregroundPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    var foregroundRequested by rememberSaveable { mutableStateOf(false) }

    // Stage 2: Background (Requires Foreground first)
    // Only available on Q+
    val backgroundPermissionName =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        } else {
            // No background permission needed pre-Q, effectively granted
            Manifest.permission.ACCESS_FINE_LOCATION
        }

    val backgroundPermissionState = rememberPermissionState(backgroundPermissionName)
    var backgroundRequested by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(foregroundPermissionState.status, backgroundPermissionState.status) {
        if (foregroundPermissionState.status.isGranted) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || backgroundPermissionState.status.isGranted) {
                onPermissionsGranted()
            }
        }
    }

    Scaffold { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Location Permissions",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (!foregroundPermissionState.status.isGranted) {
                // Foreground Request
                val isPermanentlyDenied = foregroundRequested && !foregroundPermissionState.status.shouldShowRationale

                Text(
                    text = "Locus needs 'While Using' location permission to record your tracks.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(32.dp))

                if (isPermanentlyDenied) {
                    Button(onClick = openAppSettings) {
                        Text("Open Settings")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Permission permanently denied. Please enable it in Settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                } else {
                    Button(onClick = {
                        foregroundRequested = true
                        foregroundPermissionState.launchPermissionRequest()
                    }) {
                        Text("Grant Foreground Location")
                    }
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !backgroundPermissionState.status.isGranted) {
                // Background Request (Android 10+)
                val isPermanentlyDenied = backgroundRequested && !backgroundPermissionState.status.shouldShowRationale

                Text(
                    text = "To track you while the screen is off, Locus needs 'Allow all the time' location permission.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "On the next screen, please select 'Allow all the time'.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(32.dp))

                if (isPermanentlyDenied) {
                    Button(onClick = openAppSettings) {
                        Text("Open Settings")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Permission permanently denied. Please enable it in Settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                } else {
                    Button(onClick = {
                        backgroundRequested = true
                        backgroundPermissionState.launchPermissionRequest()
                    }) {
                        Text("Grant Background Location")
                    }
                }
            } else {
                Text("All permissions granted!")
            }
        }
    }
}
