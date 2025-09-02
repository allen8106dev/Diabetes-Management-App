import androidx.room.*
import java.text.SimpleDateFormat
import java.util.*

@Entity(tableName = "glucose_readings")
data class GlucoseReading(
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

) {
    companion object {
        fun createToday(timeSlot: String, glucoseLevel: Float): GlucoseReading {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val today = dateFormat.format(Date())
            return GlucoseReading(
                date = today,
                timeSlot = timeSlot,
                glucoseLevel = glucoseLevel
            )
        }
    }
}
