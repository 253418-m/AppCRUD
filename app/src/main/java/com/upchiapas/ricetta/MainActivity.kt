package com.upchiapas.ricetta

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.upchiapas.ricetta.presentation.theme.RicettaTheme
import com.upchiapas.ricetta.presentation.RicettaApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as RicettaApplication).appContainer
        setContent {
            RicettaTheme {
                RicettaApp(container.authRepository, container.sessionStorage)
            }
        }
    }
}
