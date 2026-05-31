package com.example.data.repository

import com.example.data.database.SimulationDao
import com.example.data.models.SavedRoute
import com.example.data.models.RouteHistory
import com.example.data.models.UserAccount
import kotlinx.coroutines.flow.Flow

class SimulationRepository(private val dao: SimulationDao) {

    val savedRoutes: Flow<List<SavedRoute>> = dao.getAllSavedRoutes()
    val routeHistory: Flow<List<RouteHistory>> = dao.getAllRouteHistories()
    val userAccount: Flow<UserAccount?> = dao.getUserAccountFlow()

    suspend fun saveRoute(route: SavedRoute): Long {
        return dao.insertSavedRoute(route)
    }

    suspend fun deleteRoute(route: SavedRoute) {
        dao.deleteSavedRoute(route)
    }

    suspend fun deleteRouteById(id: Int) {
        dao.deleteSavedRouteById(id)
    }

    suspend fun addHistory(history: RouteHistory): Long {
        return dao.insertRouteHistory(history)
    }

    suspend fun deleteHistoryById(id: Int) {
        dao.deleteRouteHistoryById(id)
    }

    suspend fun clearHistory() {
        dao.clearHistory()
    }

    suspend fun getUserAccount(): UserAccount? {
        return dao.getUserAccountDirect()
    }

    suspend fun upsertUser(user: UserAccount) {
        dao.upsertUserAccount(user)
    }
}
