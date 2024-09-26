package com.example.devs.utils

import com.google.android.gms.maps.model.LatLng
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

fun haversineDistance(pointA: LatLng, pointB: LatLng): Double {
    val earthRadius = 6371.0 // Earth radius in kilometers

    // Convert degrees to radians
    val latA = Math.toRadians(pointA.latitude)
    val lonA = Math.toRadians(pointA.longitude)
    val latB = Math.toRadians(pointB.latitude)
    val lonB = Math.toRadians(pointB.longitude)

    // Haversine formula
    val dLat = latB - latA
    val dLon = lonB - lonA

    val a = sin(dLat / 2).pow(2) + cos(latA) * cos(latB) * sin(dLon / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))

    return earthRadius * c // Distance in kilometers
}