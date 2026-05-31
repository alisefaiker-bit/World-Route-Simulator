package com.example.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_accounts")
data class UserAccount(
    @PrimaryKey val id: Int = 1, // Single active local user slot
    val email: String,
    val displayName: String,
    val favoritesJson: String = "", // Semicolon/comma separated favorites list
    val syncEnabled: Boolean = true,
    val lastSyncAt: Long = System.currentTimeMillis()
)
