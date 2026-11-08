package com.upchiapas.ricetta.presentation.auth

import com.upchiapas.ricetta.domain.result.DataError

fun DataError.authMessage(): String = when (this) {
    DataError.Unauthorized -> "El correo o la contraseña son incorrectos."
    DataError.Conflict -> "Este correo ya está registrado"
    DataError.Validation -> "Revisa los datos e inténtalo nuevamente."
    DataError.NoInternet -> "No pudimos conectarnos. Revisa tu conexión e inténtalo nuevamente."
    DataError.Timeout -> "La solicitud tardó demasiado. Inténtalo nuevamente."
    DataError.Server -> "El servicio no está disponible. Inténtalo más tarde."
    DataError.Storage -> "No pudimos guardar o eliminar la sesión. Inténtalo nuevamente."
    DataError.Serialization -> "No pudimos procesar la respuesta. Inténtalo nuevamente."
    DataError.NotFound, DataError.Unknown -> "No pudimos completar la operación. Inténtalo nuevamente."
}
