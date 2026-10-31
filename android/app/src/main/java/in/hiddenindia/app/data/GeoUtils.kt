package `in`.hiddenindia.app.data

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Exact port of backend/src/services/geoService.ts's calculateDistanceKm +
 * findNearestDestination, so "nearest gem" distances match the web app's numbers.
 */
object GeoUtils {
    fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = (lat2 - lat1) * Math.PI / 180
        val dLon = (lon2 - lon1) * Math.PI / 180
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(lat1 * Math.PI / 180) * cos(lat2 * Math.PI / 180) * sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return round(r * c * 10) / 10
    }

    fun findNearestDestination(lat: Double, lng: Double, destinations: List<Destination>): Pair<Destination, Double>? {
        var nearest: Destination? = null
        var minDistance = Double.MAX_VALUE
        for (dest in destinations) {
            val d = calculateDistanceKm(lat, lng, dest.coordinates.lat, dest.coordinates.lng)
            if (d < minDistance) {
                minDistance = d
                nearest = dest
            }
        }
        return nearest?.let { it to minDistance }
    }
}
