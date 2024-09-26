package com.example.devs.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.devs.db.entity.LocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addLocation(locationEntity: LocationEntity)

    @Query("UPDATE location SET primaryAddress = :primaryAddress, city = :city,latitude = :latitude ,longitude = :longitude, distance = :distance WHERE id = :id")
    suspend fun updateLocation(
        id: Int,
        primaryAddress: String,
        city: String,
        latitude: Double,
        longitude: Double,
        distance: Double
    )

    @Delete
    fun deleteLocation(location: LocationEntity)

    @Query("SELECT * FROM location")
    fun getAllLocations(): Flow<List<LocationEntity>>

    @Query("SELECT * FROM location WHERE id = :id LIMIT 1")
    fun getLocationById(id: Int): Flow<LocationEntity>

    @Query("SELECT * FROM location ORDER BY distance ASC")
    fun getLocationsDistanceAsc(): Flow<List<LocationEntity>> // Ascending

    @Query("SELECT * FROM location ORDER BY distance DESC")
    fun getLocationsDistanceDes(): Flow<List<LocationEntity>> // Descending

    @Query("UPDATE location SET isPrimary = 1 WHERE id = :id") // Mark as primary
    fun markAsPrimary(id: Int): Int

    @Query("UPDATE location SET isPrimary = 0")
    suspend fun removePrimaryMark()

}