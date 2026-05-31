package com.example.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_routes")
data class SavedRoute(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val waypoints: List<Waypoint>,
    val routeType: String, // e.g. "Fastest", "Shortest", "Scenic"
    val createdAt: Long = System.currentTimeMillis()
)
