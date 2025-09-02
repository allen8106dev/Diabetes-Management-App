package com.example.diabetesmanager

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.*

@Entity(tableName = "glucose_readings")
data class GlucoseReadingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "date")
    val date: String,

    @ColumnInfo(name = "time_slot")
    val timeSlot: String,

    @ColumnInfo(name = "glucose_level")
    val glucoseLevel: Float,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "created_at")
    val createdAt: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
) {
    fun toGlucoseReading(): GlucoseReading {
        return GlucoseReading(
            id = this.id,
            date = this.date,
            timeSlot = this.timeSlot,
            glucoseLevel = this.glucoseLevel,
            timestamp = this.timestamp
        )
    }
}
