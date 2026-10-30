package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AutomationSettings
import com.example.data.model.MatchResult
import com.example.data.model.RideDetails
import com.example.ui.theme.DangerRed
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.SkyPrimary

@Composable
fun SimulationScreen(
    context: Context,
    settings: AutomationSettings,
    inputText: String,
    simulatedRide: RideDetails?,
    simulatedResult: MatchResult?,
    onInputTextChange: (String) -> Unit,
    onSimulateAccept: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val testScenarios = listOf(
        "Eligible Ride" to "Pickup: 1.8 km\nDrop: 3.5 km\nFare: ₹120\n[Accept Ride]",
        "Far Pickup (>3km)" to "Pickup: 4.8 km\nDrop: 3.0 km\nFare: ₹160\n[Accept Ride]",
        "Far Drop (>5km)" to "Pickup: 1.5 km\nDrop: 9.2 km\nFare: ₹280\n[Accept Ride]",
        "Meters Format" to "From: 650 m\nTo: 4.2 km\nPrice: ₹85\n[Accept Ride]",
        "Low Fare" to "Pickup: 1.2 km\nDrop: 2.5 km\nFare: ₹35\n[Accept Ride]",
        "Missing Drop" to "Pickup: 2.0 km\nSearching drop location...\n[Accept]"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Science,
                contentDescription = null,
                tint = SkyPrimary,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Ride Matching Playground",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Verify parser & matching rules without third-party app",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Quick Preset Scenarios Row
        Text(
            text = "Preset Test Scenarios:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            testScenarios.forEach { (label, text) ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onInputTextChange(text) }
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // Test Input Box
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Ride Request Screen Text",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { onInputTextChange(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .testTag("simulation_input_text"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Active Rules: Max Pickup <= ${settings.maxPickupKm} km | Max Drop <= ${settings.maxDropKm} km | Price: ${if (settings.priceFilterEnabled) ">= ₹${settings.minPrice}" else "Off"}",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Parsed & Evaluated Result Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Evaluation Result",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    val isMatch = simulatedResult?.isMatch == true
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isMatch) EmeraldSuccess else DangerRed
                    ) {
                        Text(
                            text = if (isMatch) "ELIGIBLE - MATCHED" else "INELIGIBLE - REJECTED",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Breakdown Items
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Pickup Breakdown
                    ResultRow(
                        label = "Pickup Distance",
                        value = if (simulatedRide?.pickupKm != null) "${simulatedRide.pickupKm} km" else "Not Detected",
                        rule = "<= ${settings.maxPickupKm} km",
                        passed = simulatedResult?.pickupMatched == true
                    )

                    // Drop Breakdown
                    ResultRow(
                        label = "Drop Distance",
                        value = if (simulatedRide?.dropKm != null) "${simulatedRide.dropKm} km" else "Not Detected",
                        rule = "<= ${settings.maxDropKm} km",
                        passed = simulatedResult?.dropMatched == true
                    )

                    // Price Breakdown
                    if (settings.priceFilterEnabled) {
                        ResultRow(
                            label = "Price / Fare",
                            value = if (simulatedRide?.price != null) "₹${simulatedRide.price}" else "Not Visible",
                            rule = ">= ₹${settings.minPrice}",
                            passed = simulatedResult?.priceMatched == true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Matching Engine Details:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                simulatedResult?.reasons?.forEach { reason ->
                    Text(
                        text = "• $reason",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (simulatedResult?.isMatch == true) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onSimulateAccept,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldSuccess,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("simulate_accept_button")
                    ) {
                        Icon(imageVector = Icons.Default.TouchApp, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Simulate Accept Action & Log", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun ResultRow(
    label: String,
    value: String,
    rule: String,
    passed: Boolean
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = rule,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = if (passed) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = null,
                    tint = if (passed) EmeraldSuccess else DangerRed,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
