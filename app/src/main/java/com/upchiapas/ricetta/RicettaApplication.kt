package com.upchiapas.ricetta

import android.app.Application
import com.upchiapas.ricetta.core.di.AppContainer

class RicettaApplication : Application() {
    val appContainer: AppContainer by lazy { AppContainer(this) }
}
