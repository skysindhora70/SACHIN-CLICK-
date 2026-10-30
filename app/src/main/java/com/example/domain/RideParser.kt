package com.example.domain

import android.view.accessibility.AccessibilityNodeInfo
import com.example.data.model.RideDetails
import java.util.Locale
import java.util.regex.Pattern

object RideParser {

    private val DISTANCE_KM_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:km|kms|kilometer|kilometres)", Pattern.CASE_INSENSITIVE)
    private val DISTANCE_METER_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:m|meter|meters|metre|metres)\\b", Pattern.CASE_INSENSITIVE)
    private val PRICE_PATTERN = Pattern.compile("(?:₹|rs\\.?|inr)\\s*(\\d+(?:\\.\\d+)?)", Pattern.CASE_INSENSITIVE)

    private val ACCEPT_BUTTON_KEYWORDS = listOf(
        "accept",
        "accept ride",
        "accept order",
        "accept booking",
        "take ride",
        "confirm ride",
        "book ride"
    )

    private val PICKUP_KEYWORDS = listOf(
        "pickup", "pick up", "pick-up", "from", "start", "pickup distance", "pickup location"
    )

    private val DROP_KEYWORDS = listOf(
        "drop", "drop off", "drop-off", "dropoff", "to", "destination", "drop distance", "drop location"
    )

    /**
     * Parses an AccessibilityNodeInfo root hierarchy into a RideDetails object.
     * Collects visible text, analyzes structural proximity or labeled rows for Pickup and Drop,
     * extracts price, and discovers any candidate Accept button node.
     */
    fun parseAccessibilityNode(rootNode: AccessibilityNodeInfo?): RideDetails? {
        if (rootNode == null) return null

        val nodeTexts = mutableListOf<String>()
        var acceptButton: AccessibilityNodeInfo? = null

        // Traverse hierarchy
        traverseNode(rootNode) { node ->
            val text = node.text?.toString()?.trim()
            val desc = node.contentDescription?.toString()?.trim()

            if (!text.isNullOrEmpty()) nodeTexts.add(text)
            if (!desc.isNullOrEmpty() && desc != text) nodeTexts.add(desc)

            // Detect accept button
            if (acceptButton == null) {
                val candidateText = (text ?: desc)?.lowercase(Locale.ROOT)
                val viewId = node.viewIdResourceName?.lowercase(Locale.ROOT) ?: ""

                val matchesText = candidateText != null && ACCEPT_BUTTON_KEYWORDS.any { candidateText.contains(it) }
                val matchesId = viewId.contains("accept") || viewId.contains("btn_accept")

                if ((matchesText || matchesId) && (node.isClickable || node.parent?.isClickable == true)) {
                    // Find actual clickable node (self or ancestor)
                    acceptButton = if (node.isClickable) node else findClickableParent(node)
                }
            }
        }

        if (nodeTexts.isEmpty()) return null

        val combinedText = nodeTexts.joinToString("\n")
        val parsedRide = parseFromText(combinedText)

        return parsedRide.copy(acceptButtonNode = acceptButton)
    }

    /**
     * Parses structured text into a RideDetails object.
     * Identifies Pickup and Drop using explicit context labels instead of blind position.
     */
    fun parseFromText(text: String): RideDetails {
        val lines = text.split("\n").map { it.trim() }.filter { it.isNotEmpty() }

        var pickupKm: Double? = null
        var dropKm: Double? = null
        var price: Double? = null

        // First pass: scan line by line with context awareness
        for (i in lines.indices) {
            val line = lines[i]
            val lowerLine = line.lowercase(Locale.ROOT)

            // Check if this line is a pickup label or contains pickup + distance
            if (PICKUP_KEYWORDS.any { lowerLine.contains(it) }) {
                val distInLine = extractDistanceInKm(line)
                if (distInLine != null) {
                    pickupKm = distInLine
                } else if (i + 1 < lines.size) {
                    // Distance might be in the immediate next line
                    val nextDist = extractDistanceInKm(lines[i + 1])
                    if (nextDist != null) pickupKm = nextDist
                }
            }

            // Check if this line is a drop label or contains drop + distance
            if (DROP_KEYWORDS.any { lowerLine.contains(it) }) {
                val distInLine = extractDistanceInKm(line)
                if (distInLine != null) {
                    dropKm = distInLine
                } else if (i + 1 < lines.size) {
                    // Distance might be in the immediate next line
                    val nextDist = extractDistanceInKm(lines[i + 1])
                    if (nextDist != null) dropKm = nextDist
                }
            }

            // Check for price
            if (price == null) {
                val priceMatch = PRICE_PATTERN.matcher(line)
                if (priceMatch.find()) {
                    price = priceMatch.group(1)?.toDoubleOrNull()
                }
            }
        }

        // Second pass: if labeled parsing missed one, look for combined formatted strings like:
        // "Pickup: 2.4 km | Drop: 4.8 km" or "From: 1.2km To: 6.5km"
        if (pickupKm == null || dropKm == null) {
            val pickupRegex = Pattern.compile("(?:pickup|from|pick up)[^\\d]*?(\\d+(?:\\.\\d+)?)\\s*(?:km|m)", Pattern.CASE_INSENSITIVE)
            val dropRegex = Pattern.compile("(?:drop|to|destination)[^\\d]*?(\\d+(?:\\.\\d+)?)\\s*(?:km|m)", Pattern.CASE_INSENSITIVE)

            val pMatch = pickupRegex.matcher(text)
            if (pickupKm == null && pMatch.find()) {
                val matchedChunk = pMatch.group(0) ?: ""
                pickupKm = extractDistanceInKm(matchedChunk)
            }

            val dMatch = dropRegex.matcher(text)
            if (dropKm == null && dMatch.find()) {
                val matchedChunk = dMatch.group(0) ?: ""
                dropKm = extractDistanceInKm(matchedChunk)
            }
        }

        // Generate a deterministic ride identifier based on parsed values
        val id = "RIDE_${pickupKm ?: 0.0}_${dropKm ?: 0.0}_${price ?: 0.0}_${text.hashCode()}"

        return RideDetails(
            id = id,
            pickupKm = pickupKm,
            dropKm = dropKm,
            price = price,
            rawText = text
        )
    }

    /**
     * Extracts distance from a string segment, converting meters to KM if necessary.
     */
    fun extractDistanceInKm(text: String): Double? {
        val kmMatcher = DISTANCE_KM_PATTERN.matcher(text)
        if (kmMatcher.find()) {
            return kmMatcher.group(1)?.toDoubleOrNull()
        }

        val meterMatcher = DISTANCE_METER_PATTERN.matcher(text)
        if (meterMatcher.find()) {
            val meters = meterMatcher.group(1)?.toDoubleOrNull()
            if (meters != null) {
                return meters / 1000.0
            }
        }

        return null
    }

    private fun traverseNode(node: AccessibilityNodeInfo, action: (AccessibilityNodeInfo) -> Unit) {
        action(node)
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            traverseNode(child, action)
        }
    }

    private fun findClickableParent(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = node.parent
        while (current != null) {
            if (current.isClickable) return current
            current = current.parent
        }
        return null
    }
}
