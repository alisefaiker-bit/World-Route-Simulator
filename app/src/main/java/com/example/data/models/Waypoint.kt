package com.example.data.models

import androidx.room.TypeConverter

data class Waypoint(
    val latitude: Double,
    val longitude: Double,
    val label: String,
    val transportType: String // e.g., "Car", "Train", "Plane", "Walk", "Ship", etc.
)

class WaypointListConverter {
    @TypeConverter
    fun fromString(value: String?): List<Waypoint> {
        if (value.isNullOrBlank()) return emptyList()
        return value.split("\n").mapNotNull { line ->
            val parts = line.split("|")
            if (parts.size >= 4) {
                Waypoint(
                    latitude = parts[0].toDoubleOrNull() ?: 0.0,
                    longitude = parts[1].toDoubleOrNull() ?: 0.0,
                    label = parts[2],
                    transportType = parts[3]
                )
            } else null
        }
    }

    @TypeConverter
    fun toString(list: List<Waypoint>?): String {
        if (list == null) return ""
        return list.joinToString("\n") { "${it.latitude}|${it.longitude}|${it.label}|${it.transportType}" }
    }
}
