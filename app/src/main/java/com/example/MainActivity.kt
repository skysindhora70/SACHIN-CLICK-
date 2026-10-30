package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.screens.ActivityLogScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DistanceSettingsScreen
import com.example.ui.screens.SimulationScreen
import com.example.ui.theme.DangerRed
import com.example.ui.theme.SachinClickerTheme

enum class Screen {
    DASHBOARD,
    SETTINGS,
    SIMULATOR,
    LOGS
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SachinClickerTheme {
                val context = LocalContext.current
                val lifecycleOwner = LocalLifecycleOwner.current

                // Refresh permissions whenever app comes into foreground
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            viewModel.refreshPermissionStates(context)
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                // Request notification permission for Android 13+
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { _ -> }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }

                // BackHandler for sub-screens
                if (currentScreen != Screen.DASHBOARD) {
                    BackHandler {
                        currentScreen = Screen.DASHBOARD
                    }
                }

                val settings by viewModel.settings.collectAsStateWithLifecycle()
                val isRunning by viewModel.isAutomationRunning.collectAsStateWithLifecycle()
                val isAccessibilityConnected by viewModel.isAccessibilityConnected.collectAsStateWithLifecycle()
                val hasAccessibilityPermission by viewModel.hasAccessibilityPermission.collectAsStateWithLifecycle()
                val hasOverlayPermission by viewModel.hasOverlayPermission.collectAsStateWithLifecycle()
                val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
                val lastRide by viewModel.lastEvaluatedRide.collectAsStateWithLifecycle()
                val lastMatch by viewModel.lastMatchResult.collectAsStateWithLifecycle()
                val logs by viewModel.logs.collectAsStateWithLifecycle()
                val simulatedInput by viewModel.simulatedInput.collectAsStateWithLifecycle()
                val simulatedRide by viewModel.simulatedRide.collectAsStateWithLifecycle()
                val simulatedResult by viewModel.simulatedResult.collectAsStateWithLifecycle()

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = when (currentScreen) {
                                        Screen.DASHBOARD -> "Sachin Clicker"
                                        Screen.SETTINGS -> "Distance & Filter Settings"
                                        Screen.SIMULATOR -> "Rule Simulator"
                                        Screen.LOGS -> "Activity Log"
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            actions = {
                                if (isRunning) {
                                    IconButton(
                                        onClick = { viewModel.emergencyStop(context) },
                                        modifier = Modifier.testTag("appbar_emergency_stop")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = "Emergency Stop",
                                            tint = DangerRed
                                        )
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background,
                                titleContentColor = MaterialTheme.colorScheme.onBackground
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp
                        ) {
                            NavigationBarItem(
                                selected = currentScreen == Screen.DASHBOARD,
                                onClick = { currentScreen = Screen.DASHBOARD },
                                icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                                label = { Text("Dashboard") },
                                modifier = Modifier.testTag("nav_dashboard")
                            )
                            NavigationBarItem(
                                selected = currentScreen == Screen.SETTINGS,
                                onClick = { currentScreen = Screen.SETTINGS },
                                icon = { Icon(Icons.Default.Tune, contentDescription = "Settings") },
                                label = { Text("Settings") },
                                modifier = Modifier.testTag("nav_settings")
                            )
                            NavigationBarItem(
                                selected = currentScreen == Screen.SIMULATOR,
                                onClick = { currentScreen = Screen.SIMULATOR },
                                icon = { Icon(Icons.Default.Science, contentDescription = "Simulator") },
                                label = { Text("Simulator") },
                                modifier = Modifier.testTag("nav_simulator")
                            )
                            NavigationBarItem(
                                selected = currentScreen == Screen.LOGS,
                                onClick = { currentScreen = Screen.LOGS },
                                icon = { Icon(Icons.Default.History, contentDescription = "Logs") },
                                label = { Text("Logs") },
                                modifier = Modifier.testTag("nav_logs")
                            )
                        }
                    }
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        when (currentScreen) {
                            Screen.DASHBOARD -> DashboardScreen(
                                context = context,
                                settings = settings,
                                isRunning = isRunning,
                                isAccessibilityConnected = isAccessibilityConnected,
                                hasAccessibilityPermission = hasAccessibilityPermission,
                                hasOverlayPermission = hasOverlayPermission,
                                statusMessage = statusMessage,
                                lastRide = lastRide,
                                lastMatch = lastMatch,
                                onStartAutomation = { viewModel.startAutomation(context) },
                                onStopAutomation = { viewModel.stopAutomation(context) },
                                onEmergencyStop = { viewModel.emergencyStop(context) },
                                onNavigateToSettings = { currentScreen = Screen.SETTINGS },
                                onNavigateToSimulator = { currentScreen = Screen.SIMULATOR }
                            )
                            Screen.SETTINGS -> DistanceSettingsScreen(
                                settings = settings,
                                onUpdatePickup = { viewModel.updateMaxPickup(it) },
                                onUpdateDrop = { viewModel.updateMaxDrop(it) },
                                onUpdatePriceFilter = { enabled, price -> viewModel.updatePriceFilter(enabled, price) },
                                onUpdateAutoAccept = { viewModel.updateAutoAccept(it) },
                                onUpdateFloatingControls = { viewModel.updateFloatingControls(it) },
                                onUpdateFeedback = { sound, vib -> viewModel.updateFeedbackSettings(sound, vib) },
                                onResetSettings = { viewModel.resetSettings() }
                            )
                            Screen.SIMULATOR -> SimulationScreen(
                                context = context,
                                settings = settings,
                                inputText = simulatedInput,
                                simulatedRide = simulatedRide,
                                simulatedResult = simulatedResult,
                                onInputTextChange = { viewModel.setSimulatedText(it) },
                                onSimulateAccept = { viewModel.simulateAcceptRide(context) }
                            )
                            Screen.LOGS -> ActivityLogScreen(
                                logs = logs,
                                onClearLogs = { viewModel.clearLogs() }
                            )
                        }
                    }
                }
            }
        }
    }
}
