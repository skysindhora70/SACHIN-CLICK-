package com.example.domain

import com.example.data.model.AutomationSettings
import com.example.data.model.MatchResult
import com.example.data.model.RideDetails

object RideMatchingEngine {

    /**
     * Evaluates a detected ride against the user's distance and price settings.
     *
     * Matching rules:
     * 1. actualPickupKm <= maxPickupKm AND actualDropKm <= maxDropKm (both must be true).
     * 2. If priceFilterEnabled is true: actualPrice != null && actualPrice >= minPrice.
     *    If priceFilterEnabled is false: distance matching alone determines eligibility.
     */
    fun evaluate(ride: RideDetails, settings: AutomationSettings): MatchResult {
        val reasons = mutableListOf<String>()

        // Validate complete ride data
        if (ride.pickupKm == null || ride.dropKm == null) {
            reasons.add("Incomplete distance data (Pickup: ${ride.pickupKm ?: "missing"}, Drop: ${ride.dropKm ?: "missing"})")
            return MatchResult(
                isMatch = false,
                pickupMatched = false,
                dropMatched = false,
                priceMatched = false,
                reasons = reasons
            )
        }

        val pickupMatched = ride.pickupKm <= settings.maxPickupKm
        val dropMatched = ride.dropKm <= settings.maxDropKm

        if (pickupMatched) {
            reasons.add("Pickup ${"%.2f".format(ride.pickupKm)} km <= Max ${settings.maxPickupKm} km (OK)")
        } else {
            reasons.add("Pickup ${"%.2f".format(ride.pickupKm)} km exceeds Max ${settings.maxPickupKm} km (Too far)")
        }

        if (dropMatched) {
            reasons.add("Drop ${"%.2f".format(ride.dropKm)} km <= Max ${settings.maxDropKm} km (OK)")
        } else {
            reasons.add("Drop ${"%.2f".format(ride.dropKm)} km exceeds Max ${settings.maxDropKm} km (Too far)")
        }

        val priceMatched = if (settings.priceFilterEnabled) {
            if (ride.price == null) {
                reasons.add("Price filter enabled but price is not visible on screen")
                false
            } else if (ride.price >= settings.minPrice) {
                reasons.add("Price ₹${"%.1f".format(ride.price)} >= Min ₹${settings.minPrice} (OK)")
                true
            } else {
                reasons.add("Price ₹${"%.1f".format(ride.price)} is below Min ₹${settings.minPrice} (Low fare)")
                false
            }
        } else {
            reasons.add("Price filter disabled (Distance only)")
            true
        }

        val isEligible = pickupMatched && dropMatched && priceMatched

        return MatchResult(
            isMatch = isEligible,
            pickupMatched = pickupMatched,
            dropMatched = dropMatched,
            priceMatched = priceMatched,
            reasons = reasons
        )
    }
}
