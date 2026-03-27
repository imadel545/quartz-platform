package com.quartz.platform.device.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.quartz.platform.domain.model.UserLocation
import com.quartz.platform.domain.repository.LocationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AndroidLocationRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : LocationRepository {

    @SuppressLint("MissingPermission")
    override suspend fun getLastKnownLocation(): UserLocation? {
        if (!hasLocationPermission()) return null

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null

        val providers = runCatching { locationManager.getProviders(true) }.getOrNull().orEmpty()
        if (providers.isEmpty()) return null

        val latest = providers
            .mapNotNull { provider -> runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull() }
            .maxByOrNull(Location::getTime)
            ?: return null

        return UserLocation(
            latitude = latest.latitude,
            longitude = latest.longitude,
            capturedAtEpochMillis = latest.time.takeIf { it > 0L }
        )
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fine || coarse
    }
}
