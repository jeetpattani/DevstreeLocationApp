package com.example.devs.ui.activity

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.viewModels
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.devs.R
import com.example.devs.base.BaseActivity
import com.example.devs.databinding.ActivityModifyLocationBinding
import com.example.devs.db.entity.LocationEntity
import com.example.devs.ui.adapter.LocationAdapter
import com.example.devs.ui.viewmodel.LocationViewModel
import com.example.devs.utils.Constants
import com.example.devs.utils.haversineDistance
import com.example.devs.utils.startActivityWithLauncher
import com.example.devs.utils.startNewActivity
import com.example.devs.utils.withNotNull
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ModifyLocationActivity : BaseActivity<LocationViewModel, ActivityModifyLocationBinding>() {

    private val viewModel: LocationViewModel by viewModels()
    private var isAscending = true
    private val locationAdapter by lazy {
        LocationAdapter(onEventListener = { view, adapterPos ->
            onEventListener(view, adapterPos)
        })
    }

    private fun onEventListener(view: View, adapterPos: Int) {
        when (view.id) {
            R.id.imageViewDelete -> {
                showDeleteConfirmationDialog(adapterPos)
            }

            R.id.imageViewEdit -> {
                activityLauncher.startActivityWithLauncher(
                    context = this,
                    MapActivity::class.java,
                    bundle = bundleOf(
                        Constants.BUNDLE_KEY_MAP_FOR to Constants.MapFor.EDIT,
                        Constants.BUNDLE_KEY_LOCATION_ITEM to locationAdapter.mList[adapterPos]
                    )
                ) {
                    if (it.resultCode == RESULT_OK) {
                        // update
                        if (it.data?.extras?.getString(Constants.BUNDLE_KEY_MAP_FOR) != null &&
                            it.data?.extras?.getString(Constants.BUNDLE_KEY_MAP_FOR) == Constants.MapFor.EDIT
                        ) {
                            it.data?.extras?.getParcelable<LocationEntity>(Constants.BUNDLE_KEY_ADDRESS_ITEM)
                                .withNotNull {
                                    locationAdapter.mList.find { it.isPrimary == true }?.let { item ->
                                        val distance = haversineDistance(
                                            LatLng(item.latitude ?: 0.0, item.longitude ?: 0.0),
                                            LatLng(
                                                it.latitude ?: 0.0,
                                                it.longitude ?: 0.0
                                            )
                                        )
                                        it.distance = distance
                                    }
                                    locationAdapter.notifyDataSetChanged()
                                    dbUpdateLocation(it)
                                }
                            dbGetAllLocationsFlow()
                        }
                    }
                }
            }
        }
    }

    override fun getViewBinding() = ActivityModifyLocationBinding.inflate(layoutInflater)

    override fun initSetup() {
        clickListeners()
        setAdapter()
        dbGetAllLocationsFlow()
        flowObserve()
    }

    private fun setAdapter() {
        binding.recyclerViewAddresses.adapter = locationAdapter
    }

    private fun clickListeners() = with(binding) {
        buttonSort.setOnClickListener { manageSortingDialog() }
        buttonPOI.setOnClickListener { onClickButtonPOI() }
        buttonPath.setOnClickListener { onClickButtonPath() }
    }

    private fun onClickButtonPOI() {
        activityLauncher.startActivityWithLauncher(
            this,
            MapActivity::class.java,
            bundle = bundleOf(
                Constants.BUNDLE_KEY_MAP_FOR to Constants.MapFor.ADD
            ),
        ) {
            if (it.resultCode == RESULT_OK) {
                // add
                it.data?.extras?.getParcelable<LocationEntity>(Constants.BUNDLE_KEY_ADDRESS_ITEM)
                    .withNotNull { locationEntity ->
                        locationAdapter.mList.find { it.isPrimary == true }?.let { item ->
                            val distance = haversineDistance(
                                LatLng(item.latitude ?: 0.0, item.longitude ?: 0.0),
                                LatLng(
                                    locationEntity.latitude ?: 0.0,
                                    locationEntity.longitude ?: 0.0
                                )
                            )
                            locationEntity.distance = distance
                        }
                        locationAdapter.mList.add(locationEntity)
                        dbAddLocation(locationEntity)

                        if (locationAdapter.mList.size == 1) {
                            locationAdapter.mList[0].isPrimary = true
                            dbMarkAsPrimary(locationEntity.id)
                            locationAdapter.notifyDataSetChanged()
                        }
                    }
                dbGetAllLocationsFlow()
            }
        }
    }

    private fun onClickButtonPath() {
        if (locationAdapter.mList.size > 1) {
            val bundle = Bundle()
            bundle.putParcelableArrayList(
                Constants.BUNDLE_KEY_LOCATION_LIST,
                locationAdapter.mList
            )
            startNewActivity(
                MapActivity::class.java,
                bundle = bundleOf(
                    Constants.BUNDLE_KEY_MAP_FOR to Constants.MapFor.PATH,
                    Constants.INTENT_BUNDLE to bundle
                )
            )
        } else {
            Snackbar.make(
                findViewById(android.R.id.content),
                getString(R.string.message_please_add_2_or_more_locations),
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    private fun manageSortingDialog() {
        openSortBottomSheet(
            selectedPos = isAscending, isAscendingCallback = { selectedPos ->
                isAscending = if (selectedPos == 0) {
                    getLocationsDistanceAsc()
                    true //ascending
                } else {
                    getLocationsDistanceDes()
                    false //descending
                }
            }
        )
    }

    private fun openSortBottomSheet(
        selectedPos: Boolean = true, //default 0 pos selected
        isAscendingCallback: (selectedPos: Int) -> Unit,
    ) {
        AlertDialog.Builder(this)
            .setTitle("Sorting by..")
            .setSingleChoiceItems(
                arrayOf("Ascending location", "Descending location"),
                if (selectedPos) 0 else 1,
                DialogInterface.OnClickListener { dialog: DialogInterface, index: Int ->
                    dialog.dismiss()
                    if (index == 0) {
                        isAscendingCallback.invoke(0)
                    } else {
                        isAscendingCallback.invoke(1)
                    }
                }).create().show()
    }

    private fun showDeleteConfirmationDialog(adapterPos: Int) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.label_delete_location))
        builder.setMessage(getString(R.string.label_are_you_sure_you_want_to_delete_this_location))
        builder.setPositiveButton(getString(R.string.label_delete)) { dialog, which ->
            deleteLocationLogic(adapterPos)
        }
        builder.setNegativeButton(getString(R.string.label_cancel)) { dialog, _ ->
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.show()
    }

    private fun deleteLocationLogic(adapterPos: Int) {
        if (locationAdapter.mList[adapterPos].isPrimary) {
            if (locationAdapter.mList.size == 1) {
                dbDeleteLocation(locationAdapter.mList[adapterPos])
                removePrimaryMark()
            } else {
                Snackbar.make(
                    findViewById(android.R.id.content),
                    getString(R.string.message_primary_location_can_be_deleted_only_if_it_s_the_last_one),
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        } else {
            dbDeleteLocation(locationAdapter.mList[adapterPos])
        }

    }

    /**
     * DB Controllers
     * */

    private fun dbAddLocation(locationEntity: LocationEntity) {
        viewModel.addLocation(locationEntity)
    }

    private fun dbUpdateLocation(locationEntity: LocationEntity) {
        viewModel.updateLocation(
            id = locationEntity.id,
            primaryAddress = locationEntity.primaryAddress.toString(),
            city = locationEntity.city.toString(),
            latitude = locationEntity.latitude ?: 0.0,
            longitude = locationEntity.longitude ?: 0.0,
            distance = locationEntity.distance ?: 0.0
        )
    }

    private fun dbDeleteLocation(locationEntity: LocationEntity) {
        viewModel.deleteLocation(locationEntity)
    }

    private fun dbGetAllLocationsFlow() {
        viewModel.getAllLocations()
    }

    private fun dbMarkAsPrimary(id: Int) {
        viewModel.markAsPrimary(id)
    }

    private fun removePrimaryMark() {
        viewModel.removePrimaryMark()
    }

    private fun getLocationsDistanceAsc() {
        viewModel.getLocationsDistanceAsc()
    }

    private fun getLocationsDistanceDes() {
        viewModel.getLocationsDistanceDes()
    }

    private fun flowObserve() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.getAllLocationsFlow.collect { response ->
                        if (response != null) {
                            Handler(Looper.getMainLooper()).postDelayed({
                                locationAdapter.mList.clear()
                                locationAdapter.mList.addAll(response)
                                locationAdapter.notifyDataSetChanged()
                            }, 300)
                        }
                    }
                }
                launch {
                    viewModel.getLocationsDistanceAscFlow.collect { response ->
                        if (response != null) {
                            Handler(Looper.getMainLooper()).postDelayed({
                                locationAdapter.mList.clear()
                                locationAdapter.mList.addAll(response)
                                locationAdapter.notifyDataSetChanged()
                            }, 300)
                        }
                    }
                }
                launch {
                    viewModel.getLocationsDistanceDesFlow.collect { response ->
                        if (response != null) {
                            Handler(Looper.getMainLooper()).postDelayed({
                                locationAdapter.mList.clear()
                                locationAdapter.mList.addAll(response)
                                locationAdapter.notifyDataSetChanged()
                            }, 300)
                        }
                    }
                }
            }
        }
    }
}