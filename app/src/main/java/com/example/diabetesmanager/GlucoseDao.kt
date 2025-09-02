package com.example.diabetesmanager

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface GlucoseDao {
    @Insert
    suspend fun insertReading(reading: GlucoseReadingEntity): Long

    @Update
    suspend fun updateReading(reading: GlucoseReadingEntity)

    @Delete
    suspend fun deleteReading(reading: GlucoseReadingEntity)

    @Query("SELECT * FROM glucose_readings WHERE date = :date ORDER BY timestamp ASC")
    suspend fun getReadingsForDate(date: String): List<GlucoseReadingEntity>

    @Query("SELECT * FROM glucose_readings ORDER BY timestamp DESC")
    suspend fun getAllReadings(): List<GlucoseReadingEntity>

    @Query("SELECT * FROM glucose_readings ORDER BY timestamp DESC")
    fun getAllReadingsLive(): LiveData<List<GlucoseReadingEntity>>

    @Query("SELECT * FROM glucose_readings ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentReadings(limit: Int = 10): List<GlucoseReadingEntity>

    @Query("SELECT * FROM glucose_readings WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC, timestamp ASC")
    suspend fun getReadingsInRange(startDate: String, endDate: String): List<GlucoseReadingEntity>

    @Query("SELECT AVG(glucose_level) FROM glucose_readings WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getAverageGlucose(startDate: String, endDate: String): Float?

    @Query("SELECT COUNT(*) FROM glucose_readings")
    suspend fun getTotalReadingsCount(): Int

    @Query("SELECT COUNT(*) FROM glucose_readings WHERE glucose_level BETWEEN :minNormal AND :maxNormal")
    suspend fun getNormalReadingsCount(minNormal: Float = 70f, maxNormal: Float = 130f): Int

    @Query("DELETE FROM glucose_readings")
    suspend fun deleteAllReadings()
}
