package com.example.a2

// TemperatureDataDao.kt
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TemperatureDataDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemperatureData(temperatureData: TemperatureData)

    @Query("SELECT * FROM temperature_data WHERE date = :date")
    suspend fun getTemperatureDataByDate(date: String): TemperatureData?

    @Query("SELECT AVG(maxTemperature) FROM temperature_data WHERE date LIKE :lastTenYears")
    suspend fun getAverageMaxTemperatureForLastTenYears(lastTenYears: String): Double

    @Query("SELECT AVG(minTemperature) FROM temperature_data WHERE date LIKE :lastTenYears")
    suspend fun getAverageMinTemperatureForLastTenYears(lastTenYears: String): Double
}