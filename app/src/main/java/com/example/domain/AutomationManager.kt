package com.example.domain

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import com.example.SachinApplication
import com.example.data.model.LogType
import com.example.data.model.MatchResult
import com.example.data.model.RideDetails
import com.example.service.AutomationForegroundService
import com.example.service.FloatingOverlayService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

object AutomationManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _isAutomationRunning = MutableStateFlow(false)
    val isAutomationRunning: StateFlow<Boolean> = _isAutomationRunning.asStateFlow()

    private val _isAccessibilityConnected = MutableStateFlow(false)
    val isAccessibilityConnected: StateFlow<Boolean> = _isAccessibilityConnected.asStateFlow()

    private val _isOverlayActive = MutableStateFlow(false)
    val isOverlayActive: StateFlow<Boolean> = _isOverlayActive.asStateFlow()

    private val _lastEvaluatedRide = MutableStateFlow<RideDetails?>(null)
    val lastEvaluatedRide: StateFlow<RideDetails?> = _lastEvaluatedRide.asStateFlow()

    private val _lastMatchResult = MutableStateFlow<MatchResult?>(null)
    val lastMatchResult: StateFlow<MatchResult?> = _lastMatchResult.asStateFlow()

    private val _statusMessage = MutableStateFlow("Automation is Stopped")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    // Recent ride ID cooldown cache: Map of rideId -> processed timestamp
    private val processedRides = ConcurrentHashMap<String, Long>()

    fun setAccessibilityConnected(connected: Boolean) {
        _isAccessibilityConnected.value = connected
        if (!connected && _isAutomationRunning.value) {
            _statusMessage.value = "Accessibility Service disconnected"
        }
    }

    fun setOverlayActive(active: Boolean) {
        _isOverlayActive.value = active
    }

    fun startAutomation(context: Context) {
        scope.launch {
            val settings = SachinApplication.instance.settingsRepository.settingsFlow.first()
            _isAutomationRunning.value = true
            _statusMessage.value = "Automation Active: Monitoring for rides"

            // Log event
            SachinApplication.instance.activityLogRepository.log(
                eventType = LogType.AUTOMATION_STARTED,
                message = "Automation started. Max Pickup: ${settings.maxPickupKm} km, Max Drop: ${settings.maxDropKm} km"
            )

            // Start Foreground Service
            val fgsIntent = Intent(context, AutomationForegroundService::class.java).apply {
                action = AutomationForegroundService.ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(fgsIntent)
            } else {
                context.startService(fgsIntent)
            }

            // Launch Floating Overlay if enabled and permission granted
            if (settings.floatingControlsEnabled && Settings.canDrawOverlays(context)) {
                val overlayIntent = Intent(context, FloatingOverlayService::class.java)
                context.startService(overlayIntent)
            }

            triggerHaptic(context, 80)
        }
    }

    fun stopAutomation(context: Context, reason: String = "User stopped automation") {
        _isAutomationRunning.value = false
        _statusMessage.value = "Automation Stopped"

        // Stop foreground service
        val fgsIntent = Intent(context, AutomationForegroundService::class.java).apply {
            action = AutomationForegroundService.ACTION_STOP
        }
        context.startService(fgsIntent)

        // Stop overlay service
        val overlayIntent = Intent(context, FloatingOverlayService::class.java)
        context.stopService(overlayIntent)
        _isOverlayActive.value = false

        // Log stop event
        SachinApplication.instance.activityLogRepository.log(
            eventType = LogType.AUTOMATION_STOPPED,
            message = reason
        )

        triggerHaptic(context, 100)
    }

    fun emergencyStop(context: Context) {
        _isAutomationRunning.value = false
        _statusMessage.value = "EMERGENCY STOP TRIGGERED"

        val fgsIntent = Intent(context, AutomationForegroundService::class.java).apply {
            action = AutomationForegroundService.ACTION_STOP
        }
        context.startService(fgsIntent)

        val overlayIntent = Intent(context, FloatingOverlayService::class.java)
        context.stopService(overlayIntent)
        _isOverlayActive.value = false

        SachinApplication.instance.activityLogRepository.log(
            eventType = LogType.EMERGENCY_STOP,
            message = "Emergency stop engaged by driver. All operations halted immediately."
        )

        // Strong vibration alert
        triggerEmergencyVibration(context)
    }

    fun recordRideEvaluation(ride: RideDetails, result: MatchResult) {
        _lastEvaluatedRide.value = ride
        _lastMatchResult.value = result
    }

    /**
     * Checks if this ride was already processed within the cooldown window to prevent duplicate clicks.
     */
    fun shouldProcessRide(rideId: String, cooldownSeconds: Int): Boolean {
        val now = System.currentTimeMillis()
        val cooldownMillis = cooldownSeconds * 1000L

        // Clean up expired entries
        val iterator = processedRides.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (now - entry.value > cooldownMillis * 2) {
                iterator.remove()
            }
        }

        val lastProcessed = processedRides[rideId]
        if (lastProcessed != null && (now - lastProcessed) < cooldownMillis) {
            return false // In cooldown
        }

        processedRides[rideId] = now
        return true
    }

    fun triggerHaptic(context: Context, durationMs: Long) {
        try {
            val vibrator = getVibrator(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }

    fun triggerEmergencyVibration(context: Context) {
        try {
            val vibrator = getVibrator(context)
            val timings = longArrayOf(0, 150, 80, 150, 80, 250)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(timings, -1)
            }
        } catch (_: Exception) {}
    }

    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
}
