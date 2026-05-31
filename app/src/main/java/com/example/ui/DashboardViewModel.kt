package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.models.RouteHistory
import com.example.data.models.SavedRoute
import com.example.data.models.UserAccount
import com.example.data.models.Waypoint
import com.example.data.repository.SimulationRepository
import com.example.data.network.GeminiClient
import com.example.simulation.EarthDataEngine
import com.example.simulation.LocationHub
import com.example.simulation.SimulationEngine
import com.example.simulation.SimulationTelemetry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.DecimalFormat

data class ChatMessage(
    val sender: String, // "User" or "Gemini"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

class DashboardViewModel(
    application: Application,
    private val repository: SimulationRepository
) : AndroidViewModel(application) {

    // Main DB flows
    val savedRoutes: StateFlow<List<SavedRoute>> = repository.savedRoutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val routeHistory: StateFlow<List<RouteHistory>> = repository.routeHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userAccount: StateFlow<UserAccount?> = repository.userAccount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Simulation Live flows
    val activeTelemetry: StateFlow<SimulationTelemetry> = SimulationEngine.telemetry
    val isRunning: StateFlow<Boolean> = SimulationEngine.isRunning
    val computedPath = SimulationEngine.computedPath
    val simulationWaypoints = SimulationEngine.waypoints
    val speedWarp = SimulationEngine.simulationSpeedMultiplier

    // UI Interactive States
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<LocationHub>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _aiChatLog = MutableStateFlow<List<ChatMessage>>(
        listOf(ChatMessage("Gemini", "¡Bienvenido al World Route Simulator! Soy tu asesor de viajes y analista de rutas de IA. ¿Hacia dónde nos dirigimos hoy? Puedo explicarte la historia de las ciudades o resumirte los hitos de nuestra ruta activa."))
    )
    val aiChatLog = _aiChatLog.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading = _isAiLoading.asStateFlow()

    // Temporary Planner Waypoints State
    private val _temporaryWaypoints = MutableStateFlow<List<Waypoint>>(emptyList())
    val temporaryWaypoints = _temporaryWaypoints.asStateFlow()

    // Recording and captured simulation states
    private val _screenRecordingState = MutableStateFlow("Idle") // "Idle", "Recording", "Captured_JPEG", "Finished_Timelapse"
    val screenRecordingState = _screenRecordingState.asStateFlow()

    // Authentication Modal state
    private val _authErrorMsg = MutableStateFlow<String?>(null)
    val authErrorMsg = _authErrorMsg.asStateFlow()

    // Import/Export dialog strings
    private val _routeShareString = MutableStateFlow("")
    val routeShareString = _routeShareString.asStateFlow()

    init {
        // Collect telemetry to auto-add histories upon completion
        viewModelScope.launch {
            var lastRecordedCompleted = false
            activeTelemetry.collect { telemetry ->
                val path = computedPath.value
                val ways = simulationWaypoints.value
                if (path.isNotEmpty() && telemetry.distanceRemainingMeters <= 2.0 && !isRunning.value && !lastRecordedCompleted && ways.size >= 2) {
                    lastRecordedCompleted = true
                    // Auto-record completion
                    val name = "${ways.first().label} a ${ways.last().label} (${ways.first().transportType})"
                    repository.addHistory(
                        RouteHistory(
                            routeName = name,
                            waypoints = ways,
                            totalDistanceKm = telemetry.totalDistanceMeters / 1000.0,
                            totalDurationSeconds = telemetry.totalDurationSeconds,
                            avgSpeedKmh = telemetry.averageSpeedKmh,
                            maxSpeedKmh = telemetry.maxSpeedKmh
                        )
                    )
                } else if (telemetry.distanceRemainingMeters > 50.0 && isRunning.value) {
                    lastRecordedCompleted = false
                }
            }
        }
    }

    // Search Operations
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        _searchResults.value = EarthDataEngine.search(query)
    }

    // Waypoint management
    fun addWaypointToPlanner(hub: LocationHub, transportType: String = "Coche") {
        val current = _temporaryWaypoints.value.toMutableList()
        current.add(
            Waypoint(
                latitude = hub.latitude,
                longitude = hub.longitude,
                label = hub.name,
                transportType = transportType
            )
        )
        _temporaryWaypoints.value = current
    }

    fun removeWaypointFromPlanner(index: Int) {
        val current = _temporaryWaypoints.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _temporaryWaypoints.value = current
        }
    }

    fun updateWaypointTransport(index: Int, transport: String) {
        val current = _temporaryWaypoints.value.toMutableList()
        if (index in current.indices) {
            val old = current[index]
            current[index] = old.copy(transportType = transport)
            _temporaryWaypoints.value = current
        }
    }

    fun applyPlannedRouteToSimulation() {
        if (_temporaryWaypoints.value.size >= 2) {
            SimulationEngine.setSimulationWaypoints(_temporaryWaypoints.value)
            SimulationEngine.startSimulation()
        }
    }

    fun clearPlanner() {
        _temporaryWaypoints.value = emptyList()
    }

    // DB Operations
    fun savePlannedRoute(customName: String) = viewModelScope.launch(Dispatchers.IO) {
        val points = _temporaryWaypoints.value
        if (points.size >= 2) {
            repository.saveRoute(
                SavedRoute(
                    name = if (customName.isBlank()) "${points.first().label} a ${points.last().label}" else customName,
                    waypoints = points,
                    routeType = SimulationEngine.selectedRouteType
                )
            )
        }
    }

    fun loadSavedRouteToSimulation(route: SavedRoute) {
        _temporaryWaypoints.value = route.waypoints
        SimulationEngine.setSimulationWaypoints(route.waypoints)
        SimulationEngine.selectedRouteType = route.routeType
        SimulationEngine.startSimulation()
    }

    fun duplicateSavedRoute(route: SavedRoute) = viewModelScope.launch(Dispatchers.IO) {
        repository.saveRoute(
            SavedRoute(
                name = "${route.name} (Copia)",
                waypoints = route.waypoints,
                routeType = route.routeType
            )
        )
    }

    fun deleteSavedRoute(route: SavedRoute) = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteRoute(route)
    }

    fun deleteHistoryItem(id: Int) = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteHistoryById(id)
    }

    fun clearAllHistory() = viewModelScope.launch(Dispatchers.IO) {
        repository.clearHistory()
    }

    // AI travel guide actions
    fun sendUserMessage(text: String) {
        if (text.isBlank()) return
        val current = _aiChatLog.value.toMutableList()
        current.add(ChatMessage("User", text))
        _aiChatLog.value = current

        _isAiLoading.value = true
        viewModelScope.launch {
            val prompt = buildTravelAssistantPrompt(text)
            val answer = GeminiClient.getGuideInfo(prompt)
            current.add(ChatMessage("Gemini", answer))
            _aiChatLog.value = current.toList()
            _isAiLoading.value = false
        }
    }

    fun requestRouteAutosummary() {
        val ways = simulationWaypoints.value
        if (ways.size < 2) {
            val current = _aiChatLog.value.toMutableList()
            current.add(ChatMessage("Gemini", "No hay ninguna simulación de ruta activa para resumir. ¡Planifica una ruta utilizando el panel izquierdo primero!"))
            _aiChatLog.value = current
            return
        }

        _isAiLoading.value = true
        viewModelScope.launch {
            val routeDesc = ways.joinToString(" -> ") { "${it.label} (${it.transportType})" }
            val prompt = """
                Proporciona un resumen turístico, demográfico y cultural emocionante sobre la ruta aérea/terrestre planificada:
                Ruta: $routeDesc
                
                Describe brevemente qué verán los viajeros, los contrastes culturales, distancias y puntos históricos destacados de esta emocionante travesía. Todo redactado en español.
            """.trimIndent()
            
            val answer = GeminiClient.getGuideInfo(prompt, "You are an expert global Geographer and tourism guide.")
            val current = _aiChatLog.value.toMutableList()
            current.add(ChatMessage("Gemini", answer))
            _aiChatLog.value = current
            _isAiLoading.value = false
        }
    }

    private fun buildTravelAssistantPrompt(userText: String): String {
        val telemetryValue = activeTelemetry.value
        return """
            El usuario está interactuando con la aplicación 'World Route Simulator'.
             
            Detalles de ubicación actual del simulador GPS:
            - Latitud: ${telemetryValue.currentLatitude}
            - Longitud: ${telemetryValue.currentLongitude}
            - Altura Actual: ${formatDistanceValue(telemetryValue.currentAltitude)} m
            - País Actual: ${telemetryValue.currentCountry}
            - Ciudad/Sección Actual: ${telemetryValue.currentCity}
            - Transporte: ${telemetryValue.activeTransport}
            - Clima Actual: ${telemetryValue.weather.condition}, Temp: ${telemetryValue.weather.temperatureCelsius}°C
            
            Pregunta del Usuario: "$userText"
            
            Proporciona datos históricos, demográficos y locales de interés enfocándote en la ubicación geográfica de interés o respondiendo a su pregunta en español. Sé profesional, directo y conversacional. No incluyas explicaciones de código.
        """.trimIndent()
    }

    // Export/Import formats
    fun exportRouteShareCode(): String {
        val points = _temporaryWaypoints.value
        if (points.isEmpty()) return "VACÍO"
        val serialized = points.joinToString(";") { "${it.latitude},${it.longitude},${it.label},${it.transportType}" }
        val finalCode = "WRSIM_v1|$serialized"
        _routeShareString.value = finalCode
        return finalCode
    }

    fun importRouteShareCode(code: String): Boolean {
        if (!code.startsWith("WRSIM_v1|")) return false
        try {
            val data = code.removePrefix("WRSIM_v1|")
            val segments = data.split(";")
            val parsedWaypoints = segments.mapNotNull { seg ->
                val fields = seg.split(",")
                if (fields.size >= 4) {
                    Waypoint(
                        latitude = fields[0].toDoubleOrNull() ?: 0.0,
                        longitude = fields[1].toDoubleOrNull() ?: 0.0,
                        label = fields[2],
                        transportType = fields[3]
                    )
                } else null
            }
            if (parsedWaypoints.size >= 2) {
                _temporaryWaypoints.value = parsedWaypoints
                SimulationEngine.setSimulationWaypoints(parsedWaypoints)
                return true
            }
        } catch (e: Exception) {
            // Error parser recovery
        }
        return false
    }

    // Capture Screengrab & timelapses simulations
    fun captureScreengrab() {
         _screenRecordingState.value = "Capturing"
         viewModelScope.launch {
             delay(1500)
             _screenRecordingState.value = "Captured_JPEG"
             delay(2500)
             _screenRecordingState.value = "Idle"
         }
    }

    fun generateTimelapseVideo() {
        _screenRecordingState.value = "GeneratingTimelapse"
        viewModelScope.launch {
            // Mock dynamic frame processing states
            delay(1200)
            _screenRecordingState.value = "ProcessingFrames"
            delay(1300)
            _screenRecordingState.value = "Finished_Timelapse"
            delay(2500)
            _screenRecordingState.value = "Idle"
        }
    }

    // Sync cloud backup and Account profiling
    fun registerMockAccount(email: String, name: String) = viewModelScope.launch(Dispatchers.IO) {
        if (email.isBlank() || name.isBlank()) {
            _authErrorMsg.value = "Introduce un correo electrónico y nombre válidos."
            return@launch
        }
        _authErrorMsg.value = null
        repository.upsertUser(
            UserAccount(
                email = email,
                displayName = name,
                syncEnabled = true
            )
        )
    }

    fun handleSyncCloudData() = viewModelScope.launch(Dispatchers.IO) {
        val user = repository.getUserAccount()
        if (user == null) {
            _authErrorMsg.value = "Inicia sesión primero para poder sincronizar."
            return@launch
        }
        _authErrorMsg.value = "Sincronizando datos con la nube de World Route..."
        delay(2000) // Simulation Network delay
        _authErrorMsg.value = "Sincronización Completada con Éxito. Datos migrados."
        repository.upsertUser(user.copy(lastSyncAt = System.currentTimeMillis()))
        delay(2500)
        _authErrorMsg.value = null
    }

    fun clearAuthErrorMessage() {
        _authErrorMsg.value = null
    }

    // Helper functions for raw values formatting
    fun formatDistanceValue(meters: Double): String {
        val formatter = DecimalFormat("#,###")
        val km = meters / 1000.0
        return if (km >= 1.0) {
            "${formatter.format(km.toInt())} km"
        } else {
            "${formatter.format(meters.toInt())} m"
        }
    }

    fun formatDurationSeconds(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) {
            String.format("%d h %02d min", h, m)
        } else {
            String.format("%d min %02d s", m, s)
        }
    }

    // Factory Class for direct constructor instantiation
    class Factory(
        private val application: Application,
        private val repository: SimulationRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
                return DashboardViewModel(application, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
