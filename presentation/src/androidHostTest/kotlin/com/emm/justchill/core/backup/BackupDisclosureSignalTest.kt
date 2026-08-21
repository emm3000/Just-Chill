package com.emm.justchill.core.backup

import com.emm.domain.auth.AuthRepository
import com.emm.domain.auth.AuthUser
import com.emm.domain.auth.ObserveSessionUseCase
import com.emm.domain.auth.SessionStatus
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BackupDisclosureSignalTest {

    @Test
    fun `a signed out device has no destination to disclose`() {
        assertFalse(disclosureIsPending(SessionStatus.NotAuthenticated, UNDISCLOSED))
    }

    @Test
    fun `a session still resolving has no destination to disclose`() {
        assertFalse(disclosureIsPending(SessionStatus.Initializing, UNDISCLOSED))
    }

    @Test
    fun `an account that acknowledged the destination is not pending`() {
        assertFalse(disclosureIsPending(AUTHENTICATED, DISCLOSED))
    }

    @Test
    fun `an account that never acknowledged the destination is pending`() {
        assertTrue(disclosureIsPending(AUTHENTICATED, UNDISCLOSED))
    }

    @Test
    fun `the signal fires only while the kill switch lets a cycle run`() = runBlocking {
        val signal = BackupDisclosureSignal(
            observeSession = ObserveSessionUseCase(
                mockk<AuthRepository> { every { sessionStatus } returns flowOf(AUTHENTICATED) },
            ),
            backupController = mockk<BackupController> { every { health } returns MutableStateFlow(UNDISCLOSED) },
        )

        assertEquals(SNAPSHOT_BACKUP_ENABLED, signal.isPending.first())
    }

    private companion object {

        val AUTHENTICATED = SessionStatus.Authenticated(AuthUser(userId = "user-1", email = "user@example.com"))

        val UNDISCLOSED: BackupHealth = BackupHealth.None.copy(canUploadToDestination = false)

        val DISCLOSED: BackupHealth = BackupHealth.None.copy(canUploadToDestination = true)
    }
}
