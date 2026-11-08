package com.upchiapas.ricetta.core.di

import android.content.Context
import com.upchiapas.ricetta.BuildConfig
import com.upchiapas.ricetta.core.network.AuthInterceptor
import com.upchiapas.ricetta.data.remote.AuthApi
import com.upchiapas.ricetta.data.remote.RecetasApi
import com.upchiapas.ricetta.data.repository.AuthRepositoryImpl
import com.upchiapas.ricetta.data.repository.RecipeRepositoryImpl
import com.upchiapas.ricetta.data.session.SharedPreferencesSessionStorage
import com.upchiapas.ricetta.domain.repository.AuthRepository
import com.upchiapas.ricetta.domain.repository.RecipeRepository
import com.upchiapas.ricetta.domain.session.SessionStorage
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class AppContainer(context: Context, private val baseUrl: String = BuildConfig.BASE_URL) {
    private val applicationContext = context.applicationContext

    val sessionStorage: SessionStorage by lazy {
        SharedPreferencesSessionStorage(applicationContext.getSharedPreferences(
            SharedPreferencesSessionStorage.PREFERENCES_NAME, Context.MODE_PRIVATE
        ))
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(sessionStorage))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        check(baseUrl.isNotBlank()) { "Configura ricetta.releaseBaseUrl antes de usar la API en release" }
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(Json { ignoreUnknownKeys = true }
                .asConverterFactory("application/json".toMediaType()))
            .build()
    }

    val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(retrofit.create(AuthApi::class.java), sessionStorage)
    }

    val recipeRepository: RecipeRepository by lazy {
        RecipeRepositoryImpl(retrofit.create(RecetasApi::class.java))
    }
}
