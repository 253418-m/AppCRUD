package com.upchiapas.ricetta.domain.result

enum class DataError {
    NoInternet,
    Timeout,
    Unauthorized,
    NotFound,
    Conflict,
    Validation,
    Server,
    Serialization,
    Storage,
    Unknown
}
