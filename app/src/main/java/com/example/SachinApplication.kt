package com.example

import android.app.Application
import com.example.data.log.ActivityLogRepository
import com.example.data.preferences.SettingsRepository

class SachinApplication : Application() {

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var activityLogRepository: ActivityLogRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        settingsRepository = SettingsRepository(this)
        activityLogRepository = ActivityLogRepository(this)
    }

    companion object {
        lateinit var instance: SachinApplication
            private set
    }
}
