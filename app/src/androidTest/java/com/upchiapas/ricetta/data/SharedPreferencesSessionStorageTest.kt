package com.upchiapas.ricetta.data

import android.content.Context
import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.upchiapas.ricetta.data.session.SharedPreferencesSessionStorage
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SharedPreferencesSessionStorageTest {
    private lateinit var context: Context
    private lateinit var preferences: SharedPreferences
    private lateinit var preferencesName: String

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        preferencesName = "session-test-${UUID.randomUUID()}"
        preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    }

    @After
    fun tearDown() {
        context.deleteSharedPreferences(preferencesName)
    }

    @Test
    fun emptyStorageHasNoSession() = runBlocking {
        val storage = SharedPreferencesSessionStorage(preferences)

        assertNull(storage.getToken())
        assertFalse(storage.hasSession())
    }

    @Test
    fun savingAndReplacingTokenIsVisibleFromAnotherStorageInstance() = runBlocking {
        val storage = SharedPreferencesSessionStorage(preferences)
        storage.saveToken("test-token-first")

        val reopened = SharedPreferencesSessionStorage(
            context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        )
        assertTrue(reopened.hasSession())
        assertEquals("test-token-first", reopened.getToken())

        reopened.saveToken("test-token-replacement")
        val reopenedAgain = SharedPreferencesSessionStorage(
            context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        )
        assertEquals("test-token-replacement", reopenedAgain.getToken())
        assertTrue(reopenedAgain.hasSession())
    }

    @Test
    fun clearingTokenRemovesPersistedSession() = runBlocking {
        val storage = SharedPreferencesSessionStorage(preferences)
        storage.saveToken("test-token-to-delete")
        storage.clearToken()

        val reopened = SharedPreferencesSessionStorage(
            context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        )
        assertNull(reopened.getToken())
        assertFalse(reopened.hasSession())
    }
}
