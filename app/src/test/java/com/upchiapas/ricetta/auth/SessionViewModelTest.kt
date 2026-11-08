package com.upchiapas.ricetta.auth

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.upchiapas.ricetta.domain.result.AppResult
import com.upchiapas.ricetta.domain.result.DataError
import com.upchiapas.ricetta.domain.session.SessionStorageException
import com.upchiapas.ricetta.presentation.auth.AuthViewModelFactories
import com.upchiapas.ricetta.presentation.session.SessionPhase
import com.upchiapas.ricetta.presentation.session.SessionViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    @Test fun startupWaitsForStorageWithoutFlashingSignedOut() = runTest {
        val result = CompletableDeferred<Boolean>()
        val storage = FakeSessionStorage().apply { check = { result.await() } }
        val vm = SessionViewModel(FakeAuthRepository(), storage)
        assertEquals(SessionPhase.Checking, vm.state.value.phase)
        runCurrent()
        assertEquals(SessionPhase.Checking, vm.state.value.phase)
        assertTrue(vm.state.value.isLoading)
        result.complete(true)
        advanceUntilIdle()
        assertEquals(SessionPhase.SignedIn, vm.state.value.phase)
        assertFalse(vm.state.value.isLoading)
    }

    @Test fun absentSessionOpensSignedOutFlow() = runTest {
        val storage = FakeSessionStorage(false)
        val vm = SessionViewModel(FakeAuthRepository(), storage)
        advanceUntilIdle()
        assertEquals(SessionPhase.SignedOut, vm.state.value.phase)
        assertEquals(1, storage.checkCalls)
    }

    @Test fun storageReadErrorAllowsRetryWithoutClaimingAuthentication() = runTest {
        val storage = FakeSessionStorage().apply { check = { throw SessionStorageException("failed read") } }
        val vm = SessionViewModel(FakeAuthRepository(), storage)
        advanceUntilIdle()
        assertEquals(SessionPhase.Checking, vm.state.value.phase)
        assertNotNull(vm.state.value.errorMessage)
        assertFalse(vm.state.value.isLoading)
        storage.check = { true }
        vm.retryCheckSession()
        advanceUntilIdle()
        assertEquals(SessionPhase.SignedIn, vm.state.value.phase)
        assertNull(vm.state.value.errorMessage)
    }

    @Test fun successfulAuthCanSetSignedInAfterStartup() = runTest {
        val vm = SessionViewModel(FakeAuthRepository(), FakeSessionStorage())
        advanceUntilIdle()
        assertEquals(SessionPhase.SignedOut, vm.state.value.phase)
        vm.onAuthenticated()
        assertEquals(SessionPhase.SignedIn, vm.state.value.phase)
    }

    @Test fun cancellingDialogDoesNotCallLogoutOrDeleteSession() = runTest {
        val repository = FakeAuthRepository()
        val storage = FakeSessionStorage(true)
        val vm = SessionViewModel(repository, storage)
        advanceUntilIdle()
        vm.requestLogout()
        assertTrue(vm.state.value.showLogoutDialog)
        vm.cancelLogout()
        assertFalse(vm.state.value.showLogoutDialog)
        assertEquals(SessionPhase.SignedIn, vm.state.value.phase)
        assertEquals(0, repository.logoutCalls)
        assertTrue(storage.active)
    }

    @Test fun successfulLogoutChangesFlowOnlyAfterRepositoryCompletes() = runTest {
        val response = CompletableDeferred<AppResult<Unit>>()
        val storage = FakeSessionStorage(true)
        val repository = FakeAuthRepository().apply {
            logout = {
                response.await().also { if (it is AppResult.Success) storage.active = false }
            }
        }
        val vm = SessionViewModel(repository, storage)
        advanceUntilIdle()
        vm.requestLogout()
        vm.confirmLogout()
        runCurrent()
        assertTrue(vm.state.value.isLoggingOut)
        assertEquals(SessionPhase.SignedIn, vm.state.value.phase)
        assertTrue(storage.active)
        vm.confirmLogout()
        assertEquals(1, repository.logoutCalls)
        response.complete(AppResult.Success(Unit))
        advanceUntilIdle()
        assertFalse(storage.active)
        assertFalse(vm.state.value.isLoggingOut)
        assertFalse(vm.state.value.showLogoutDialog)
        assertEquals(SessionPhase.SignedOut, vm.state.value.phase)
    }

    @Test fun failedLogoutKeepsAuthenticatedSessionAndOffersRetry() = runTest {
        val storage = FakeSessionStorage(true)
        val repository = FakeAuthRepository().apply { logout = { AppResult.Failure(DataError.Storage) } }
        val vm = SessionViewModel(repository, storage)
        advanceUntilIdle()
        vm.requestLogout()
        vm.confirmLogout()
        advanceUntilIdle()
        assertEquals(SessionPhase.SignedIn, vm.state.value.phase)
        assertTrue(storage.active)
        assertTrue(vm.state.value.showLogoutDialog)
        assertNotNull(vm.state.value.errorMessage)
        assertFalse(vm.state.value.isLoggingOut)
        repository.logout = { storage.active = false; AppResult.Success(Unit) }
        vm.confirmLogout()
        advanceUntilIdle()
        assertEquals(SessionPhase.SignedOut, vm.state.value.phase)
        assertFalse(storage.active)
    }

    @Test fun timeoutDoesNotDeleteTokenOrLeaveAuthenticatedFlow() = runTest {
        val repository = FakeAuthRepository()
        val storage = FakeSessionStorage(true)
        val vm = SessionViewModel(repository, storage)
        advanceUntilIdle()
        vm.onProtectedError(DataError.Timeout)
        advanceUntilIdle()
        assertTrue(storage.active)
        assertEquals(SessionPhase.SignedIn, vm.state.value.phase)
        assertEquals(0, repository.logoutCalls)
        assertNotNull(vm.state.value.errorMessage)
    }

    @Test fun protectedUnauthorizedClearsSessionThroughRepository() = runTest {
        val storage = FakeSessionStorage(true)
        val repository = FakeAuthRepository().apply {
            logout = { storage.active = false; AppResult.Success(Unit) }
        }
        val vm = SessionViewModel(repository, storage)
        advanceUntilIdle()
        vm.onProtectedError(DataError.Unauthorized)
        advanceUntilIdle()
        assertEquals(1, repository.logoutCalls)
        assertFalse(storage.active)
        assertEquals(SessionPhase.SignedOut, vm.state.value.phase)
    }

    @Test fun staleStartupCheckCannotUndoSuccessfulAuthentication() = runTest {
        val result = CompletableDeferred<Boolean>()
        val storage = FakeSessionStorage().apply { check = { result.await() } }
        val vm = SessionViewModel(FakeAuthRepository(), storage)
        runCurrent()
        vm.onAuthenticated()
        result.complete(false)
        advanceUntilIdle()
        assertEquals(SessionPhase.SignedIn, vm.state.value.phase)
        assertFalse(vm.state.value.isLoading)
    }

    @Test fun clearingOwnerCancelsLogoutWithoutClaimingSuccess() = runTest {
        var cancelled = false
        val storage = FakeSessionStorage(true)
        val repository = FakeAuthRepository().apply {
            logout = {
                try { kotlinx.coroutines.awaitCancellation() }
                catch (error: kotlinx.coroutines.CancellationException) { cancelled = true; throw error }
            }
        }
        val store = ViewModelStore()
        val vm = ViewModelProvider(
            store, AuthViewModelFactories.session(repository, storage)
        )[SessionViewModel::class.java]
        advanceUntilIdle()
        vm.requestLogout()
        vm.confirmLogout()
        runCurrent()
        assertTrue(vm.state.value.isLoggingOut)
        store.clear()
        advanceUntilIdle()
        assertTrue(cancelled)
        assertFalse(vm.state.value.isLoggingOut)
        assertEquals(SessionPhase.SignedIn, vm.state.value.phase)
        assertTrue(storage.active)
        assertNull(vm.state.value.errorMessage)
    }

    @Test fun factoryInjectsStorageAndRepositoryAndRetainsSessionState() = runTest {
        val storage = FakeSessionStorage(true)
        val repository = FakeAuthRepository()
        val store = ViewModelStore()
        try {
            val factory = AuthViewModelFactories.session(repository, storage)
            val first = ViewModelProvider(store, factory)[SessionViewModel::class.java]
            advanceUntilIdle()
            first.requestLogout()
            val retained = ViewModelProvider(store, factory)[SessionViewModel::class.java]
            assertSame(first, retained)
            assertTrue(retained.state.value.showLogoutDialog)
            assertEquals(SessionPhase.SignedIn, retained.state.value.phase)
            retained.cancelLogout()
        } finally { store.clear() }
    }
}
