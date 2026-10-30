package com.example.data.log

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.ActivityLogEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {
    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 100): Flow<List<ActivityLogEntry>>

    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<ActivityLogEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: ActivityLogEntry): Long

    @Query("DELETE FROM activity_logs")
    suspend fun clearAll()

    @Query("DELETE FROM activity_logs WHERE timestamp < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)
}
