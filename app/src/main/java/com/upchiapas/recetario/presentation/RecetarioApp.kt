package com.upchiapas.recetario.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun RecetarioApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "welcome") {
        composable("welcome") { WelcomeScreen() }
    }
}

@Composable
private fun WelcomeScreen() {
    Scaffold { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.widthIn(max = 600.dp)
                    .verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Recetario", style = MaterialTheme.typography.headlineLarge)
                Text("Tu espacio personal para organizar recetas.",
                    style = MaterialTheme.typography.titleMedium)
                Card {
                    Text(
                        "Proyecto en preparación · Etapa 1\n\nEsta pantalla temporal verifica la base de navegación y diseño.",
                        modifier = Modifier.padding(24.dp)
                    )
                }
            }
        }
    }
}
