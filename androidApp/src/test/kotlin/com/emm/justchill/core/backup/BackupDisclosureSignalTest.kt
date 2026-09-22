package com.emm.justchill.core.backup

import com.emm.justchill.core.domain.auth.AuthRepository
import com.emm.justchill.core.domain.auth.AuthUser
import com.emm.justchill.core.domain.auth.GetSessionStatusUseCase
import com.emm.justchill.core.domain.auth.SessionStatus
import com.emm.justchill.core.domain.shared.backup.BackupController
import com.emm.justchill.core.domain.shared.backup.BackupHealth
import com.emm.justchill.core.testing.FakeBackupAvailability
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Test
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
    fun `an available backup announces an undisclosed destination`() = runBlocking {
        val signal: BackupDisclosureSignal = signedInUndisclosedSignal(isAvailable = true)

        assertTrue(signal.isPending.first())
    }

    @Test
    fun `an unavailable backup never announces a destination it cannot upload to`() = runBlocking {
        val signal: BackupDisclosureSignal = signedInUndisclosedSignal(isAvailable = false)

        assertFalse(signal.isPending.first())
    }

    private fun signedInUndisclosedSignal(isAvailable: Boolean): BackupDisclosureSignal =
        BackupDisclosureSignal(
            getSessionStatus = GetSessionStatusUseCase(
                mockk<AuthRepository> { every { sessionStatus } returns flowOf(AUTHENTICATED) },
            ),
            backupController = mockk<BackupController> { every { health } returns MutableStateFlow(UNDISCLOSED) },
            backupAvailability = FakeBackupAvailability(isAvailable),
        )

    private companion object {

        val AUTHENTICATED = SessionStatus.Authenticated(AuthUser(userId = "user-1", email = "user@example.com"))

        val UNDISCLOSED: BackupHealth = BackupHealth.None.copy(canUploadToDestination = false)

        val DISCLOSED: BackupHealth = BackupHealth.None.copy(canUploadToDestination = true)    }
}
