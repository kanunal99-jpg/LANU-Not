package com.lanu.notdefteri.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import java.util.Locale

data class UserLocation(
    val latitude: Double,
    val longitude: Double,
    val addressName: String
)

object LocationHelper {

    /**
     * Attempts to fetch the device's current location using LocationManager.
     */
    @SuppressLint("MissingPermission")
    fun getCurrentLocation(
        context: Context,
        onSuccess: (UserLocation) -> Unit,
        onError: (String) -> Unit
    ) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (locationManager == null) {
            onError("Konum servisi kullanılamıyor.")
            return
        }

        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!isGpsEnabled && !isNetworkEnabled) {
            onError("Cihazda GPS veya Konum servisleri kapalı.")
            return
        }

        val provider = when {
            isGpsEnabled -> LocationManager.GPS_PROVIDER
            else -> LocationManager.NETWORK_PROVIDER
        }

        try {
            val lastKnown = locationManager.getLastKnownLocation(provider)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            if (lastKnown != null) {
                val address = getAddressFromLocation(context, lastKnown.latitude, lastKnown.longitude)
                onSuccess(UserLocation(lastKnown.latitude, lastKnown.longitude, address))
                return
            }

            // Request single update if last known is null
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    locationManager.removeUpdates(this)
                    val address = getAddressFromLocation(context, location.latitude, location.longitude)
                    onSuccess(UserLocation(location.latitude, location.longitude, address))
                }

                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }

            locationManager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
        } catch (e: Exception) {
            e.printStackTrace()
            onError("Konum alınamadı: ${e.localizedMessage}")
        }
    }

    /**
     * Resolves human-readable address or neighborhood name from coordinates.
     */
    fun getAddressFromLocation(context: Context, lat: Double, lng: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale("tr", "TR"))
            if (Build.VERSION.SDK_INT >= 33) {
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    val subLocality = addr.subLocality ?: addr.thoroughfare ?: addr.locality ?: addr.adminArea
                    val locality = addr.locality ?: addr.adminArea ?: ""
                    if (!subLocality.isNullOrBlank()) "$subLocality, $locality" else "$lat, $lng"
                } else {
                    "Konum: %.4f, %.4f".format(lat, lng)
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    val subLocality = addr.subLocality ?: addr.thoroughfare ?: addr.locality ?: addr.adminArea
                    val locality = addr.locality ?: addr.adminArea ?: ""
                    if (!subLocality.isNullOrBlank()) "$subLocality, $locality" else "$lat, $lng"
                } else {
                    "Konum: %.4f, %.4f".format(lat, lng)
                }
            }
        } catch (e: Exception) {
            "Konum: %.4f, %.4f".format(lat, lng)
        }
    }

    /**
     * Calculates distance in meters between two lat/lng points.
     */
    fun calculateDistanceMeters(
        startLat: Double, startLng: Double,
        endLat: Double, endLng: Double
    ): Float {
        val results = FloatArray(1)
        Location.distanceBetween(startLat, startLng, endLat, endLng, results)
        return results[0]
    }
}
