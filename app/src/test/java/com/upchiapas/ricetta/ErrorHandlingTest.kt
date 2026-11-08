package com.upchiapas.ricetta

import com.upchiapas.ricetta.core.network.apiResult
import com.upchiapas.ricetta.domain.result.AppResult
import com.upchiapas.ricetta.domain.result.DataError
import com.upchiapas.ricetta.domain.session.SessionStorageException
import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class ErrorHandlingTest {
    @Test fun successfulValueIsPreserved() = runTest {
        assertEquals(AppResult.Success(7), apiResult { 7 })
    }

    @Test fun httpCodesBecomeDomainErrors() = runTest {
        val cases = mapOf(
            401 to DataError.Unauthorized,
            404 to DataError.NotFound,
            409 to DataError.Conflict,
            422 to DataError.Validation,
            500 to DataError.Server,
            503 to DataError.Server,
            418 to DataError.Unknown
        )
        for ((code, expected) in cases) {
            val response = Response.error<Unit>(code, """{"detail":"internal detail"}""".toResponseBody())
            assertEquals(AppResult.Failure(expected), apiResult<Unit> { throw HttpException(response) })
        }
    }

    @Test fun timeoutIsHandledBeforeGeneralIoError() = runTest {
        assertEquals(AppResult.Failure(DataError.Timeout), apiResult<Unit> {
            throw SocketTimeoutException("not for presentation")
        })
        assertEquals(AppResult.Failure(DataError.Timeout), apiResult<Unit> {
            throw InterruptedIOException("call timeout")
        })
        assertEquals(AppResult.Failure(DataError.NoInternet), apiResult<Unit> {
            throw IOException("not for presentation")
        })
    }

    @Test fun serializationStorageAndUnexpectedErrorsAreTyped() = runTest {
        assertEquals(AppResult.Failure(DataError.Serialization), apiResult<Unit> {
            throw SerializationException("not for presentation")
        })
        assertEquals(AppResult.Failure(DataError.Storage), apiResult<Unit> {
            throw SessionStorageException("not for presentation")
        })
        assertEquals(AppResult.Failure(DataError.Unknown), apiResult<Unit> {
            throw IllegalStateException("not for presentation")
        })
    }

    @Test fun cancellationIsRethrownAsTheSameInstance() = runTest {
        val cancellation = CancellationException("cancelled by caller")
        try {
            apiResult<Unit> { throw cancellation }
            fail("CancellationException must propagate")
        } catch (actual: CancellationException) {
            assertSame(cancellation, actual)
        }
    }
}
