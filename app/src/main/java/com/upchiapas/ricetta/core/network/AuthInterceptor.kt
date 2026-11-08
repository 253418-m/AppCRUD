package com.upchiapas.ricetta.core.network

import com.upchiapas.ricetta.domain.session.SessionStorage
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val sessionStorage: SessionStorage) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val segments = request.url.pathSegments.filter { it.isNotEmpty() }
        val isProtected = segments.lastOrNull() == "recetas" ||
            segments.dropLast(1).lastOrNull() == "recetas"
        val builder = request.newBuilder().removeHeader("Authorization")
        if (isProtected) {
            sessionStorage.getToken()?.takeIf { it.isNotBlank() }?.let {
                builder.header("Authorization", "Bearer $it")
            }
        }
        return chain.proceed(builder.build())
    }
}
