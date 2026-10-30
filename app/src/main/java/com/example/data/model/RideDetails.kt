package com.example.data.model

import android.view.accessibility.AccessibilityNodeInfo

data class RideDetails(
    val id: String,
    val pickupKm: Double?,
    val dropKm: Double?,
    val price: Double?,
    val rawText: String,
    val acceptButtonNode: AccessibilityNodeInfo? = null
) {
    val isComplete: Boolean
        get() = pickupKm != null && dropKm != null
}

data class MatchResult(
    val isMatch: Boolean,
    val pickupMatched: Boolean,
    val dropMatched: Boolean,
    val priceMatched: Boolean,
    val reasons: List<String>
)
