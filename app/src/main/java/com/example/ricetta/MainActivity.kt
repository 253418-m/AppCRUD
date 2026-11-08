package com.upchiapas.recetario

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ricetta.ui.theme.RicettaTheme
import com.upchiapas.recetario.presentation.RecetarioApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { RicettaTheme { RecetarioApp() } }
    }
}
