package com.example.a2

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "temperature_data")
data class TemperatureData(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val date: String,
    val maxTemperature: Double,
    val minTemperature: Double
)