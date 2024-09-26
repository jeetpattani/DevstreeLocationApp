package com.example.devs.ui.repository

import com.example.devs.db.dao.LocationDao
import com.example.devs.db.entity.LocationEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class LocationRepository @Inject constructor(
    private var locationDao: LocationDao,
) {

    fun addLocation(locationEntity: LocationEntity): Flow<Unit> = flow {
        locationDao.addLocation(locationEntity)
    }

    fun updateLocation(
        id: Int,
        primaryAddress: String,
        city: String,
        latitude: Double,
        longitude: Double,
        distance:Double
    ): Flow<Unit> = flow {
        locationDao.updateLocation(id, primaryAddress, city, latitude, longitude, distance)
    }

    fun deleteLocation(locationEntity: LocationEntity): Flow<Unit> = flow {
        locationDao.deleteLocation(locationEntity)
    }

    fun getAllLocations(): Flow<List<LocationEntity>> {
        return locationDao.getAllLocations()
    }

    fun getLocationsDistanceAsc(): Flow<List<LocationEntity>> {
        return locationDao.getLocationsDistanceAsc()
    }

    fun getLocationsDistanceDes(): Flow<List<LocationEntity>> {
        return locationDao.getLocationsDistanceDes()
    }

    fun markAsPrimary(id: Int): Int {
        return locationDao.markAsPrimary(id)
    }

    suspend fun removePrimaryMark() {
        return locationDao.removePrimaryMark()
    }

}