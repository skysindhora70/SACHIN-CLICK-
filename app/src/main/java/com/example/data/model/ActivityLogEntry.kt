package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activity_logs")
data class ActivityLogEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: String,
    val message: String,
    val pickupKm: Double? = null,
    val dropKm: Double? = null,
    val price: Double? = null,
    val isMatched: Boolean? = null
)

object LogType {
    const val INFO = "INFO"
    const val DETECTED = "DETECTED"
    const val MATCHED = "MATCHED"
    const val REJECTED = "REJECTED"
    const val ACCEPTED = "ACCEPTED"
    const val AUTOMATION_STARTED = "AUTOMATION_STARTED"
    const val AUTOMATION_STOPPED = "AUTOMATION_STOPPED"
    const val EMERGENCY_STOP = "EMERGENCY_STOP"
    const val ERROR = "ERROR"
}
