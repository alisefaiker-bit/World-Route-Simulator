package com.example.data.database

import androidx.room.*
import com.example.data.models.SavedRoute
import com.example.data.models.RouteHistory
import com.example.data.models.UserAccount
import kotlinx.coroutines.flow.Flow

@Dao
interface SimulationDao {
    // Saved Routes
    @Query("SELECT * FROM saved_routes ORDER BY createdAt DESC")
    fun getAllSavedRoutes(): Flow<List<SavedRoute>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedRoute(route: SavedRoute): Long

    @Delete
    suspend fun deleteSavedRoute(route: SavedRoute)

    @Query("DELETE FROM saved_routes WHERE id = :id")
    suspend fun deleteSavedRouteById(id: Int)

    // Route Histories
    @Query("SELECT * FROM route_history ORDER BY completedAt DESC")
    fun getAllRouteHistories(): Flow<List<RouteHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRouteHistory(history: RouteHistory): Long

    @Query("DELETE FROM route_history WHERE id = :id")
    suspend fun deleteRouteHistoryById(id: Int)

    @Query("DELETE FROM route_history")
    suspend fun clearHistory()

    // User Account
    @Query("SELECT * FROM user_accounts WHERE id = 1 LIMIT 1")
    fun getUserAccountFlow(): Flow<UserAccount?>

    @Query("SELECT * FROM user_accounts WHERE id = 1 LIMIT 1")
    suspend fun getUserAccountDirect(): UserAccount?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserAccount(account: UserAccount)
}
