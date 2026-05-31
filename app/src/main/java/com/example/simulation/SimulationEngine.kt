package com.example.simulation

import com.example.data.models.Waypoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

data class WeatherState(
    val condition: String = "Despejado", // "Soleado", "Lluvia Fuerte", "Nevada Ligera", "Niebla Densa", "Tormenta"
    val temperatureCelsius: Double = 18.0,
    val humidityPercentage: Int = 55,
    val windSpeedKmh: Double = 12.0,
    val windDirectionDegrees: Int = 180,
    val visibilityMeters: Double = 10000.0,
    val cloudCoverPercentage: Int = 10,
    val rainRateMmHour: Double = 0.0,
    val snowAccumulationCmHour: Double = 0.0
)

data class SimulationTelemetry(
    val currentLatitude: Double = 0.0,
    val currentLongitude: Double = 0.0,
    val currentAltitude: Double = 0.0,
    val currentSlope: Double = 0.0, // Pendiente porcentual %
    val currentSpeedKmh: Double = 0.0,
    val averageSpeedKmh: Double = 0.0,
    val maxSpeedKmh: Double = 0.0,
    val minSpeedKmh: Double = 0.0,
    val totalDistanceMeters: Double = 0.0,
    val distanceCoveredMeters: Double = 0.0,
    val distanceRemainingMeters: Double = 0.0,
    val totalDurationSeconds: Long = 0,
    val elapsedDurationSeconds: Long = 0,
    val remainingDurationSeconds: Long = 0,
    val etaString: String = "00:00",
    val currentCountry: String = "Océano Global",
    val currentCity: String = "Mar Abierto",
    val currentTimezone: String = "UTC",
    val activeTransport: String = "Coche",
    val activeSegmentIndex: Int = 0,
    val speedLimitKmh: Int = 120,
    val trafficCondition: String = "Fluido", // "Fluido", "Moderado", "Lento", "Retención"
    val activeSignal: String = "Ninguno", // "Ninguno", "Semáforo (Rojo)", "Semáforo (Verde)", "Alto", "Ceda el Paso"
    val speedLimitSignActive: Boolean = false,
    val weather: WeatherState = WeatherState()
)

data class SimPathNode(
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double,
    val slopePercent: Double,
    val targetSpeedKmh: Int,
    val segmentIndex: Int,
    val country: String,
    val city: String,
    val timezone: String,
    val transportType: String,
    val signalType: String = "Ninguno",
    val trafficCondition: String = "Fluido"
)

object SimulationEngine {

    private val _telemetry = MutableStateFlow(SimulationTelemetry())
    val telemetry: StateFlow<SimulationTelemetry> = _telemetry.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _waypoints = MutableStateFlow<List<Waypoint>>(emptyList())
    val waypoints: StateFlow<List<Waypoint>> = _waypoints.asStateFlow()

    private val _computedPath = MutableStateFlow<List<SimPathNode>>(emptyList())
    val computedPath: StateFlow<List<SimPathNode>> = _computedPath.asStateFlow()

    // Configuration
    var simulationSpeedMultiplier = MutableStateFlow(1) // 1x, 2x, 5x, 10x, 50x, 100x, 1000x
    var selectedRouteType = "Rápida" // "Rápida", "Corta", "Económica", "Panorámica", "Tráfico Mínimo"

    private var activePathNodeIndex = 0
    private var simulationJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Historical records
    private var speedRecordList = mutableListOf<Double>()

    fun setSimulationWaypoints(points: List<Waypoint>) {
        _waypoints.value = points
        speedRecordList.clear()
        if (points.size >= 2) {
            calculateSimulationPath(points)
        } else {
            _computedPath.value = emptyList()
            _telemetry.value = SimulationTelemetry()
        }
        activePathNodeIndex = 0
    }

    fun startSimulation() {
        if (_computedPath.value.isEmpty()) return
        _isRunning.value = true
        simulationJob?.cancel()
        simulationJob = coroutineScope.launch {
            runSimulationLoop()
        }
    }

    fun pauseSimulation() {
        _isRunning.value = false
        simulationJob?.cancel()
    }

    fun stopSimulation() {
        _isRunning.value = false
        simulationJob?.cancel()
        activePathNodeIndex = 0
        speedRecordList.clear()
        updateTelemetryForNode(0)
    }

    fun restartSimulation() {
        stopSimulation()
        startSimulation()
    }

    fun setMultiplier(multiplier: Int) {
        simulationSpeedMultiplier.value = multiplier
    }

    private fun calculateSimulationPath(points: List<Waypoint>) {
        val nodes = mutableListOf<SimPathNode>()
        
        for (i in 0 until points.size - 1) {
            val start = points[i]
            val end = points[i + 1]
            
            // Interpolate points based on distance & transport mode
            val distanceKm = calculateHaversineDistance(start.latitude, start.longitude, end.latitude, end.longitude)
            
            // Determine segment transport characteristics
            val transport = start.transportType
            val stepsCount = max(10, min(120, (distanceKm / if (transport == "Avión") 80.0 else 5.0).toInt()))
            
            val startHub = EarthDataEngine.hubs.find { abs(it.latitude - start.latitude) < 0.1 && abs(it.longitude - start.longitude) < 0.1 }
            val endHub = EarthDataEngine.hubs.find { abs(it.latitude - end.latitude) < 0.1 && abs(it.longitude - end.longitude) < 0.1 }

            val sourceCountry = startHub?.country ?: "Océano Global"
            val destCountry = endHub?.country ?: sourceCountry
            val timezoneStr = startHub?.timezone ?: "UTC+0"

            val maxSpeedKmh = when (transport) {
                "Avión" -> 850
                "Tren" -> 250
                "Metro", "Tranvía" -> 80
                "Camión", "Autobús" -> 90
                "Coche", "Moto" -> 120
                "Bicicleta" -> 22
                "Caminando" -> 5
                else -> 100
            }

            for (step in 0..stepsCount) {
                val fraction = step.toDouble() / stepsCount
                
                // Linear lat/lng interpolation (or fine geodesic arc pathing)
                val lat = start.latitude + (end.latitude - start.latitude) * fraction
                val lng = start.longitude + (end.longitude - start.longitude) * fraction

                // Elevation calculations
                val startAlt = startHub?.altitudeMeters ?: 50.0
                val endAlt = endHub?.altitudeMeters ?: 50.0
                val baseAltitude: Double = startAlt + (endAlt - startAlt) * fraction
                val currentAltitude = if (transport == "Avión") {
                    // Parabolic Flight flight profile (takeoff -> climb -> cruise -> descent -> land)
                    val flightClimbFraction = 0.15
                    val flightDescFraction = 0.85
                    when {
                        fraction < flightClimbFraction -> {
                            val ratio = fraction / flightClimbFraction
                            baseAltitude + (10200.0 - baseAltitude) * sin(ratio * kotlin.math.PI / 2)
                        }
                        fraction > flightDescFraction -> {
                            val ratio = (1.0 - fraction) / (1.0 - flightDescFraction)
                            baseAltitude + (10200.0 - baseAltitude) * sin(ratio * kotlin.math.PI / 2)
                        }
                        else -> 10200.0 // Cruise Altitude FL330
                    }
                } else {
                    // Standard ground with wave elements simulating valleys/hills
                    baseAltitude + (sin(fraction * kotlin.math.PI * 18) * 45.0)
                }

                // Slope calculations
                val slope = if (transport == "Avión") {
                    if (fraction < 0.15) 12.5 else if (fraction > 0.85) -10.0 else 0.0
                } else {
                    val angle = cos(fraction * kotlin.math.PI * 18) * 4.5
                    round(angle * 10) / 10
                }

                // Speed limits and signaling events
                var signal = "Ninguno"
                var traffic = "Fluido"
                var finalTargetSpeed = maxSpeedKmh

                if (transport != "Avión" && transport != "Barco") {
                    // Periodic signals along land paths
                    if (step % 25 == 20) {
                        signal = "Semáforo (Rojo)"
                        finalTargetSpeed = 0
                    } else if (step % 40 == 10) {
                        signal = "Alto"
                        finalTargetSpeed = 0
                    } else if (step % 30 == 15) {
                        traffic = listOf("Moderado", "Congestión", "Obras").random()
                        finalTargetSpeed = (maxSpeedKmh * 0.4).toInt()
                    }
                }

                val segmentCity = if (fraction < 0.5) startHub?.name?.split("-")?.firstOrNull()?.trim() ?: "Carretera" 
                                  else endHub?.name?.split("-")?.firstOrNull()?.trim() ?: "Ruta Rural"

                nodes.add(
                    SimPathNode(
                        latitude = lat,
                        longitude = lng,
                        altitudeMeters = currentAltitude,
                        slopePercent = slope,
                        targetSpeedKmh = finalTargetSpeed,
                        segmentIndex = i,
                        country = if (fraction < 0.5) sourceCountry else destCountry,
                        city = segmentCity,
                        timezone = timezoneStr,
                        transportType = transport,
                        signalType = signal,
                        trafficCondition = traffic
                    )
                )
            }
        }
        _computedPath.value = nodes
    }

    private suspend fun runSimulationLoop() {
        while (activePathNodeIndex < _computedPath.value.size && _isRunning.value) {
            updateTelemetryForNode(activePathNodeIndex)
            
            // Speed logic based on time multiples
            val currentWarp = simulationSpeedMultiplier.value
            val delayDuration = when {
                currentWarp >= 1000 -> 2L
                currentWarp >= 100 -> 10L
                currentWarp >= 50 -> 20L
                currentWarp >= 10 -> 80L
                currentWarp >= 5 -> 150L
                currentWarp >= 2 -> 300L
                else -> 600L // 1x Speed updates
            }
            
            delay(delayDuration)
            
            // Advance indexes proportional to warp speeds
            val indexStride = when {
                currentWarp >= 1000 -> 8
                currentWarp >= 100 -> 3
                currentWarp >= 10 -> 2
                else -> 1
            }
            
            activePathNodeIndex += indexStride
        }
        
        if (activePathNodeIndex >= _computedPath.value.size) {
            _isRunning.value = false
            activePathNodeIndex = _computedPath.value.size - 1
            updateTelemetryForNode(activePathNodeIndex)
        }
    }

    private fun updateTelemetryForNode(index: Int) {
        val path = _computedPath.value
        if (path.isEmpty() || index < 0 || index >= path.size) return
        
        val node = path[index]

        // Calculate distances based on index ratios
        val totalDistanceM = calculateTotalPathDistanceMeters(path)
        val progressFract = index.toDouble() / (path.size - 1)
        val distanceCoveredM = totalDistanceM * progressFract
        val distanceRemainingM = totalDistanceM - distanceCoveredM

        // Calculate dynamic live speech/speeds
        val randomSpeedFluct = (node.targetSpeedKmh * (0.9 + RandomSpec.nextDouble(0.15)))
        val currentKmh = if (node.targetSpeedKmh == 0) 0.0 else max(2.0, min(node.targetSpeedKmh.toDouble(), randomSpeedFluct))
        
        speedRecordList.add(currentKmh)
        val avgSpeed = if (speedRecordList.isEmpty()) 0.0 else speedRecordList.average()
        val maxSpeed = if (speedRecordList.isEmpty()) 0.0 else speedRecordList.maxOrNull() ?: 0.0
        val minSpeed = if (speedRecordList.isEmpty()) 0.0 else speedRecordList.minOrNull() ?: 0.0

        // Time logic: Average transport rate determines remaining hours
        val timeDivider = if (avgSpeed > 0) avgSpeed else 50.0
        val totalHours = (totalDistanceM / 1000.0) / timeDivider
        val totalSeconds = (totalHours * 3600.0).toLong()
        val elapsedSeconds = (totalSeconds * progressFract).toLong()
        val remainingSeconds = totalSeconds - elapsedSeconds

        // Dynamic ETA calculation
        val currentCalendar = Calendar.getInstance()
        currentCalendar.add(Calendar.SECOND, remainingSeconds.toInt())
        val etaFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val etaStr = etaFormat.format(currentCalendar.time)

        // Dynamic meteorology conditions mapped to location latitudes
        val isNorthCold = node.latitude > 50.0
        val isEquatorHot = abs(node.latitude) < 20.0
        
        val weatherCondition = when {
            isNorthCold && node.latitude > 60.0 -> "Nevada Ligera"
            isEquatorHot -> "Tormenta"
            index % 30 == 12 -> "Lluvia Fuerte"
            index % 45 == 8 -> "Niebla Densa"
            else -> "Soleado"
        }

        val weatherStateValue = when (weatherCondition) {
            "Nevada Ligera" -> WeatherState(condition = "Nevada Ligera", temperatureCelsius = -2.0, humidityPercentage = 85, windSpeedKmh = 25.0, cloudCoverPercentage = 95, snowAccumulationCmHour = 1.2)
            "Tormenta" -> WeatherState(condition = "Tormenta Eléctrica", temperatureCelsius = 28.0, humidityPercentage = 90, windSpeedKmh = 45.0, cloudCoverPercentage = 100, rainRateMmHour = 20.0)
            "Lluvia Fuerte" -> WeatherState(condition = "Lluvia Fuerte", temperatureCelsius = 14.0, humidityPercentage = 95, windSpeedKmh = 18.0, cloudCoverPercentage = 90, rainRateMmHour = 8.5)
            "Niebla Densa" -> WeatherState(condition = "Niebla Densa", temperatureCelsius = 8.0, humidityPercentage = 100, windSpeedKmh = 3.0, visibilityMeters = 250.0, cloudCoverPercentage = 70)
            else -> WeatherState(condition = "Soleado", temperatureCelsius = if (isEquatorHot) 32.0 else 22.0, humidityPercentage = 45, windSpeedKmh = 8.0, visibilityMeters = 15000.0, cloudCoverPercentage = 0)
        }

        _telemetry.value = SimulationTelemetry(
            currentLatitude = node.latitude,
            currentLongitude = node.longitude,
            currentAltitude = node.altitudeMeters,
            currentSlope = node.slopePercent,
            currentSpeedKmh = currentKmh,
            averageSpeedKmh = avgSpeed,
            maxSpeedKmh = maxSpeed,
            minSpeedKmh = if (minSpeed == 0.0 && currentKmh > 0) currentKmh else minSpeed,
            totalDistanceMeters = totalDistanceM,
            distanceCoveredMeters = distanceCoveredM,
            distanceRemainingMeters = distanceRemainingM,
            totalDurationSeconds = totalSeconds,
            elapsedDurationSeconds = elapsedSeconds,
            remainingDurationSeconds = remainingSeconds,
            etaString = etaStr,
            currentCountry = node.country,
            currentCity = node.city,
            currentTimezone = node.timezone,
            activeTransport = node.transportType,
            activeSegmentIndex = node.segmentIndex,
            speedLimitKmh = if (node.transportType == "Avión") 900 else node.targetSpeedKmh + 20,
            trafficCondition = node.trafficCondition,
            activeSignal = node.signalType,
            speedLimitSignActive = node.targetSpeedKmh > 0,
            weather = weatherStateValue
        )
    }

    private fun calculateTotalPathDistanceMeters(path: List<SimPathNode>): Double {
        if (path.size < 2) return 0.0
        var sum = 0.0
        for (i in 0 until path.size - 1) {
            sum += calculateHaversineDistance(
                path[i].latitude, path[i].longitude,
                path[i + 1].latitude, path[i + 1].longitude
            )
        }
        return sum * 1000.0 // convert to meters
    }

    private fun calculateHaversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Earth's Radius Km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}

object RandomSpec {
    private val rand = java.util.Random()
    fun nextDouble(bound: Double): Double {
        return rand.nextDouble() * bound
    }
}
