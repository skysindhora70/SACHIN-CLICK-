package com.example.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.SachinApplication
import com.example.data.model.LogType
import com.example.domain.AutomationManager
import com.example.domain.RideMatchingEngine
import com.example.domain.RideParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SachinAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var lastScanTimeMs = 0L

    companion object {
        private const val TAG = "SachinAccessibility"
        private const val MIN_SCAN_INTERVAL_MS = 250L // Debounce rapid accessibility spam
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "Sachin Clicker Accessibility Service Connected")
        AutomationManager.setAccessibilityConnected(true)
        SachinApplication.instance.activityLogRepository.log(
            eventType = LogType.INFO,
            message = "Accessibility service connected and ready"
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // 1. Strict guard: Observe visible UI changes only while automation is enabled
        if (!AutomationManager.isAutomationRunning.value) {
            return
        }

        // 2. Filter relevant event types only
        val eventType = event.eventType
        if (eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) {
            return
        }

        // 3. Debounce to prevent high CPU / battery drain
        val now = System.currentTimeMillis()
        if (now - lastScanTimeMs < MIN_SCAN_INTERVAL_MS) {
            return
        }
        lastScanTimeMs = now

        serviceScope.launch {
            processActiveWindow()
        }
    }

    private suspend fun processActiveWindow() {
        val rootNode: AccessibilityNodeInfo = rootInActiveWindow ?: return

        try {
            // Parse screen structure and detect ride request
            val ride = RideParser.parseAccessibilityNode(rootNode) ?: return

            // Requirement 6: Ignore incomplete ride requests
            if (!ride.isComplete) {
                return
            }

            val settings = SachinApplication.instance.settingsRepository.settingsFlow.first()

            // Requirement 7: Prevent duplicate acceptance attempts within cooldown
            if (!AutomationManager.shouldProcessRide(ride.id, settings.cooldownSeconds)) {
                return
            }

            // Evaluate against matching rules
            val matchResult = RideMatchingEngine.evaluate(ride, settings)
            AutomationManager.recordRideEvaluation(ride, matchResult)

            if (matchResult.isMatch) {
                SachinApplication.instance.activityLogRepository.log(
                    eventType = LogType.MATCHED,
                    message = "Ride Matched! " + matchResult.reasons.joinToString(", "),
                    pickupKm = ride.pickupKm,
                    dropKm = ride.dropKm,
                    price = ride.price,
                    isMatched = true
                )

                // Requirement 7: Only perform click if auto-accept enabled and button clearly identified
                if (settings.autoAcceptEnabled) {
                    val acceptButton = ride.acceptButtonNode
                    if (acceptButton != null) {
                        val clicked = acceptButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        if (clicked) {
                            SachinApplication.instance.activityLogRepository.log(
                                eventType = LogType.ACCEPTED,
                                message = "Successfully clicked Accept for Pickup: ${ride.pickupKm}km, Drop: ${ride.dropKm}km",
                                pickupKm = ride.pickupKm,
                                dropKm = ride.dropKm,
                                price = ride.price,
                                isMatched = true
                            )
                            AutomationManager.triggerHaptic(this@SachinAccessibilityService, 120)
                        } else {
                            SachinApplication.instance.activityLogRepository.log(
                                eventType = LogType.ERROR,
                                message = "Accept button node was found but ACTION_CLICK could not be performed.",
                                pickupKm = ride.pickupKm,
                                dropKm = ride.dropKm,
                                price = ride.price,
                                isMatched = true
                            )
                        }
                    } else {
                        // Interface cannot be identified reliably, do not click!
                        SachinApplication.instance.activityLogRepository.log(
                            eventType = LogType.INFO,
                            message = "Ride matched criteria, but no safe Accept button node was found. Skipping click.",
                            pickupKm = ride.pickupKm,
                            dropKm = ride.dropKm,
                            price = ride.price,
                            isMatched = true
                        )
                    }
                }
            } else {
                // Rejected
                SachinApplication.instance.activityLogRepository.log(
                    eventType = LogType.REJECTED,
                    message = "Ride Rejected: " + matchResult.reasons.joinToString(", "),
                    pickupKm = ride.pickupKm,
                    dropKm = ride.dropKm,
                    price = ride.price,
                    isMatched = false
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in processActiveWindow", e)
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "Sachin Clicker Accessibility Service Interrupted")
        AutomationManager.setAccessibilityConnected(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        AutomationManager.setAccessibilityConnected(false)
        serviceScope.cancel()
    }
}
