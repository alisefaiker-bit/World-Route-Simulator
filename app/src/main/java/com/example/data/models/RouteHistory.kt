package com.example.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "route_history")
data class RouteHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val routeName: String,
    val waypoints: List<Waypoint>,
    val totalDistanceKm: Double,
    val totalDurationSeconds: Long,
    val avgSpeedKmh: Double,
    val maxSpeedKmh: Double,
    val completedAt: Long = System.currentTimeMillis()
)
