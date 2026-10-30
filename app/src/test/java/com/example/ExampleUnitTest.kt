package com.example

import com.example.data.model.AutomationSettings
import com.example.data.model.RideDetails
import com.example.domain.RideMatchingEngine
import com.example.domain.RideParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun rideMatchingEngine_bothDistancesWithinLimits_isEligible() {
        val settings = AutomationSettings(
            maxPickupKm = 3.0,
            maxDropKm = 5.0,
            priceFilterEnabled = false
        )
        val ride = RideDetails(
            id = "ride_1",
            pickupKm = 2.5,
            dropKm = 4.8,
            price = 120.0,
            rawText = "Pickup: 2.5 km, Drop: 4.8 km"
        )

        val result = RideMatchingEngine.evaluate(ride, settings)

        assertTrue("Ride should match when pickup <= max and drop <= max", result.isMatch)
        assertTrue(result.pickupMatched)
        assertTrue(result.dropMatched)
    }

    @Test
    fun rideMatchingEngine_pickupExceedsLimit_isRejected() {
        val settings = AutomationSettings(
            maxPickupKm = 3.0,
            maxDropKm = 5.0,
            priceFilterEnabled = false
        )
        val ride = RideDetails(
            id = "ride_2",
            pickupKm = 3.8, // Exceeds 3.0
            dropKm = 4.0,
            price = 150.0,
            rawText = "Pickup: 3.8 km, Drop: 4.0 km"
        )

        val result = RideMatchingEngine.evaluate(ride, settings)

        assertFalse("Ride should be rejected if pickup exceeds maxPickupKm", result.isMatch)
        assertFalse(result.pickupMatched)
        assertTrue(result.dropMatched)
    }

    @Test
    fun rideMatchingEngine_dropExceedsLimit_isRejected() {
        val settings = AutomationSettings(
            maxPickupKm = 3.0,
            maxDropKm = 5.0,
            priceFilterEnabled = false
        )
        val ride = RideDetails(
            id = "ride_3",
            pickupKm = 1.2,
            dropKm = 7.5, // Exceeds 5.0
            price = 200.0,
            rawText = "Pickup: 1.2 km, Drop: 7.5 km"
        )

        val result = RideMatchingEngine.evaluate(ride, settings)

        assertFalse("Ride should be rejected if drop exceeds maxDropKm", result.isMatch)
        assertTrue(result.pickupMatched)
        assertFalse(result.dropMatched)
    }

    @Test
    fun rideMatchingEngine_priceFilter_enforcesMinimumPrice() {
        val settings = AutomationSettings(
            maxPickupKm = 3.0,
            maxDropKm = 5.0,
            priceFilterEnabled = true,
            minPrice = 100.0
        )

        val cheapRide = RideDetails(
            id = "ride_low",
            pickupKm = 2.0,
            dropKm = 3.0,
            price = 45.0, // Below 100.0
            rawText = "Pickup: 2 km, Drop: 3 km, ₹45"
        )
        val cheapResult = RideMatchingEngine.evaluate(cheapRide, settings)
        assertFalse("Low price ride should be rejected when price filter is on", cheapResult.isMatch)

        val goodRide = RideDetails(
            id = "ride_good",
            pickupKm = 2.0,
            dropKm = 3.0,
            price = 120.0, // >= 100.0
            rawText = "Pickup: 2 km, Drop: 3 km, ₹120"
        )
        val goodResult = RideMatchingEngine.evaluate(goodRide, settings)
        assertTrue("Eligible price ride should be accepted", goodResult.isMatch)
    }

    @Test
    fun rideParser_extractsPickupAndDropDistances() {
        val text = "Pickup Location\nDistance: 2.4 km\nDrop Location\nDistance: 4.8 km\nFare: ₹160\n[Accept]"
        val parsed = RideParser.parseFromText(text)

        assertEquals(2.4, parsed.pickupKm ?: 0.0, 0.01)
        assertEquals(4.8, parsed.dropKm ?: 0.0, 0.01)
        assertEquals(160.0, parsed.price ?: 0.0, 0.01)
        assertTrue(parsed.isComplete)
    }

    @Test
    fun rideParser_convertsMetersToKm() {
        val text = "From: 650 m\nTo: 3.5 km"
        val parsed = RideParser.parseFromText(text)

        assertEquals(0.65, parsed.pickupKm ?: 0.0, 0.01)
        assertEquals(3.5, parsed.dropKm ?: 0.0, 0.01)
    }
}
