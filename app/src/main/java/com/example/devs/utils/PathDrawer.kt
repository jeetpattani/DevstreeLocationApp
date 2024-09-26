package com.example.devs.utils

import android.animation.*
import android.content.Context
import android.util.TypedValue
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.example.devs.R
import com.example.devs.model.Routes
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import com.google.assistant.appactions.testing.aatl.converter.ValidationException
import com.google.maps.android.PolyUtil
import com.google.maps.android.SphericalUtil
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.sign

@Singleton
class PathDrawer @Inject constructor(private val context: Context) {

    private var color: Int = 0
    private var width: Int = 0
    private var cameraPadding: Int = 0
    private var startMarker: Marker? = null
    private var endMarker: Marker? = null
    private var currentMarker: Marker? = null
    private var polyline: Polyline? = null

    //private Polyline greyPolyLine;
    private var googleMap: GoogleMap? = null
    private var latLngs: MutableList<LatLng> = mutableListOf()
    private var animation: Boolean = false
    private var isNewPath: Boolean = false
    var durationInSecond = 0

    init {
        this.color = ContextCompat.getColor(context, R.color.colorPrimary)
        this.width = 4
        this.cameraPadding = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            20f,
            context.resources.displayMetrics
        ).toInt()
        this.latLngs = ArrayList()
        this.animation = true
        this.isNewPath = true
    }

    fun setMap(googleMap: GoogleMap) {
        this.googleMap = googleMap
        this.currentMarker = null
    }

    fun pathColor(@ColorRes color: Int): PathDrawer {
        this.color = ContextCompat.getColor(context, color)
        return this
    }

    fun pathWidth(width: Int): PathDrawer {
        this.width = width
        return this
    }

    fun animate(animate: Boolean): PathDrawer {
        this.animation = animate
        return this
    }

    fun newPolyline(routes: List<Routes>): PathDrawer {
        val getData = getPolylinePointsAndDuration(routes)
        this.latLngs.clear()
        this.latLngs.addAll(getData.first)
        this.durationInSecond = getData.second
        return this
    }

    @Throws(ValidationException::class)
    fun draw() {

        if (isNewPath)
            googleMap!!.clear()

        if (latLngs.size > 0) {

            if (polyline != null) polyline!!.remove()
            polyline = googleMap!!.addPolyline(buildPolyline(latLngs, color, width))

            if (isNewPath) {
                googleMap!!.moveCamera(buildCamera(latLngs, cameraPadding))
                isNewPath = false
            } else {
                googleMap!!.animateCamera(buildCamera(latLngs, cameraPadding))
                animateMarker(startMarker, latLngs[0])
                animateBearing(startMarker, latLngs[0])
            }

        } else {
            throw ValidationException("Path not found!")
        }
    }

    /**
     * To create polyline
     *
     * @param points locations to include in polyline
     * @param color  color of polyline
     * @param width  width of polyline
     * @return polyline options
     */
    private fun buildPolyline(points: List<LatLng>, color: Int, width: Int): PolylineOptions {
        return PolylineOptions()
            .color(color)
            .width(width.toFloat())
            .startCap(RoundCap())
            .endCap(RoundCap())
            .addAll(points)
    }

    /**
     * To build camera bound with given locations
     *
     * @param path    list of locations
     * @param padding padding for camera
     * @return camera update
     */
    fun buildCamera(path: List<LatLng>?, padding: Int): CameraUpdate {
        val builder = LatLngBounds.Builder()
        if (path != null && path.isNotEmpty()) {
            for (latLng in path) {
                builder.include(latLng)
            }
        } else {
            if (startMarker != null)
                builder.include(startMarker!!.position)
            if (endMarker != null)
                builder.include(endMarker!!.position)
        }
        return CameraUpdateFactory.newLatLngBounds(builder.build(), padding)
    }

    /**
     * To get location points from polyline
     *
     * @param routes routes fetched from google API
     * @return Pair of List of location points in the path and total time in second
     */
    fun getPolylinePointsAndDuration(routes: List<Routes>?): Pair<MutableList<LatLng>, Int> {
        val points = ArrayList<LatLng>()
        var durationInSecond = 0
        routes?.let {
            val legs = it[0].legs ?: emptyList()
            for (element in legs) {
                durationInSecond += (element.duration?.value ?: 0)
                val steps = element.steps ?: emptyList()
                for (step in steps) {
                    points.addAll(PolyUtil.decode(step.polyline?.points))
                }
            }
        }
        return Pair(points, durationInSecond)
    }

    /**
     * To animate marker while driving
     *
     * @param marker marker to animate
     * @param latLng location to which animate
     */
    fun animateMarker(marker: Marker?, latLng: LatLng) {
        if (marker == null) {
            return
        }
        val animation = ValueAnimator.ofFloat(0f, 100f)
        var previousStep = 0f
        val deltaLatitude = latLng.latitude - marker.position.latitude
        val deltaLongitude = latLng.longitude - marker.position.longitude
        animation.duration = 1200
        animation.addUpdateListener { updatedAnimation ->
            val deltaStep = updatedAnimation.animatedValue as Float - previousStep
            previousStep = updatedAnimation.animatedValue as Float
            marker.position = LatLng(
                marker.position.latitude + deltaLatitude * deltaStep * 1 / 100,
                marker.position.longitude + deltaStep * deltaLongitude * 1 / 100
            )
        }
        animation.start()
    }

    /**
     * To animate the bearing of marker on map
     *
     * @param marker to animate marker
     * @param latLng location to which animate
     */
    fun animateBearing(marker: Marker?, latLng: LatLng) {
        val bearingAnimator =
            ValueAnimator.ofFloat(marker!!.rotation, getBearing(marker.position, latLng))
        bearingAnimator.interpolator = AccelerateDecelerateInterpolator()
        bearingAnimator.duration = MARKER_BEARING_ANIMATION_DURATION.toLong()
        bearingAnimator.addUpdateListener { valueAnimator ->
            val value = valueAnimator.animatedValue as Float
            marker.rotation = value
        }
        bearingAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
            }
        })
        bearingAnimator.start()
    }

    /**
     * To get bearing from two @[LatLng]
     *
     * @param start starting LatLng
     * @param end   ending LatLng
     * @return Bearing value
     */
    fun getBearing(start: LatLng?, end: LatLng?): Float {
        if (start != null) {
            if (end != null) {
                return SphericalUtil.computeHeading(start, end).toFloat()
            }
        }
        return 0f
    }

    /**
     * LatLng Interpolator to animate markers using LatLng parameters.
     */
    interface LatLngInterpolatorNew {

        fun interpolate(fraction: Float, a: LatLng, b: LatLng): LatLng

        class LinearFixed : LatLngInterpolatorNew {
            override fun interpolate(fraction: Float, a: LatLng, b: LatLng): LatLng {
                val lat = (b.latitude - a.latitude) * fraction + a.latitude
                var lngDelta = b.longitude - a.longitude
                // Take the shortest path across the 180th meridian.
                if (abs(lngDelta) > 180) {
                    lngDelta -= sign(lngDelta) * 360
                }
                val lng = lngDelta * fraction + a.longitude
                return LatLng(lat, lng)
            }
        }
    }

    companion object {
        private val MARKER_BEARING_ANIMATION_DURATION = 1000 * 1  // 1   Seconds
    }
}