package com.example.data.log

import android.content.Context
import com.example.data.model.ActivityLogEntry
import com.example.data.model.LogType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ActivityLogRepository(context: Context) {
    private val logDao = AppDatabase.getInstance(context).logDao()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val recentLogs: Flow<List<ActivityLogEntry>> = logDao.getRecentLogs(limit = 100)

    fun log(
        eventType: String,
        message: String,
        pickupKm: Double? = null,
        dropKm: Double? = null,
        price: Double? = null,
        isMatched: Boolean? = null
    ) {
        scope.launch {
            logDao.insert(
                ActivityLogEntry(
                    eventType = eventType,
                    message = message,
                    pickupKm = pickupKm,
                    dropKm = dropKm,
                    price = price,
                    isMatched = isMatched
                )
            )
        }
    }

    suspend fun clearLogs() {
        logDao.clearAll()
    }
}
