package com.example.devs.ui.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.devs.base.BaseViewModel
import com.example.devs.db.entity.LocationEntity
import com.example.devs.ui.repository.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LocationViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
) :
    BaseViewModel() {

    private val _addLocation = MutableSharedFlow<Unit?>()
    val addLocationStateFlow: SharedFlow<Unit?> = _addLocation.asSharedFlow()
    fun addLocation(locationEntity: LocationEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            locationRepository.addLocation(locationEntity).collect { response ->
                _addLocation.emit(response)
            }
        }
    }

    private val _updateLocation = MutableSharedFlow<Unit?>()
    val updateLocationFlow: SharedFlow<Unit?> = _updateLocation.asSharedFlow()
    fun updateLocation(
        id: Int,
        primaryAddress: String,
        city: String,
        latitude: Double,
        longitude: Double,
        distance : Double
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            locationRepository.updateLocation(id, primaryAddress, city, latitude, longitude, distance)
                .collect { response ->
                    _updateLocation.emit(response)
                }
        }
    }

    private val _deleteLocation = MutableSharedFlow<Unit?>()
    val deleteLocationFlow: SharedFlow<Unit?> = _deleteLocation
    fun deleteLocation(locationEntity: LocationEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            locationRepository.deleteLocation(locationEntity).collect { response ->
                _deleteLocation.emit( response)
            }
        }
    }

    private val _getAllLocations = MutableSharedFlow<List<LocationEntity>?>()
    val getAllLocationsFlow: SharedFlow<List<LocationEntity>?> = _getAllLocations.asSharedFlow()
    fun getAllLocations() {
        viewModelScope.launch(Dispatchers.IO) {
            locationRepository.getAllLocations().collect { response ->
                _getAllLocations.emit(response)
            }
        }
    }

    private val _getLocationsDistanceAsc = MutableSharedFlow<List<LocationEntity>?>()
    val getLocationsDistanceAscFlow: SharedFlow<List<LocationEntity>?> = _getLocationsDistanceAsc.asSharedFlow()
    fun getLocationsDistanceAsc() {
        viewModelScope.launch(Dispatchers.IO) {
            locationRepository.getLocationsDistanceAsc().collect { response ->
                _getLocationsDistanceAsc.emit(response)
            }
        }
    }

    private val _getLocationsDistanceDes = MutableSharedFlow<List<LocationEntity>?>()
    val getLocationsDistanceDesFlow: SharedFlow<List<LocationEntity>?> = _getLocationsDistanceDes.asSharedFlow()
    fun getLocationsDistanceDes() {
        viewModelScope.launch(Dispatchers.IO) {
            locationRepository.getLocationsDistanceDes().collect { response ->
                _getLocationsDistanceDes.emit(response)
            }
        }
    }

    fun markAsPrimary(id:Int) {
        viewModelScope.launch(Dispatchers.IO) {
            locationRepository.markAsPrimary(id)
        }
    }

    fun removePrimaryMark() {
        viewModelScope.launch(Dispatchers.IO) {
            locationRepository.removePrimaryMark()
        }
    }


}