package com.example.devs.ui.activity

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import com.example.devs.R
import com.example.devs.base.BaseActivity
import com.example.devs.databinding.ActivityMapBinding
import com.example.devs.db.entity.LocationEntity
import com.example.devs.model.Directions
import com.example.devs.ui.adapter.SearchAddressAdapter
import com.example.devs.ui.repository.PlacesRepository
import com.example.devs.ui.viewmodel.LocationViewModel
import com.example.devs.utils.Constants
import com.example.devs.utils.PathDrawer
import com.example.devs.utils.finishActivityWithLauncherResult
import com.example.devs.utils.gone
import com.example.devs.utils.visible
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.gson.Gson
import com.google.maps.android.ui.IconGenerator
import dagger.hilt.android.AndroidEntryPoint
import org.json.JSONObject
import java.util.Locale

@AndroidEntryPoint
class MapActivity : BaseActivity<LocationViewModel, ActivityMapBinding>(), OnMapReadyCallback,
    GoogleMap.OnCameraIdleListener {

    private lateinit var mMap: GoogleMap
    private var mapFor = Constants.MapFor.ADD //Info: default "ADD"
    private lateinit var placesRepository: PlacesRepository
    private lateinit var placesClient: PlacesClient
    private lateinit var locationEntityForEdit: LocationEntity
    private lateinit var newLocationEntityForEdit: LocationEntity

    private val searchAddressAdapter by lazy {
        SearchAddressAdapter(itemClickCallback = ::fetchPlaceDetails)
    }

    private fun fetchPlaceDetails(
        placeId: String?,
        primaryAddress: String?,
        mainAddress: String?,
    ) {
        val placeFields =
            listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
        val request = placeId?.let { FetchPlaceRequest.newInstance(it, placeFields) }

        if (request != null) {
            placesClient.fetchPlace(request).addOnSuccessListener { response ->
                val place = response.place
                val name = place.name
                val latLng = place.latLng
                if (latLng != null) {
                    val lc = LocationEntity(
                        primaryAddress = primaryAddress, city = mainAddress.toString(),
                        latitude = latLng.latitude, longitude = latLng.longitude
                    )
                    if (mapFor == Constants.MapFor.EDIT) {
                        binding.includeSaveNewAddress.root.visible()
                        newLocationEntityForEdit = LocationEntity(
                            id = locationEntityForEdit.id,
                            primaryAddress = primaryAddress, city = mainAddress.toString(),
                            latitude = latLng.latitude, longitude = latLng.longitude
                        )
                        if (::mMap.isInitialized) {
                            val newLatLng =
                                newLocationEntityForEdit.latitude?.let { lat ->
                                    newLocationEntityForEdit.longitude?.let { lng ->
                                        LatLng(lat, lng)
                                    }
                                }
                            newLatLng?.let { ltLng ->
                                CameraUpdateFactory.newLatLngZoom(ltLng, 12.0f)
                            }?.let { mMap.animateCamera(it) }
                        }
                    } else {
                        finishActivityWithLauncherResult(bundle = Bundle().apply {
                            putParcelable(
                                Constants.BUNDLE_KEY_ADDRESS_ITEM,
                                lc
                            )
                        })
                    }
                }
            }.addOnFailureListener { exception ->
                exception.printStackTrace()
            }
        }
    }

    override fun getViewBinding() = ActivityMapBinding.inflate(layoutInflater)

    override fun initSetup() {
        initPlacePicker()
        initMap()
        getBundle()
        setAdapter()
        searchingAddress()
        listeners()
    }

    private fun initPlacePicker() {
        if (!Places.isInitialized()) {
            Places.initialize(
                applicationContext,
                "AIzaSyBSNyp6GQnnKlrMr7hD2HGiyF365tFlK5U", Locale.US
            )
        } else {
            placesClient = Places.createClient(this)
            placesRepository = PlacesRepository(placesClient)
        }
    }

    private fun listeners() {
        binding.includeSaveNewAddress.buttonSave.setOnClickListener {
            if (mapFor == Constants.MapFor.EDIT) {
                finishActivityWithLauncherResult(bundle = Bundle().apply {
                    putParcelable(
                        Constants.BUNDLE_KEY_ADDRESS_ITEM,
                        newLocationEntityForEdit
                    )
                    putString(Constants.BUNDLE_KEY_MAP_FOR, Constants.MapFor.EDIT)
                })
            }
        }
    }

    private fun searchingAddress() {
        binding.includeAddress.editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int,
            ) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val text = s.toString()
                if (!text.trim().isNullOrEmpty()) {
                    placesRepository.getAutocompletePredictions(text) { predictions ->
                        if (predictions.isNotEmpty()) {
                            searchAddressAdapter.setPredictions(predictions)
                        }
                    }
                }
                if (text.trim().isEmpty()) {
                    searchAddressAdapter.mList = emptyList()
                    searchAddressAdapter.notifyDataSetChanged()
                }
            }
        })
    }

    private fun setAdapter() {
        if (mapFor != Constants.MapFor.PATH) {
            binding.includeAddress.recyclerViewAddresses.adapter = searchAddressAdapter
        } else {
            //Nothing.
            //Info : only view purpose for marker & path
        }
    }

    private fun getBundle() {
        if (intent.extras?.getString(Constants.BUNDLE_KEY_MAP_FOR) != null) {
            mapFor = intent.extras?.getString(Constants.BUNDLE_KEY_MAP_FOR)!!
            if (mapFor == Constants.MapFor.ADD) {
                binding.imageViewPin.gone()
                binding.includeSaveNewAddress.root.gone()
            } else if (mapFor == Constants.MapFor.EDIT) {
                locationEntityForEdit =
                    intent.extras?.getParcelable(Constants.BUNDLE_KEY_LOCATION_ITEM)!!
                binding.includeSaveNewAddress.root.gone()
            } else if (mapFor == Constants.MapFor.PATH) {
                binding.imageViewPin.gone()
                binding.includeSaveNewAddress.root.gone()
                binding.includeAddress.root.gone()
            } else {
                // none
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setOnCameraIdleListener(this@MapActivity)
        googleMap.setMapStyle(
            MapStyleOptions.loadRawResourceStyle(
                this, R.raw.map_style_dark
            )
        )

        if (mapFor == Constants.MapFor.ADD) {
            mapReadyForAdd()
        } else if (mapFor == Constants.MapFor.EDIT) {
            mapReadyForEdit()
        } else if (mapFor == Constants.MapFor.PATH) {
            mapReadyForPath()
        } else {
            /// none
        }
    }

    private fun mapReadyForPath() {
        val bundle = intent.extras?.getBundle(Constants.INTENT_BUNDLE)
        val mList =
            bundle?.getParcelableArrayList<LocationEntity>(Constants.BUNDLE_KEY_LOCATION_LIST)
        val stopList = if (mList?.size!! > 2) mList.drop(1)?.dropLast(1) else listOf()

        val url = getDirectionsUrl(
            origin = LatLng(
                mList.first()?.latitude!!,
                mList.first()?.longitude!!
            ),
            dest = LatLng(mList.last()?.latitude!!, mList.last()?.longitude!!),
            markerPoints = stopList
        )
        Handler(Looper.getMainLooper()).postDelayed({
            polylineDrawer.downloadTaskExecute(url) {
                drawPolylineIntoMap(it) {
                    addStopsMarker(mList)
                }
            }
        }, 300)
    }

    private fun mapReadyForEdit() {
        Handler(Looper.getMainLooper()).postDelayed({
            val latLng =
                locationEntityForEdit.latitude?.let { lat ->
                    locationEntityForEdit.longitude?.let { lng ->
                        LatLng(lat, lng)
                    }
                }
            latLng?.let { ltLng ->
                CameraUpdateFactory.newLatLngZoom(ltLng, 12.0f)
            }?.let { mMap.animateCamera(it) }
        }, 100)
    }

    private fun mapReadyForAdd() {
        Handler(Looper.getMainLooper()).postDelayed({
            val india = LatLng(20.5937, 78.9629)
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(india, 4.0f))
        }, 100)
    }

    private fun addStopsMarker(
        mList: List<LocationEntity>?,
    ) {
        mList?.forEachIndexed { index, data ->
            mMap.addMarker(
                MarkerOptions()
                    .position(LatLng(data.latitude!!, data.longitude!!))
                    .icon(
                        getStopsMarker(
                            context = this@MapActivity,
                            text = mList[index].primaryAddress.toString()
                        )
                    )
            )
        }
    }

    private fun getStopsMarker(context: Context, text: String): BitmapDescriptor {
        val iconGenerator = IconGenerator(context)
        iconGenerator.setBackground(null)
        val inflatedView = View.inflate(context, R.layout.marker_view, null)
        val textView = inflatedView?.findViewById<AppCompatTextView>(R.id.textViewStopTitle)
        textView?.text = text
        iconGenerator.setContentView(inflatedView)
        return BitmapDescriptorFactory.fromBitmap(iconGenerator.makeIcon())
    }

    private fun getDirectionsUrl(
        origin: LatLng,
        dest: LatLng,
        markerPoints: List<LocationEntity> = listOf<LocationEntity>(),
    ): String {
        val str_origin = "origin=" + origin.latitude + "," + origin.longitude
        val str_dest = "destination=" + dest.latitude + "," + dest.longitude
        val sensor = "sensor=false&units=metric&mode=driving&alternatives=false"
        var waypoints = ""
        for ((position, data) in markerPoints.withIndex()) {
            if (position == 0) waypoints = "&waypoints=optimize:false|"
            waypoints = waypoints + data.latitude + "," + data.longitude + "|"
        }
        val parameters: String = "$str_origin$waypoints&$str_dest&$sensor"
        val output = "json"
        val key =
            "&key=AIzaSyBSNyp6GQnnKlrMr7hD2HGiyF365tFlK5U"

        //return url
        return "https://maps.googleapis.com/maps/api/directions/$output?$parameters$key"
    }

    private fun drawPolylineIntoMap(
        jObject: JSONObject = JSONObject(),
        durationInSecond: ((Int) -> Unit)? = null,
    ) {
        val directions: Directions? = Gson().fromJson(jObject.toString(), Directions::class.java)
        PathDrawer(this@MapActivity).apply {
            setMap(mMap)
            if (directions?.routes?.isNotEmpty() == true) {
                newPolyline(directions.routes ?: listOf())
                pathColor(R.color.black_white_dark_mode)
                pathWidth(5)
                animate(true)
                draw()
                durationInSecond?.invoke(this.durationInSecond)
            } else {
                mMap.clear()
            }
        }
    }

    private fun initMap() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onCameraIdle() {}

}