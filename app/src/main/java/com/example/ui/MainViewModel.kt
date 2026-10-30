package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.SachinApplication
import com.example.data.model.ActivityLogEntry
import com.example.data.model.AutomationSettings
import com.example.data.model.LogType
import com.example.data.model.MatchResult
import com.example.data.model.RideDetails
import com.example.domain.AutomationManager
import com.example.domain.RideMatchingEngine
import com.example.domain.RideParser
import com.example.ui.components.PermissionUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepo = SachinApplication.instance.settingsRepository
    private val logRepo = SachinApplication.instance.activityLogRepository

    val settings: StateFlow<AutomationSettings> = settingsRepo.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AutomationSettings()
    )

    val logs: StateFlow<List<ActivityLogEntry>> = logRepo.recentLogs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val isAutomationRunning: StateFlow<Boolean> = AutomationManager.isAutomationRunning
    val isAccessibilityConnected: StateFlow<Boolean> = AutomationManager.isAccessibilityConnected
    val isOverlayActive: StateFlow<Boolean> = AutomationManager.isOverlayActive
    val statusMessage: StateFlow<String> = AutomationManager.statusMessage
    val lastEvaluatedRide: StateFlow<RideDetails?> = AutomationManager.lastEvaluatedRide
    val lastMatchResult: StateFlow<MatchResult?> = AutomationManager.lastMatchResult

    private val _hasAccessibilityPermission = MutableStateFlow(false)
    val hasAccessibilityPermission: StateFlow<Boolean> = _hasAccessibilityPermission.asStateFlow()

    private val _hasOverlayPermission = MutableStateFlow(false)
    val hasOverlayPermission: StateFlow<Boolean> = _hasOverlayPermission.asStateFlow()

    // Simulation state
    private val _simulatedInput = MutableStateFlow("Pickup: 2.1 km\nDrop: 4.5 km\nFare: ₹145\n[Accept Ride]")
    val simulatedInput: StateFlow<String> = _simulatedInput.asStateFlow()

    private val _simulatedRide = MutableStateFlow<RideDetails?>(null)
    val simulatedRide: StateFlow<RideDetails?> = _simulatedRide.asStateFlow()

    private val _simulatedResult = MutableStateFlow<MatchResult?>(null)
    val simulatedResult: StateFlow<MatchResult?> = _simulatedResult.asStateFlow()

    init {
        refreshPermissionStates(application)
        runSimulation(_simulatedInput.value)
    }

    fun refreshPermissionStates(context: Context) {
        _hasAccessibilityPermission.value = PermissionUtils.isAccessibilityServiceEnabled(context)
        _hasOverlayPermission.value = PermissionUtils.isOverlayPermissionGranted(context)
    }

    fun startAutomation(context: Context) {
        refreshPermissionStates(context)
        AutomationManager.startAutomation(context)
    }

    fun stopAutomation(context: Context) {
        AutomationManager.stopAutomation(context)
    }

    fun emergencyStop(context: Context) {
        AutomationManager.emergencyStop(context)
    }

    fun updateMaxPickup(km: Double) {
        viewModelScope.launch {
            settingsRepo.updateMaxPickupKm(km)
        }
    }

    fun updateMaxDrop(km: Double) {
        viewModelScope.launch {
            settingsRepo.updateMaxDropKm(km)
        }
    }

    fun updatePriceFilter(enabled: Boolean, minPrice: Double) {
        viewModelScope.launch {
            settingsRepo.updatePriceFilter(enabled, minPrice)
        }
    }

    fun updateAutoAccept(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.updateAutoAccept(enabled)
        }
    }

    fun updateFloatingControls(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.updateFloatingControls(enabled)
        }
    }

    fun updateFeedbackSettings(sound: Boolean, vibration: Boolean) {
        viewModelScope.launch {
            settingsRepo.updateFeedbackSettings(sound, vibration)
        }
    }

    fun resetSettings() {
        viewModelScope.launch {
            settingsRepo.resetToDefaults()
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            logRepo.clearLogs()
        }
    }

    // Simulation methods
    fun setSimulatedText(text: String) {
        _simulatedInput.value = text
        runSimulation(text)
    }

    fun runSimulation(text: String) {
        val ride = RideParser.parseFromText(text)
        _simulatedRide.value = ride
        val currentSettings = settings.value
        val result = RideMatchingEngine.evaluate(ride, currentSettings)
        _simulatedResult.value = result
    }

    fun simulateAcceptRide(context: Context) {
        val ride = _simulatedRide.value ?: return
        val result = _simulatedResult.value ?: return

        if (result.isMatch) {
            logRepo.log(
                eventType = LogType.ACCEPTED,
                message = "[SIMULATION] Ride accepted: Pickup ${ride.pickupKm}km, Drop ${ride.dropKm}km, Price ₹${ride.price ?: 0.0}",
                pickupKm = ride.pickupKm,
                dropKm = ride.dropKm,
                price = ride.price,
                isMatched = true
            )
            AutomationManager.triggerHaptic(context, 100)
        }
    }
}
