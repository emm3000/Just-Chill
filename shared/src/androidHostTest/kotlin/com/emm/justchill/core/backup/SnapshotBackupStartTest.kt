package com.emm.justchill.core.backup

import com.emm.justchill.core.domain.auth.AuthRepository
import com.emm.justchill.core.domain.auth.GetSessionStatusUseCase
import com.emm.justchill.core.domain.auth.SessionStatus
import com.emm.justchill.core.domain.shared.RemoteWriteMutex
import com.emm.justchill.core.domain.shared.backup.BackupAvailability
import com.emm.justchill.core.testing.FakeBackupAvailability
import com.emm.justchill.core.testing.NoOpDiagnosticsLogger
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import org.junit.Test
import org.koin.core.Koin
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant

class SnapshotBackupStartTest {

    private val sessionStatus: MutableStateFlow<SessionStatus> = MutableStateFlow(SessionStatus.NotAuthenticated)

    @Test
    fun `an unavailable backup starts no cycle`() = runTest {
        val koin: Koin = graph(FakeBackupAvailability(isAvailable = false))

        startSnapshotBackup(koin)
        runCurrent()

        assertEquals(0, sessionStatus.subscriptionCount.value)
        koin.close()
    }

    @Test
    fun `an available backup starts the cycle, which watches the session`() = runTest {
        val koin: Koin = graph(FakeBackupAvailability(isAvailable = true))

        startSnapshotBackup(koin)
        runCurrent()

        assertEquals(1, sessionStatus.subscriptionCount.value)
        koin.close()
    }

    private fun TestScope.graph(availability: BackupAvailability): Koin {
        val authRepository: AuthRepository = mockk {
            every { sessionStatus } returns this@SnapshotBackupStartTest.sessionStatus
        }
        val orchestrator = BackupOrchestrator(
            backupRepository = mockk(),
            uploader = mockk(),
            pruner = mockk(),
            metadata = mockk(),
            remoteWriteMutex = RemoteWriteMutex(),
            getSessionStatus = GetSessionStatusUseCase(authRepository),
            appVersion = "0.0.0-test",
            clock = FixedClock,
            timeZone = TimeZone.UTC,
            externalScope = backgroundScope,
            backgroundEvents = emptyFlow(),
            resumeEvents = emptyFlow(),
            logger = NoOpDiagnosticsLogger(),
        )
        return koinApplication {
            modules(
                module {
                    single<BackupAvailability> { availability }
                    single { orchestrator }
                },
            )
        }.koin
    }

    private object FixedClock : Clock {
        override fun now(): Instant = Instant.parse("2026-09-29T12:00:00Z")
    }
}
