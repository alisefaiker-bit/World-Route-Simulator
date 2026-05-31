package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.data.database.SimulationDatabase
import com.example.data.repository.SimulationRepository
import com.example.ui.DashboardScreen
import com.example.ui.DashboardViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Room persistence layer holding routes and accounts data
        val database = SimulationDatabase.getInstance(applicationContext)
        val repository = SimulationRepository(database.dao())
        
        // Construct the viewmodel factory
        val factory = DashboardViewModel.Factory(application, repository)
        val viewModel = ViewModelProvider(this, factory)[DashboardViewModel::class.java]
        
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                DashboardScreen(viewModel = viewModel)
            }
        }
    }
}
