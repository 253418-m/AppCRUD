package com.upchiapas.ricetta.presentation.auth

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.upchiapas.ricetta.domain.repository.AuthRepository
import com.upchiapas.ricetta.domain.session.SessionStorage
import com.upchiapas.ricetta.presentation.auth.login.LoginViewModel
import com.upchiapas.ricetta.presentation.auth.register.RegisterViewModel
import com.upchiapas.ricetta.presentation.session.SessionViewModel

object AuthViewModelFactories {
    fun login(authRepository: AuthRepository): ViewModelProvider.Factory = viewModelFactory {
        initializer { LoginViewModel(authRepository) }
    }

    fun register(authRepository: AuthRepository): ViewModelProvider.Factory = viewModelFactory {
        initializer { RegisterViewModel(authRepository) }
    }

    fun session(
        authRepository: AuthRepository,
        sessionStorage: SessionStorage,
    ): ViewModelProvider.Factory = viewModelFactory {
        initializer { SessionViewModel(authRepository, sessionStorage) }
    }
}
