package com.example.data.model

data class AutomationSettings(
    val maxPickupKm: Double = 3.0,
    val maxDropKm: Double = 5.0,
    val priceFilterEnabled: Boolean = false,
    val minPrice: Double = 50.0,
    val autoAcceptEnabled: Boolean = true,
    val floatingControlsEnabled: Boolean = true,
    val soundFeedbackEnabled: Boolean = true,
    val vibrationFeedbackEnabled: Boolean = true,
    val cooldownSeconds: Int = 3
)
