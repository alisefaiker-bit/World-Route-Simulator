package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.SavedRoute
import com.example.data.models.RouteHistory
import com.example.data.models.UserAccount
import com.example.data.models.Waypoint
import com.example.simulation.EarthDataEngine
import com.example.simulation.LocationHub
import com.example.simulation.SimulationEngine
import com.example.simulation.SimPathNode
import com.example.simulation.SimulationTelemetry
import com.example.simulation.WeatherState
import androidx.compose.foundation.lazy.rememberLazyListState
import java.text.DecimalFormat
import java.util.UUID

// Global visual color variables matching Sophisticated Dark vibe
val ColorBackground = Color(0xFF0A0B0D)
val ColorSurfaceDark = Color(0xFF1A1C1E)
val ColorSurfaceLight = Color(0xFF272A30)
val ColorAccentNeonCyan = Color(0xFF818CF8) // Indigo 400
val ColorAccentNeonGreen = Color(0xFF34D399) // Emerald 400
val ColorAccentNeonAmber = Color(0xFFFBBF24) // Amber 400
val ColorAccentNeonPink = Color(0xFFFB7185) // Rose 400

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    val telemetry by viewModel.activeTelemetry.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val computedPath by viewModel.computedPath.collectAsState()
    val ways by viewModel.simulationWaypoints.collectAsState()
    val savedRoutes by viewModel.savedRoutes.collectAsState()
    val historyList by viewModel.routeHistory.collectAsState()
    val accountInfo by viewModel.userAccount.collectAsState()
    val speedWarp by viewModel.speedWarp.collectAsState()

    // Screen Layout states
    var selectedTab by remember { mutableStateOf("SIMULATION") } // "SIMULATION", "PLANNER", "AI_GUIDE", "HISTORY", "CLOUD_SYNC"
    var activeCameraMode by remember { mutableStateOf("INTERIOR") } // "INTERIOR", "EXTERIOR", "REAR", "AERIAL", "FREE", "CINEMATOGRAPHIC"
    var activeMapMode by remember { mutableStateOf("STANDARD") } // "STANDARD", "SATELLITE", "HYBRID", "RELIEF"

    // UI Dialogs
    var showSaveRouteDialog by remember { mutableStateOf(false) }
    var saveRouteNameInput by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }
    var importStringInput by remember { mutableStateOf("") }
    var showExportDialog by remember { mutableStateOf(false) }

    // Navigation and screen split dimensions
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("main_scaffold"),
        containerColor = ColorBackground
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Left menu navigation sidebar (Tablet / Wide layout) or bottom navigation on small screens
            if (isTablet) {
                NavigationRailComponent(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    accountInfo = accountInfo
                )
            }

            // Main simulator body
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // Secondary Header Section
                TopAppBeltHeader(
                    telemetry = telemetry,
                    isRunning = isRunning,
                    onStart = { viewModel.applyPlannedRouteToSimulation() },
                    onPause = { SimulationEngine.pauseSimulation() },
                    onStop = { SimulationEngine.stopSimulation() },
                    onRestart = { viewModel.captureScreengrab() },
                    speedWarp = speedWarp,
                    onWarpChange = { viewModel.speedWarp.value = it }
                )

                // Split Layout for wide screens or tabs on mobile
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    // Simulation workspace (Interactive Map Canvas + pseudothree-dimensional cockpit)
                    Column(
                        modifier = Modifier
                            .weight(1.5f)
                            .fillMaxHeight()
                    ) {
                        // Cockpit Perspective simulator (Top part)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(Color.Black)
                                .border(1.dp, ColorSurfaceLight.copy(alpha = 0.5f))
                        ) {
                            Immersive3DCockpit(
                                telemetry = telemetry,
                                cameraMode = activeCameraMode,
                                isRunning = isRunning,
                                pathFraction = if (computedPath.isEmpty()) 0.0 else (computedPath.indexOfFirst { it.latitude == telemetry.currentLatitude && it.longitude == telemetry.currentLongitude }.coerceAtLeast(0).toDouble() / computedPath.size)
                            )

                            // Quick overlay for camera filters
                            CameraSelectionPill(
                                activeMode = activeCameraMode,
                                onSelect = { activeCameraMode = it }
                            )

                            // Weather status overlay
                            WeatherOverlayHUD(weather = telemetry.weather)
                        }

                        // World Map HUD canvas (Bottom part)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(ColorBackground)
                        ) {
                            InteractiveWorldMap(
                                telemetry = telemetry,
                                computedPath = computedPath,
                                mapMode = activeMapMode,
                                hubs = EarthDataEngine.hubs,
                                waypoints = ways,
                                onLocationDoubleClicked = { hub ->
                                    viewModel.addWaypointToPlanner(hub)
                                }
                            )

                            // Quick overlay for Map Layer styling
                            MapLayerSelectionPill(
                                activeMode = activeMapMode,
                                onSelect = { activeMapMode = it }
                            )

                            // Compass indicator
                            TelemetryCompassOverlay(telemetry = telemetry)
                        }
                    }

                    // Mobile sliding controls drawer or inline settings panel
                    if (isTablet || selectedTab != "SIMULATION") {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .border(1.dp, ColorSurfaceLight.copy(alpha = 0.5f))
                                .background(ColorSurfaceDark),
                            color = ColorSurfaceDark
                        ) {
                            when (selectedTab) {
                                "PLANNER" -> PlannerPanel(
                                    viewModel = viewModel,
                                    ways = ways,
                                    onSaveAsked = {
                                        saveRouteNameInput = ""
                                        showSaveRouteDialog = true
                                    },
                                    onImportAsked = { showImportDialog = true },
                                    onExportAsked = {
                                        viewModel.exportRouteShareCode()
                                        showExportDialog = true
                                    }
                                )
                                "AI_GUIDE" -> AITravelChatPanel(viewModel = viewModel)
                                "HISTORY" -> HistoryAndFavoritesPanel(
                                    viewModel = viewModel,
                                    historyList = historyList,
                                    savedRoutes = savedRoutes
                                )
                                "CLOUD_SYNC" -> CloudSyncAccountPanel(
                                    viewModel = viewModel,
                                    accountInfo = accountInfo
                                )
                                else -> TelemetryHUDDetailsPanel(telemetry = telemetry, viewModel = viewModel)
                            }
                        }
                    }
                }

                // Small screen bottom navigation bar
                if (!isTablet) {
                    MobileBottomBarComponent(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it }
                    )
                }
            }
        }

        // --- Dialog overlays ---

        // Save Route Dialog
        if (showSaveRouteDialog) {
            AlertDialog(
                onDismissRequest = { showSaveRouteDialog = false },
                containerColor = ColorSurfaceLight,
                title = { Text("Guardar Ruta en Memoria", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("Introduce un nombre identificativo para esta ruta simulada:", color = Color.Gray, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = saveRouteNameInput,
                            onValueChange = { saveRouteNameInput = it },
                            label = { Text("Nombre de la ruta") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = ColorAccentNeonCyan,
                                unfocusedBorderColor = Color.Gray
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.savePlannedRoute(saveRouteNameInput)
                            showSaveRouteDialog = false
                        }
                    ) {
                        Text("Guardar", color = ColorAccentNeonCyan)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSaveRouteDialog = false }) {
                        Text("Cancelar", color = Color.White)
                    }
                }
            )
        }

        // Export Dialog
        if (showExportDialog) {
            val shareCode by viewModel.routeShareString.collectAsState()
            AlertDialog(
                onDismissRequest = { showExportDialog = false },
                containerColor = ColorSurfaceLight,
                title = { Text("Código de Exportación", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("Copia el siguiente token cifrado para clonar esta ruta en otro dispositivo:", color = Color.Gray, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ColorBackground)
                                .padding(12.dp)
                                .border(1.dp, ColorSurfaceLight)
                        ) {
                            Text(
                                text = shareCode,
                                color = ColorAccentNeonGreen,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 5,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showExportDialog = false }) {
                        Text("Entendido", color = ColorAccentNeonCyan)
                    }
                }
            )
        }

        // Import Dialog
        if (showImportDialog) {
            AlertDialog(
                onDismissRequest = { showImportDialog = false },
                containerColor = ColorSurfaceLight,
                title = { Text("Importar Token de Ruta", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("Pega el código de importación (comienza con WRSIM_v1|):", color = Color.Gray, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = importStringInput,
                            onValueChange = { importStringInput = it },
                            label = { Text("Token cifrado") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = ColorAccentNeonCyan,
                                unfocusedBorderColor = Color.Gray
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val success = viewModel.importRouteShareCode(importStringInput)
                            if (success) {
                                showImportDialog = false
                            }
                        }
                    ) {
                        Text("Cargar Ruta", color = ColorAccentNeonCyan)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showImportDialog = false }) {
                        Text("Cancelar", color = Color.White)
                    }
                }
            )
        }

        // Capture/Timelapse overlay indicator
        val recordingState by viewModel.screenRecordingState.collectAsState()
        if (recordingState != "Idle") {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ColorSurfaceLight),
                    border = BorderStroke(1.dp, ColorAccentNeonCyan)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = ColorAccentNeonCyan)
                        Spacer(modifier = Modifier.height(16.dp))
                        val statusText = when (recordingState) {
                            "Capturing" -> "CAPTURANDO FOTOGRAMA HUD DE LA SIMULACIÓN..."
                            "Captured_JPEG" -> "CAPTURA GUARDADA EN COCKPIT_DCIM SUCCESS!"
                            "GeneratingTimelapse" -> "PROCESANDO TIMELAPSE DE COCKPIT EN MOCK_MP4..."
                            "ProcessingFrames" -> "CODIFICANDO IMÁGENES A 60 FPS..."
                            "Finished_Timelapse" -> "TIMELAPSE EXPORTADO CON ÉXITO EN VIDEO_OUT!"
                            else -> "PROCESANDO..."
                        }
                        Text(statusText, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

// Tablet navigation rail component
@Composable
fun NavigationRailComponent(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    accountInfo: UserAccount?
) {
    NavigationRail(
        containerColor = ColorBackground,
        contentColor = Color.LightGray
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(16.dp))
                Icon(
                    imageVector = Icons.Default.Explore,
                    contentDescription = "Logo",
                    tint = ColorAccentNeonCyan,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                NavigationRailItem(
                    selected = selectedTab == "SIMULATION",
                    onClick = { onTabSelected("SIMULATION") },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Simulador") },
                    label = { Text("Simulator", fontSize = 10.sp) },
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = ColorAccentNeonCyan,
                        indicatorColor = ColorSurfaceLight
                    )
                )

                NavigationRailItem(
                    selected = selectedTab == "PLANNER",
                    onClick = { onTabSelected("PLANNER") },
                    icon = { Icon(Icons.Default.AltRoute, contentDescription = "Planificador") },
                    label = { Text("Rutas", fontSize = 10.sp) },
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = ColorAccentNeonCyan,
                        indicatorColor = ColorSurfaceLight
                    )
                )

                NavigationRailItem(
                    selected = selectedTab == "AI_GUIDE",
                    onClick = { onTabSelected("AI_GUIDE") },
                    icon = { Icon(Icons.Default.Language, contentDescription = "IA Guía") },
                    label = { Text("Asistente IA", fontSize = 10.sp) },
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = ColorAccentNeonCyan,
                        indicatorColor = ColorSurfaceLight
                    )
                )

                NavigationRailItem(
                    selected = selectedTab == "HISTORY",
                    onClick = { onTabSelected("HISTORY") },
                    icon = { Icon(Icons.Default.History, contentDescription = "Historial") },
                    label = { Text("Fichas", fontSize = 10.sp) },
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = ColorAccentNeonCyan,
                        indicatorColor = ColorSurfaceLight
                    )
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                NavigationRailItem(
                    selected = selectedTab == "CLOUD_SYNC",
                    onClick = { onTabSelected("CLOUD_SYNC") },
                    icon = { 
                        Icon(
                            imageVector = if (accountInfo != null) Icons.Default.CloudQueue else Icons.Default.CloudOff, 
                            contentDescription = "Cloud Setup"
                        ) 
                    },
                    label = { Text(if (accountInfo != null) "Sinc" else "Login", fontSize = 10.sp) },
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = ColorAccentNeonCyan,
                        indicatorColor = ColorSurfaceLight
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// Bottom navigation for phones
@Composable
fun MobileBottomBarComponent(
    selectedTab: String,
    onTabSelected: (String) -> Unit
) {
    NavigationBar(
        containerColor = ColorBackground,
        modifier = Modifier.navigationBarsPadding()
    ) {
        NavigationBarItem(
            selected = selectedTab == "SIMULATION",
            onClick = { onTabSelected("SIMULATION") },
            icon = { Icon(Icons.Default.Dashboard, "Simulator") },
            label = { Text("Simulador", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = ColorAccentNeonCyan,
                unselectedIconColor = Color.Gray,
                indicatorColor = ColorSurfaceLight
            )
        )
        NavigationBarItem(
            selected = selectedTab == "PLANNER",
            onClick = { onTabSelected("PLANNER") },
            icon = { Icon(Icons.Default.AltRoute, "Planner") },
            label = { Text("Planear", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = ColorAccentNeonCyan,
                unselectedIconColor = Color.Gray,
                indicatorColor = ColorSurfaceLight
            )
        )
        NavigationBarItem(
            selected = selectedTab == "AI_GUIDE",
            onClick = { onTabSelected("AI_GUIDE") },
            icon = { Icon(Icons.Default.Language, "AI Guide") },
            label = { Text("Guía IA", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = ColorAccentNeonCyan,
                unselectedIconColor = Color.Gray,
                indicatorColor = ColorSurfaceLight
            )
        )
        NavigationBarItem(
            selected = selectedTab == "HISTORY",
            onClick = { onTabSelected("HISTORY") },
            icon = { Icon(Icons.Default.History, "History") },
            label = { Text("Log", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = ColorAccentNeonCyan,
                unselectedIconColor = Color.Gray,
                indicatorColor = ColorSurfaceLight
            )
        )
        NavigationBarItem(
            selected = selectedTab == "CLOUD_SYNC",
            onClick = { onTabSelected("CLOUD_SYNC") },
            icon = { Icon(Icons.Default.AccountCircle, "Profile") },
            label = { Text("Sinc", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = ColorAccentNeonCyan,
                unselectedIconColor = Color.Gray,
                indicatorColor = ColorSurfaceLight
            )
        )
    }
}

// Top simulation controller header
@Composable
fun TopAppBeltHeader(
    telemetry: SimulationTelemetry,
    isRunning: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onRestart: () -> Unit,
    speedWarp: Int,
    onWarpChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = ColorSurfaceDark),
        border = BorderStroke(1.dp, ColorSurfaceLight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "WORLD ROUTE SIMULATOR",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = ColorAccentNeonCyan,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "GPS: ${String.format("%.5f", telemetry.currentLatitude)}, ${String.format("%.5f", telemetry.currentLongitude)}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = Color.White
                )
            }

            // Central control buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onStop,
                    modifier = Modifier
                        .size(36.dp)
                        .background(ColorSurfaceLight, RoundedCornerShape(8.dp)),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color.Red)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Detener", modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))

                if (isRunning) {
                    IconButton(
                        onClick = onPause,
                        modifier = Modifier
                            .size(36.dp)
                            .background(ColorSurfaceLight, RoundedCornerShape(8.dp)),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = ColorAccentNeonAmber)
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = "Pausar", modifier = Modifier.size(20.dp))
                    }
                } else {
                    IconButton(
                        onClick = onStart,
                        modifier = Modifier
                            .size(36.dp)
                            .background(ColorSurfaceLight, RoundedCornerShape(8.dp)),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = ColorAccentNeonGreen)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Iniciar", modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onRestart,
                    modifier = Modifier
                        .size(36.dp)
                        .background(ColorSurfaceLight, RoundedCornerShape(8.dp)),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = "Capturar", modifier = Modifier.size(20.dp))
                }
            }

            // Warp Selector
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("WARP:", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.width(4.dp))
                listOf(1, 10, 100, 1000).forEach { warp ->
                    val isSelected = speedWarp == warp
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isSelected) ColorAccentNeonCyan else ColorSurfaceLight)
                            .clickable { onWarpChange(warp) }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "x$warp",
                            color = if (isSelected) Color.Black else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

// Interactivo custom vector world map rendering utilizing standard sinusoids or simple continents points
@Composable
fun InteractiveWorldMap(
    telemetry: SimulationTelemetry,
    computedPath: List<SimPathNode>,
    mapMode: String,
    hubs: List<LocationHub>,
    waypoints: List<Waypoint>,
    onLocationDoubleClicked: (LocationHub) -> Unit
) {
    var scale by remember { mutableStateOf(1.0f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1.0f, 15.0f)
                    offset = Offset(
                        x = offset.x * zoom + pan.x,
                        y = offset.y * zoom + pan.y
                    )
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // 1. Draw oceans dynamic shading
            val oceanBrush = when (mapMode) {
                "SATELLITE", "HYBRID" -> Brush.radialGradient(
                    colors = listOf(Color(0xFF0A122C), Color(0xFF05060B)),
                    center = Offset(width / 2, height / 2),
                    radius = width
                )
                "RELIEF" -> Brush.linearGradient(
                    colors = listOf(Color(0xFF1E1E2F), Color(0xFF0A0B0D))
                )
                else -> SolidColor(Color(0xFF07080B)) // Standard sophisticated dark neutral ocean
            }
            drawRect(brush = oceanBrush)

            // Draw coordinate grids
            val gridSize = 30.0
            (1..12).forEach { i ->
                val x = (width / 12) * i
                drawLine(
                    color = ColorAccentNeonCyan.copy(alpha = 0.08f),
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 1f
                )
            }
            (1..6).forEach { i ->
                val y = (height / 6) * i
                drawLine(
                    color = ColorAccentNeonCyan.copy(alpha = 0.08f),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f
                )
            }

            // Transform Coordinates helpers
            fun geoToCanvas(lat: Double, lng: Double): Offset {
                // Mercator / Equirectangular Projection wrapping around scale & panning offsets
                val xNorm = (lng + 180.0) / 360.0
                val yNorm = (90.0 - lat) / 180.0
                
                val xCanvas = (xNorm * width * scale) + offset.x
                val yCanvas = (yNorm * height * scale) + offset.y
                return Offset(xCanvas.toFloat(), yCanvas.toFloat())
            }

            // 2. Draw Vector stylized landmass shapes
            val landBrush = when (mapMode) {
                "SATELLITE", "HYBRID" -> SolidColor(Color(0xFF1E293B))
                "RELIEF" -> SolidColor(Color(0xFF162535))
                else -> SolidColor(Color(0xFF171D26)) // Sophisticated Slate Landmass
            }

            // Simplified polygons representing continents for highly responsive, standalone visual mapping
            drawSimplifiedContinent(this, landBrush, ::geoToCanvas)

            // 3. Render routing lines
            if (computedPath.isNotEmpty()) {
                val strokeW = when (mapMode) {
                    "HYBRID" -> 6f
                    else -> 4f
                }
                
                // Draw glowing track path
                var prevOffset: Offset? = null
                computedPath.forEach { node ->
                    val currentOffset = geoToCanvas(node.latitude, node.longitude)
                    if (prevOffset != null) {
                        drawLine(
                            color = when (node.transportType) {
                                "Avión" -> ColorAccentNeonCyan
                                "Tren" -> ColorAccentNeonAmber
                                "Coche", "Moto" -> ColorAccentNeonGreen
                                else -> ColorAccentNeonPink
                            },
                            start = prevOffset!!,
                            end = currentOffset,
                            strokeWidth = strokeW,
                            cap = StrokeCap.Round
                        )
                    }
                    prevOffset = currentOffset
                }
            }

            // 4. Render Waypoint Pins
            waypoints.forEachIndexed { idx, point ->
                val pinOffset = geoToCanvas(point.latitude, point.longitude)
                drawCircle(
                    color = if (idx == 0) ColorAccentNeonGreen else if (idx == waypoints.size - 1) Color.Red else ColorAccentNeonAmber,
                    radius = 8f * scale.coerceAtMost(2f),
                    center = pinOffset
                )
                // Draw outer glowing ring
                drawCircle(
                    color = Color.White.copy(alpha = 0.5f),
                    radius = 14f * scale.coerceAtMost(2f),
                    center = pinOffset,
                    style = Stroke(2f)
                )
            }

            // 5. Render cities hubs
            hubs.forEach { hub ->
                val hubOffset = geoToCanvas(hub.latitude, hub.longitude)
                
                // Check if map is zoomed in to draw small glowing dot and label
                drawCircle(
                    color = when (hub.type) {
                        "Monumento" -> ColorAccentNeonPink
                        "Aeropuerto" -> ColorAccentNeonCyan
                        "Estación" -> ColorAccentNeonAmber
                        else -> Color.White
                    }.copy(alpha = 0.8f),
                    radius = 5f * scale.coerceAtMost(2.5f),
                    center = hubOffset
                )
            }

            // 6. Draw simulating active vehicle outline
            val vehicleOffset = geoToCanvas(telemetry.currentLatitude, telemetry.currentLongitude)
            drawCircle(
                color = Color.White,
                radius = 12f * scale.coerceAtMost(2.5f),
                center = vehicleOffset
            )
            drawCircle(
                color = ColorAccentNeonCyan,
                radius = 24f * scale.coerceAtMost(2.5f),
                center = vehicleOffset,
                style = Stroke(3f)
            )
        }

        // Tap HUD information regarding current city coordinates
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
                .background(ColorSurfaceLight.copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                .border(1.dp, ColorSurfaceLight)
                .padding(8.dp)
        ) {
            Text(
                text = "${telemetry.currentCity}, ${telemetry.currentCountry} (${telemetry.currentTimezone})",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        // Search overlay to quickly jump to landmarks
        IconButton(
            onClick = {
                scale = 1.0f
                offset = Offset.Zero
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .background(ColorSurfaceLight, RoundedCornerShape(8.dp))
        ) {
            Icon(Icons.Default.YoutubeSearchedFor, "Recenter Earth Map", tint = Color.LightGray)
        }
    }
}

private fun drawSimplifiedContinent(
    drawScope: DrawScope,
    brush: Brush,
    geoToCanvas: (Double, Double) -> Offset
) {
    // Coordinate nodes mapping simplified outlines for North America, South America, Eurasia, Africa, Australia, Greenland
    val continents = listOf(
        // America del Norte
        listOf(
            Pair(72.0, -125.0), Pair(70.0, -70.0), Pair(50.0, -55.0), Pair(25.0, -80.0),
            Pair(15.0, -95.0), Pair(25.0, -115.0), Pair(50.0, -125.0), Pair(65.0, -165.0)
        ),
        // America del Sur
        listOf(
            Pair(12.0, -72.0), Pair(5.0, -50.0), Pair(-8.0, -35.0), Pair(-23.0, -43.0),
            Pair(-55.0, -68.0), Pair(-42.0, -74.0), Pair(-15.0, -75.0), Pair(-5.0, -80.0)
        ),
        // Eurasia
        listOf(
            Pair(70.0, 10.0), Pair(75.0, 60.0), Pair(72.0, 130.0), Pair(60.0, 170.0),
            Pair(35.0, 140.0), Pair(22.0, 115.0), Pair(10.0, 105.0), Pair(5.0, 95.0),
            Pair(15.0, 75.0), Pair(25.0, 60.0), Pair(12.0, 45.0), Pair(30.0, 32.0),
            Pair(36.0, 26.0), Pair(41.0, 15.0), Pair(50.0, 10.0), Pair(60.0, 5.0)
        ),
        // África
        listOf(
            Pair(35.0, -5.0), Pair(30.0, 32.0), Pair(10.0, 43.0), Pair(2.0, 45.0),
            Pair(-20.0, 35.0), Pair(-34.0, 18.0), Pair(-15.0, 12.0), Pair(5.0, 8.0),
            Pair(5.0, -10.0), Pair(15.0, -15.0), Pair(20.0, -17.0)
        ),
        // Australia
        listOf(
            Pair(-12.0, 130.0), Pair(-15.0, 142.0), Pair(-28.0, 153.0), Pair(-37.0, 145.0),
            Pair(-35.0, 115.0), Pair(-22.0, 113.0)
        ),
        // Groenlandia
        listOf(
            Pair(83.0, -40.0), Pair(70.0, -20.0), Pair(60.0, -45.0), Pair(75.0, -60.0)
        )
    )

    continents.forEach { shape ->
        val path = Path()
        shape.forEachIndexed { idx, pair ->
            val canvasOffset = geoToCanvas(pair.first, pair.second)
            if (idx == 0) {
                path.moveTo(canvasOffset.x, canvasOffset.y)
            } else {
                path.lineTo(canvasOffset.x, canvasOffset.y)
            }
        }
        path.close()
        drawScope.drawPath(path, brush)
    }
}

// Immersive pseudothree-dimensional viewpoint dashboard simulator
@Composable
fun Immersive3DCockpit(
    telemetry: SimulationTelemetry,
    cameraMode: String,
    isRunning: Boolean,
    pathFraction: Double
) {
    val infiniteTransition = rememberInfiniteTransition(label = "dynamic_grid")
    
    // Scrolling lines offset
    val terrainScroll by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 400f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isRunning) (4000 / telemetry.currentSpeedKmh.coerceAtLeast(1.0).coerceAtMost(300.0)).toInt() else 1000000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "terrain_scroll"
    )

    // Weather lightning flashes
    val lightningFlash by infiniteRepeatableFlash(isActive = telemetry.weather.condition == "Tormenta Eléctrica")

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                if (lightningFlash > 0.6f) {
                    drawRect(Color.White.copy(alpha = 0.4f * (lightningFlash - 0.6f) / 0.4f))
                }
            }
    ) {
        val width = size.width
        val height = size.height
        val horizonY = height * 0.45f

        // 1. Sky Gradient representation mapping sunset, dawn, night, or midday atmospheric colors
        val skyBrush = when {
            telemetry.weather.condition == "Tormenta Eléctrica" || telemetry.weather.condition == "Niebla Densa" -> Brush.verticalGradient(
                colors = listOf(Color(0xFF1E202B), Color(0xFF0E0F14))
            )
            telemetry.currentAltitude > 8000 -> Brush.verticalGradient(
                colors = listOf(Color(0xFF02040A), Color(0xFF0A0D1A)) // Ultra high altitude deep space
            )
            else -> Brush.verticalGradient(
                colors = listOf(Color(0xFF0B0D19), Color(0xFF1B1E30)) // Sophisticated Twilight Horizon
            )
        }
        drawRect(brush = skyBrush, size = Size(width, horizonY))

        // Draw Sun or Moon based on timezone values
        val celestialX = (width * 0.35f) + (pathFraction * width * 0.4f).toFloat()
        drawCircle(
            color = ColorAccentNeonAmber.copy(alpha = 0.82f),
            radius = 35f,
            center = Offset(celestialX, horizonY * 0.4f)
        )

        // Draw floating dynamic clouds
        drawCircle(Color.Gray.copy(alpha = 0.30f), 50f, Offset(width * 0.15f, horizonY * 0.6f))
        drawCircle(Color.Gray.copy(alpha = 0.30f), 40f, Offset(width * 0.18f, horizonY * 0.58f))
        drawCircle(Color.Gray.copy(alpha = 0.30f), 60f, Offset(width * 0.65f, horizonY * 0.5f))

        // 2. Mountains terrain drawing rising based on telemetry altitude peaks
        val mountainPath = Path().apply {
            moveTo(0f, horizonY)
            lineTo(width * 0.2f, horizonY - 120f - (telemetry.currentAltitude / 150f).toFloat())
            lineTo(width * 0.4f, horizonY)
            lineTo(width * 0.6f, horizonY - 80f - (telemetry.currentAltitude / 220f).toFloat())
            lineTo(width * 0.8f, horizonY - 160f - (telemetry.currentAltitude / 100f).toFloat())
            lineTo(width, horizonY)
            close()
        }
        drawPath(
            mountainPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF1B263B), Color(0xFF0D1B2A))
            )
        )

        // 3. Ground roadway perspective representation
        val groundBrush = Brush.verticalGradient(
            colors = listOf(Color(0xFF121420), Color(0xFF07080D))
        )
        drawRect(brush = groundBrush, size = Size(width, height - horizonY), topLeft = Offset(0f, horizonY))

        // Draw ground grid / road receding perspective lines
        val vanishingPoint = Offset(width / 2, horizonY)
        val lineCount = 8
        for (i in 0..lineCount) {
            val ratio = i.toDouble() / lineCount
            val bottomX = width * ratio
            drawLine(
                color = ColorAccentNeonCyan.copy(alpha = 0.15f),
                start = vanishingPoint,
                end = Offset(bottomX.toFloat(), height),
                strokeWidth = 2f
            )
        }

        // receded terrain landscape speed markers
        val horizontalLineY = horizonY + (terrainScroll % 180f)
        val scaleWeight = (horizontalLineY - horizonY) / (height - horizonY)
        drawLine(
            color = ColorAccentNeonCyan.copy(alpha = 0.08f),
            start = Offset(0f, horizontalLineY),
            end = Offset(width, horizontalLineY),
            strokeWidth = 1f + (scaleWeight * 4f)
        )

        // Receding Central Road/Track dashes
        if (telemetry.activeTransport != "Avión" && telemetry.activeTransport != "Barco") {
            val centralDashPath = Path().apply {
                val topW = 4f
                val botW = 24f
                val centerTopX = width / 2
                val centerBotX = width / 2
                moveTo(centerTopX - topW, horizonY)
                lineTo(centerTopX + topW, horizonY)
                lineTo(centerBotX + botW, height)
                lineTo(centerBotX - botW, height)
                close()
            }
            drawPath(centralDashPath, brush = SolidColor(Color.DarkGray.copy(alpha = 0.4f)))

            // Receding center dashboard lines
            val dashY = horizonY + (terrainScroll % 120f)
            val dashH = 20f * (dashY - horizonY) / (height - horizonY)
            drawRect(
                color = ColorAccentNeonAmber,
                topLeft = Offset(width / 2 - 5f, dashY),
                size = Size(10f, dashH)
            )
            
            // Draw roadside lights/barrier trees moving sideways
            val sideMoveX = (terrainScroll * 1.5f)
            drawCircle(ColorAccentNeonGreen.copy(alpha = 0.22f), 12f, Offset(width * 0.15f - sideMoveX, horizonY + sideMoveX))
            drawCircle(ColorAccentNeonPink.copy(alpha = 0.22f), 12f, Offset(width * 0.85f + sideMoveX, horizonY + sideMoveX))
        }

        // 4. Weather state active visual animations (falling particles over screen)
        if (telemetry.weather.condition == "Lluvia Fuerte" || telemetry.weather.condition == "Tormenta Eléctrica") {
            // angled rain drops
            for (r in 0..45) {
                val rx = (r * 25) % width
                val ry = (r * 18 + terrainScroll * 2.5f) % height
                drawLine(
                    color = Color.Cyan.copy(alpha = 0.4f),
                    start = Offset(rx, ry),
                    end = Offset(rx - 5f, ry + 15f),
                    strokeWidth = 2f
                )
            }
        } else if (telemetry.weather.condition == "Nevada Ligera") {
            // soft drifting snowflakes
            for (s in 0..30) {
                val sx = (s * 35) % width
                val sy = (s * 22 + terrainScroll * 0.8f) % height
                drawCircle(
                    color = Color.White.copy(alpha = 0.6f),
                    radius = 4f,
                    center = Offset(sx, sy)
                )
            }
        } else if (telemetry.weather.condition == "Niebla Densa") {
            // misty gradient overlays
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.18f), Color.Transparent)
                )
            )
        }

        // 5. Active Camera Overlay filtering representing dashboard instrumentation!
        when (cameraMode) {
            "INTERIOR" -> {
                // Draw steering wheel outline & dashboard windshield bezel
                drawRect(
                    color = Color.Black.copy(alpha = 0.05f),
                    size = Size(width, height)
                )
                // Dashboard gauge bar
                drawRect(
                    color = ColorSurfaceDark,
                    topLeft = Offset(0f, height * 0.85f),
                    size = Size(width, height * 0.15f)
                )
                drawLine(
                    color = ColorAccentNeonCyan,
                    start = Offset(0f, height * 0.85f),
                    end = Offset(width, height * 0.85f),
                    strokeWidth = 4f
                )
                // Steering Column vector
                drawCircle(
                    color = Color.DarkGray,
                    radius = 70f,
                    center = Offset(width * 0.25f, height * 0.95f),
                    style = Stroke(12f)
                )
                // Rearview HUD Mirror representing visual rear state!
                val mirrorW = width * 0.35f
                val mirrorH = 50f
                val mirrorLeft = width * 0.325f
                drawRect(
                    color = ColorSurfaceDark,
                    topLeft = Offset(mirrorLeft, 10f),
                    size = Size(mirrorW, mirrorH),
                    style = Stroke(3f)
                )
                drawRect(
                    color = Color(0xFF0F1E29),
                    topLeft = Offset(mirrorLeft + 3f, 13f),
                    size = Size(mirrorW - 6f, mirrorH - 6f)
                )
                // Render micro receding grid inside rear mirror
                drawLine(Color.Red.copy(alpha = 0.4f), Offset(mirrorLeft + mirrorW / 2, 13f), Offset(mirrorLeft + mirrorW / 2, 13f + mirrorH - 6f), strokeWidth = 2f)
            }
            "EXTERIOR" -> {
                // Over the shoulder view tracking the vehicle model
                val vehBrush = Brush.linearGradient(
                    colors = listOf(ColorAccentNeonCyan, ColorAccentNeonPink)
                )
                // Draw dynamic futuristic vehicle wireframe
                val vehPath = Path().apply {
                    moveTo(width / 2 - 30f, height * 0.82f)
                    lineTo(width / 2 + 30f, height * 0.82f)
                    lineTo(width / 2 + 45f, height * 0.92f)
                    lineTo(width / 2 - 45f, height * 0.92f)
                    close()
                }
                drawPath(vehPath, brush = vehBrush)
                // Tail lights
                drawCircle(Color.Red, 8f, Offset(width / 2 - 35f, height * 0.88f))
                drawCircle(Color.Red, 8f, Offset(width / 2 + 35f, height * 0.88f))
            }
            "REAR" -> {
                // Looking backward over the dashboard
                drawRect(
                    color = ColorSurfaceDark,
                    topLeft = Offset(0f, height * 0.90f),
                    size = Size(width, height * 0.10f)
                )
                // Receding yellow dashed lane lines
                drawLine(
                    color = ColorAccentNeonAmber,
                    start = Offset(width / 2, horizonY),
                    end = Offset(width * 0.1f, height),
                    strokeWidth = 3f
                )
                drawLine(
                    color = ColorAccentNeonAmber,
                    start = Offset(width / 2, horizonY),
                    end = Offset(width * 0.9f, height),
                    strokeWidth = 3f
                )
            }
            "AERIAL" -> {
                // Full top-down HUD targeting circle
                drawCircle(
                    color = ColorAccentNeonGreen.copy(alpha = 0.15f),
                    radius = 200f,
                    center = Offset(width / 2, height / 2),
                    style = Stroke(2f)
                )
                drawLine(ColorAccentNeonGreen.copy(alpha = 0.2f), Offset(width / 2, 0f), Offset(width / 2, height), strokeWidth = 1f)
                drawLine(ColorAccentNeonGreen.copy(alpha = 0.2f), Offset(0f, height / 2), Offset(width, height / 2), strokeWidth = 1f)
            }
            else -> {
                // Free Cinematic panning outlines
                drawRect(
                    color = Color.Black.copy(alpha = 0.35f),
                    size = Size(width, height * 0.10f)
                )
                drawRect(
                    color = Color.Black.copy(alpha = 0.35f),
                    topLeft = Offset(0f, height * 0.90f),
                    size = Size(width, height * 0.10f)
                )
            }
        }
    }
}

// Infinite flash repeatable timer for storms
@Composable
fun infiniteRepeatableFlash(isActive: Boolean): State<Float> {
    if (!isActive) return remember { mutableStateOf(0f) }
    val transition = rememberInfiniteTransition(label = "lightning_transition")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "flash_float"
    )
}

// Camera control selection overlay pill
@Composable
fun CameraSelectionPill(
    activeMode: String,
    onSelect: (String) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = listOf("INTERIOR", "EXTERIOR", "REAR", "AERIAL", "FREE", "CINEMATOGRAPHIC").indexOf(activeMode),
        containerColor = ColorUtils.overlayAlphaBack(),
        contentColor = Color.White,
        edgePadding = 4.dp,
        indicator = {},
        divider = {},
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clip(RoundedCornerShape(8.dp))
    ) {
        listOf("INTERIOR", "EXTERIOR", "REAR", "AERIAL", "FREE", "CINEMATOGRAPHIC").forEach { mode ->
            val isSelected = activeMode == mode
            Tab(
                selected = isSelected,
                onClick = { onSelect(mode) },
                text = {
                    Text(
                        text = mode,
                        color = if (isSelected) ColorAccentNeonCyan else Color.LightGray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                },
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

// Map layer styler overlay selection pill
@Composable
fun MapLayerSelectionPill(
    activeMode: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .padding(12.dp)
            .background(ColorUtils.overlayAlphaBack(), RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        listOf("STANDARD", "SATELLITE", "HYBRID", "RELIEF").forEach { mode ->
            val isSelected = activeMode == mode
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) ColorAccentNeonCyan else Color.Transparent)
                    .clickable { onSelect(mode) }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = mode,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.Black else Color.White,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

// Weather overlay displays
@Composable
fun WeatherOverlayHUD(weather: WeatherState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 64.dp, end = 12.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = ColorUtils.overlayAlphaBack()),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val weatherIcon = when (weather.condition) {
                        "Solar", "Soleado" -> Icons.Default.WbSunny
                        "Lluvia Fuerte" -> Icons.Default.Thunderstorm
                        "Nevada Ligera" -> Icons.Default.AcUnit
                        "Niebla Densa" -> Icons.Default.Waves
                        else -> Icons.Default.Cloud
                    }
                    Icon(
                        imageVector = weatherIcon,
                        contentDescription = "Estado Clima",
                        tint = ColorAccentNeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = weather.condition.uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "TEMP: ${weather.temperatureCelsius.toInt()}°C  HUM: ${weather.humidityPercentage}%",
                    fontSize = 9.sp,
                    color = Color.LightGray,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "WIND: ${weather.windSpeedKmh.toInt()} km/h DIR: ${weather.windDirectionDegrees}°",
                    fontSize = 9.sp,
                    color = Color.LightGray,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

// Dynamic Geographic compass overlay
@Composable
fun TelemetryCompassOverlay(telemetry: SimulationTelemetry) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        contentAlignment = Alignment.TopStart
    ) {
        Canvas(modifier = Modifier.size(45.dp)) {
            // Draw compass glowing background ring
            drawCircle(ColorAccentNeonCyan.copy(alpha = 0.2f), size.width / 2, style = Stroke(2f))
            
            // Draw needle heading indicators pointing North
            val center = Offset(size.width / 2, size.height / 2)
            val needleLength = size.width * 0.4f
            
            // Draw N, S markers
            // Let's simple plot line pointing up
            drawLine(
                color = ColorAccentNeonCyan,
                start = center,
                end = Offset(center.x, center.y - needleLength),
                strokeWidth = 3f
            )
            drawLine(
                color = Color.Red,
                start = center,
                end = Offset(center.x, center.y + needleLength),
                strokeWidth = 2f
            )
        }
    }
}

// Custom route planner panel dashboard
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlannerPanel(
    viewModel: DashboardViewModel,
    ways: List<Waypoint>,
    onSaveAsked: () -> Unit,
    onImportAsked: () -> Unit,
    onExportAsked: () -> Unit
) {
    val tempPoints by viewModel.temporaryWaypoints.collectAsState()
    val searchQ by viewModel.searchQuery.collectAsState()
    val searchRes by viewModel.searchResults.collectAsState()

    var isAddingWaypoint by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Text(
            "PLANIFICADOR DE RUTA MUNDIAL",
            fontWeight = FontWeight.Bold,
            color = ColorAccentNeonCyan,
            fontSize = 13.sp,
            letterSpacing = 1.sp
        )
        Text(
            "Selecciona y concatena múltiples medios de transporte en tu viaje",
            color = Color.LightGray,
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Live Search Form
        OutlinedTextField(
            value = searchQ,
            onValueChange = { viewModel.updateSearchQuery(it) },
            label = { Text("Buscar Aeropuerto, Monumento, Coordenadas lat,lng...") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = ColorAccentNeonCyan,
                unfocusedBorderColor = Color.Gray
            ),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, "Search", tint = Color.Gray) },
            modifier = Modifier.fillMaxWidth()
        )

        // Places Search Results List
        if (searchQ.isNotBlank()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 140.dp)
                    .background(ColorSurfaceLight)
            ) {
                items(searchRes) { hub ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.addWaypointToPlanner(hub)
                                viewModel.updateSearchQuery("")
                            }
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(hub.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("${hub.country} • ${hub.type}", color = Color.LightGray, fontSize = 10.sp)
                        }
                        Icon(Icons.Default.AddCircleOutline, "Add Point", tint = ColorAccentNeonGreen)
                    }
                    HorizontalDivider(color = ColorBackground)
                }
                if (searchRes.isEmpty()) {
                    item {
                        Text("No se encontraron coincidencias.", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(8.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Selected Waypoints timeline listing
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("ITINERARIO (${tempPoints.size} PUNTOS)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            Row {
                TextButton(onClick = { viewModel.clearPlanner() }) {
                    Text("Limpiar todo", color = Color.Gray, fontSize = 11.sp)
                }
            }
        }

        // Active List elements Scrollable timeline
        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(tempPoints) { idx, point ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ColorSurfaceLight),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        border = BorderStroke(1.dp, ColorSurfaceLight)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${idx + 1}. ${point.label}",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Simulación mediante: ", color = Color.LightGray, fontSize = 9.sp)
                                    // Transport spinner trigger buttons
                                    val transportOptions = listOf("Car", "Train", "Plane", "Walk")
                                    val mappedTransportLabel = when (point.transportType) {
                                        "Car" -> "COCHE"
                                        "Train" -> "TREN"
                                        "Plane" -> "AVIÓN"
                                        else -> "PASEO"
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(ColorBackground)
                                            .clickable {
                                                val nextOpt = when (point.transportType) {
                                                    "Car" -> "Train"
                                                    "Train" -> "Plane"
                                                    "Plane" -> "Walk"
                                                    else -> "Car"
                                                }
                                                viewModel.updateWaypointTransport(idx, nextOpt)
                                            }
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(mappedTransportLabel, color = ColorAccentNeonCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            IconButton(onClick = { viewModel.removeWaypointFromPlanner(idx) }) {
                                Icon(Icons.Default.RemoveCircleOutline, "Delete Point", tint = Color.Red, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
                
                if (tempPoints.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Map, "Intro", tint = Color.Gray, modifier = Modifier.size(45.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Busca locaciones arriba o haz doble clic sobre el mapa para conformar un plan multiruta.", color = Color.Gray, fontSize = 11.sp, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Setup routing optimizations checkboxes
        Card(
            colors = CardDefaults.cardColors(containerColor = ColorSurfaceLight.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text("OPTIMIZACIÓN DE CÁLCULO DE RUTA", color = Color.LightGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("Rápida", "Corta", "Económica", "Panorámica").forEach { opt ->
                        val isSel = SimulationEngine.selectedRouteType == opt
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSel) ColorAccentNeonCyan else ColorBackground)
                                .clickable { SimulationEngine.selectedRouteType = opt }
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(opt, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.Black else Color.White)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Simulation Action triggers
        Button(
            onClick = { viewModel.applyPlannedRouteToSimulation() },
            colors = ButtonDefaults.buttonColors(containerColor = ColorAccentNeonCyan),
            modifier = Modifier
                .fillMaxWidth()
                .height(45.dp),
            shape = RoundedCornerShape(8.dp),
            enabled = tempPoints.size >= 2
        ) {
            Icon(Icons.Default.DirectionsRun, "Start Sim", tint = Color.Black)
            Spacer(modifier = Modifier.width(8.dp))
            Text("PROYECTAR Y SIMULAR RUTA", color = Color.Black, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Saved routes utility operations
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TextButton(onClick = onSaveAsked, enabled = tempPoints.size >= 2) {
                Icon(Icons.Default.Save, "Save DB", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Guardar", fontSize = 12.sp)
            }
            TextButton(onClick = onExportAsked, enabled = tempPoints.size >= 2) {
                Icon(Icons.Default.IosShare, "Export", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Exportar", fontSize = 12.sp)
            }
            TextButton(onClick = onImportAsked) {
                Icon(Icons.Default.Download, "Import", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Importar", fontSize = 12.sp)
            }
        }
    }
}

// Telemetry information display panel HUD details
@Composable
fun TelemetryHUDDetailsPanel(telemetry: SimulationTelemetry, viewModel: DashboardViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            "CUBIERTA DE INSTRUMENTOS HUD",
            fontWeight = FontWeight.Bold,
            color = ColorAccentNeonCyan,
            fontSize = 13.sp,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Speed Display Panel and Tachometer speedometer layout
        Card(
            colors = CardDefaults.cardColors(containerColor = ColorBackground),
            border = BorderStroke(1.dp, ColorSurfaceLight),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("VELOCIDAD REAL DE SIMULACIÓN", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${telemetry.currentSpeedKmh.toInt()} KM/H",
                    fontSize = 32.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = ColorAccentNeonGreen
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("MÍN", color = Color.Gray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        Text("${telemetry.minSpeedKmh.toInt()} KM/H", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("MED", color = Color.Gray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        Text("${telemetry.averageSpeedKmh.toInt()} KM/H", color = ColorAccentNeonCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("MÁX", color = Color.Gray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        Text("${telemetry.maxSpeedKmh.toInt()} KM/H", color = Color.Red, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        // Elevations panel
        Card(
            colors = CardDefaults.cardColors(containerColor = ColorBackground),
            border = BorderStroke(1.dp, ColorSurfaceLight),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text("PERFIL DE ELEVACIÓN Y PENDIENTE", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Altitud Actual:", color = Color.White, fontSize = 11.sp)
                    Text(viewModel.formatDistanceValue(telemetry.currentAltitude), color = ColorAccentNeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Pendiente Actual:", color = Color.White, fontSize = 11.sp)
                    Text("${telemetry.currentSlope}%", color = if (telemetry.currentSlope >= 0) ColorAccentNeonGreen else Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Text("ALT MÁXIMA: ${telemetry.currentAltitude.coerceAtLeast(300.0).toInt()} m", color = Color.LightGray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    Text("ALT MÍNIMA: ${telemetry.currentAltitude.coerceAtMost(30.0).coerceAtLeast(0.0).toInt()} m", color = Color.LightGray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        // Timing & Remaining Distance Instrumentation HUD Panel (NO numeric abbreviations!)
        Card(
            colors = CardDefaults.cardColors(containerColor = ColorBackground),
            border = BorderStroke(1.dp, ColorSurfaceLight),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text("DISTANCIAS Y LOGÍSTICA DE TIEMPO", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Distancia Total:", color = Color.White, fontSize = 11.sp)
                    Text(viewModel.formatDistanceValue(telemetry.totalDistanceMeters), color = ColorAccentNeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Recorrida:", color = Color.White, fontSize = 11.sp)
                    Text(viewModel.formatDistanceValue(telemetry.distanceCoveredMeters), color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Restante:", color = Color.White, fontSize = 11.sp)
                    Text(viewModel.formatDistanceValue(telemetry.distanceRemainingMeters), color = ColorAccentNeonPink, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = ColorSurfaceLight)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Tiempo Total:", color = Color.White, fontSize = 11.sp)
                    Text(viewModel.formatDurationSeconds(telemetry.totalDurationSeconds), color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Transcurrido:", color = Color.White, fontSize = 11.sp)
                    Text(viewModel.formatDurationSeconds(telemetry.elapsedDurationSeconds), color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Restante:", color = Color.White, fontSize = 11.sp)
                    Text(viewModel.formatDurationSeconds(telemetry.remainingDurationSeconds), color = ColorAccentNeonPink, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("ETA Estimado:", color = Color.White, fontSize = 11.sp)
                    Text(telemetry.etaString, color = ColorAccentNeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }

        // Live Traffic signalling alerts and traffic limits
        Card(
            colors = CardDefaults.cardColors(containerColor = ColorBackground),
            border = BorderStroke(1.dp, ColorSurfaceLight),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text("CONDICIONES DE TRÁFICO Y SEÑALIZACIÓN", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Estado Tráfico:", color = Color.White, fontSize = 11.sp)
                    val trafColor = when (telemetry.trafficCondition) {
                        "Fluido" -> ColorAccentNeonGreen
                        "Moderado" -> ColorAccentNeonAmber
                        else -> Color.Red
                    }
                    Text(telemetry.trafficCondition.uppercase(), color = trafColor, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Alerta de Tránsito:", color = Color.White, fontSize = 11.sp)
                    val signalBoxColor = when (telemetry.activeSignal) {
                        "Semáforo (Rojo)" -> Color.Red
                        "Semáforo (Verde)" -> ColorAccentNeonGreen
                        "Alto" -> ColorAccentNeonPink
                        else -> Color.Gray
                    }
                    Text(telemetry.activeSignal.uppercase(), color = signalBoxColor, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Límite de Velocidad:", color = Color.White, fontSize = 11.sp)
                    Text("${telemetry.speedLimitKmh} KM/H", color = ColorAccentNeonAmber, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

// AI travel guide chat dashboard
@Composable
fun AITravelChatPanel(viewModel: DashboardViewModel) {
    val chatLog by viewModel.aiChatLog.collectAsState()
    val isLoading by viewModel.isAiLoading.collectAsState()

    var userSpeechInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Text(
            "GEOGUÍA DE VIAJE - INTELIGENCIA ARTIFICIAL",
            fontWeight = FontWeight.Bold,
            color = ColorAccentNeonCyan,
            fontSize = 13.sp,
            letterSpacing = 1.sp
        )
        Text(
            "Información demográfica, turística e histórica sobre tus travesías",
            color = Color.LightGray,
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Generate auto summary of active waypoints route
        Button(
            onClick = { viewModel.requestRouteAutosummary() },
            colors = ButtonDefaults.buttonColors(containerColor = ColorSurfaceLight),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(6.dp)
        ) {
            Icon(Icons.Default.AutoAwesome, "Summary", tint = ColorAccentNeonCyan)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Generar Resumen de Ruta por IA", color = Color.White, fontSize = 11.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Chat Log Scrollable Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(ColorBackground, RoundedCornerShape(8.dp))
                .border(1.dp, ColorSurfaceLight)
                .padding(8.dp)
        ) {
            val chatState = rememberLazyListState()
            
            LazyColumn(
                state = chatState,
                modifier = Modifier.fillMaxSize()
            ) {
                items(chatLog) { msg ->
                    val isGemini = msg.sender == "Gemini"
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalAlignment = if (isGemini) Alignment.Start else Alignment.End
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isGemini) ColorSurfaceLight else ColorAccentNeonCyan)
                                .padding(10.dp)
                                .widthIn(max = 240.dp)
                        ) {
                            Text(
                                text = msg.content,
                                color = if (isGemini) Color.White else Color.Black,
                                fontSize = 11.5.sp
                            )
                        }
                        Text(
                            text = if (isGemini) "Gemini Travel Assistant" else "Tú",
                            fontSize = 8.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
                        )
                    }
                }

                if (isLoading) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = ColorAccentNeonCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Gemini analizando orografía...", color = Color.Gray, fontSize = 11.sp)
                        }
                    }
                }
            }

            // Auto Scroll logic
            LaunchedEffect(chatLog.size, isLoading) {
                if (chatLog.isNotEmpty()) {
                    chatState.animateScrollToItem(chatLog.size - 1)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Send Speech messages
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = userSpeechInput,
                onValueChange = { userSpeechInput = it },
                placeholder = { Text("Pregunta sobre París, monumentos de la ruta...", fontSize = 11.sp, color = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = ColorAccentNeonCyan,
                    unfocusedBorderColor = Color.Gray
                ),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    viewModel.sendUserMessage(userSpeechInput)
                    userSpeechInput = ""
                },
                modifier = Modifier
                    .size(45.dp)
                    .background(ColorAccentNeonCyan, RoundedCornerShape(8.dp)),
                colors = IconButtonDefaults.iconButtonColors(contentColor = Color.Black)
            ) {
                Icon(Icons.Default.Send, "Send Chat")
            }
        }
    }
}

// History of runs and saved favorites listing
@Composable
fun HistoryAndFavoritesPanel(
    viewModel: DashboardViewModel,
    historyList: List<RouteHistory>,
    savedRoutes: List<SavedRoute>
) {
    var selectedScreenTab by remember { mutableStateOf("HISTORY") } // "HISTORY", "FAVORITES"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Upper selection controls Tab
        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { selectedScreenTab = "HISTORY" },
                colors = ButtonDefaults.buttonColors(containerColor = if (selectedScreenTab == "HISTORY") ColorAccentNeonCyan else ColorSurfaceLight),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("HISTORIAL SIM", color = if (selectedScreenTab == "HISTORY") Color.Black else Color.White, fontSize = 11.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { selectedScreenTab = "FAVORITES" },
                colors = ButtonDefaults.buttonColors(containerColor = if (selectedScreenTab == "FAVORITES") ColorAccentNeonCyan else ColorSurfaceLight),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("RUTAS GUARDADAS", color = if (selectedScreenTab == "FAVORITES") Color.Black else Color.White, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedScreenTab == "HISTORY") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("HISTORIAL DE SIMULACIONES COMPLETAS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                TextButton(onClick = { viewModel.clearAllHistory() }) {
                    Text("Borrar todo", color = Color.Red, fontSize = 11.sp)
                }
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(historyList) { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ColorSurfaceLight),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(item.routeName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                                IconButton(
                                    onClick = { viewModel.deleteHistoryItem(item.id) },
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Icon(Icons.Default.Delete, "Delete Node", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Distancia: ${DecimalFormat("#,###").format(item.totalDistanceKm)} km  Duración: ${viewModel.formatDurationSeconds(item.totalDurationSeconds)}",
                                color = Color.LightGray,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Med Velocidad: ${item.avgSpeedKmh.toInt()} km/h  Máx: ${item.maxSpeedKmh.toInt()} km/h",
                                color = ColorAccentNeonGreen,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                if (historyList.isEmpty()) {
                    item {
                        Text("No se han registrado simulaciones completadas aún.", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(12.dp))
                    }
                }
            }
        } else {
            // Favorites / Saved routes
            Text("RUTAS GUARDADAS EN BASE DE DATOS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(bottom = 6.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(savedRoutes) { route ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ColorSurfaceLight),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(route.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Puntos: ${route.waypoints.size} • Optimización: ${route.routeType}", color = Color.Gray, fontSize = 10.sp)
                            
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Button(
                                    onClick = { viewModel.loadSavedRouteToSimulation(route) },
                                    colors = ButtonDefaults.buttonColors(containerColor = ColorAccentNeonCyan),
                                    shape = RoundedCornerShape(4.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Simular", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }

                                Row {
                                    IconButton(
                                        onClick = { viewModel.duplicateSavedRoute(route) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, "Duplicate Node", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = { viewModel.deleteSavedRoute(route) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, "Delete Saved Node", tint = Color.Red, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                if (savedRoutes.isEmpty()) {
                    item {
                        Text("No tienes rutas guardadas en memoria local.", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(12.dp))
                    }
                }
            }
        }
    }
}

// User Profile Accounts & sync backups
@Composable
fun CloudSyncAccountPanel(viewModel: DashboardViewModel, accountInfo: UserAccount?) {
    val errorMsg by viewModel.authErrorMsg.collectAsState()
    var emailInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            "SINCRONIZACIÓN Y PERFIL DE USUARIO",
            fontWeight = FontWeight.Bold,
            color = ColorAccentNeonCyan,
            fontSize = 13.sp,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (accountInfo == null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = ColorBackground),
                border = BorderStroke(1.dp, ColorSurfaceLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("REGISTRAR CUENTA SIMULACIÓN", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Nombre Completo") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = ColorAccentNeonCyan,
                            unfocusedBorderColor = Color.Gray
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Correo Electrónico") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = ColorAccentNeonCyan,
                            unfocusedBorderColor = Color.Gray
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.registerMockAccount(emailInput, nameInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = ColorAccentNeonCyan),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Crear Cuenta Local", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = ColorBackground),
                border = BorderStroke(1.dp, ColorSurfaceLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("CUENTA ACTIVA", color = ColorAccentNeonGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text(accountInfo.displayName, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(accountInfo.email, color = Color.Gray, fontSize = 11.sp)
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = ColorSurfaceLight)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Sincronización Automática:", color = Color.LightGray, fontSize = 11.sp)
                        Text(if (accountInfo.syncEnabled) "SI" else "NO", color = ColorAccentNeonCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.handleSyncCloudData() },
                        colors = ButtonDefaults.buttonColors(containerColor = ColorSurfaceLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CloudSync, "Sync Backup")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sincronizar favorítos ahora", color = Color.White)
                    }
                }
            }
        }

        if (errorMsg != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2E0C0E)),
                border = BorderStroke(1.dp, Color.Red),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Error, "Error", tint = Color.Red)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(errorMsg!!, color = Color.White, fontSize = 11.sp)
                }
            }
        }
    }
}

object ColorUtils {
    fun overlayAlphaBack(): Color {
        return Color(0x9913171F)
    }
}
