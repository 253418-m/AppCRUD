package com.upchiapas.ricetta.core.network

import com.upchiapas.ricetta.domain.result.AppResult
import com.upchiapas.ricetta.domain.result.DataError
import com.upchiapas.ricetta.domain.session.SessionStorageException
import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

suspend fun <T> apiResult(block: suspend () -> T): AppResult<T> = try {
    AppResult.Success(block())
} catch (error: CancellationException) {
    throw error
} catch (error: HttpException) {
    AppResult.Failure(when (error.code()) {
        401 -> DataError.Unauthorized
        404 -> DataError.NotFound
        409 -> DataError.Conflict
        422 -> DataError.Validation
        in 500..599 -> DataError.Server
        else -> DataError.Unknown
    })
} catch (_: SocketTimeoutException) {
    AppResult.Failure(DataError.Timeout)
} catch (_: InterruptedIOException) {
    AppResult.Failure(DataError.Timeout)
} catch (_: SerializationException) {
    AppResult.Failure(DataError.Serialization)
} catch (_: SessionStorageException) {
    AppResult.Failure(DataError.Storage)
} catch (_: IOException) {
    AppResult.Failure(DataError.NoInternet)
} catch (_: Exception) {
    AppResult.Failure(DataError.Unknown)
}
