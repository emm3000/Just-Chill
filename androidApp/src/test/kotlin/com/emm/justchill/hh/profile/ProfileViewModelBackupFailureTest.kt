package com.emm.justchill.hh.profile

import com.emm.domain.account.AccountRepository
import com.emm.domain.auth.AuthUser
import com.emm.domain.auth.DeleteUserAccountUseCase
import com.emm.domain.auth.ObserveSessionUseCase
import com.emm.domain.auth.SessionStatus
import com.emm.domain.auth.SignOutUseCase
import com.emm.domain.category.CategoryRepository
import com.emm.domain.shared.backup.BackupFailureReason
import com.emm.domain.shared.backup.BackupFailureState
import com.emm.domain.shared.backup.BackupMetadataStore
import com.emm.domain.shared.backup.BackupPruneReport
import com.emm.domain.shared.backup.BackupPruner
import com.emm.domain.shared.backup.BackupRepository
import com.emm.domain.shared.backup.BackupUploader
import com.emm.domain.shared.backup.BackupVerifier
import com.emm.domain.shared.backup.GetBackupStalenessUseCase
import com.emm.domain.shared.backup.ImportDataUseCase
import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.error.ValidationCode
import com.emm.domain.shared.logging.DiagnosticsLogger
import com.emm.domain.sync.SyncMutex
import com.emm.justchill.MainDispatcherRule
import com.emm.justchill.core.backup.BackupOrchestrator
import com.emm.justchill.hh.shared.toText
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Every case here taps Respaldar on a real [BackupOrchestrator] wired to a real [ProfileViewModel],
 * fails the port that raises that exception in production, and reads the sentence off the effect.
 * Rendering [ProfileMessage.BackupFailed] straight from a reason would prove the copy exists and
 * nothing about whether a failing backup can reach it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelBackupFailureTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val backupRepository = mockk<BackupRepository>(relaxed = true)
    private val uploader = mockk<BackupUploader>(relaxed = true)
    private val pruner = mockk<BackupPruner>(relaxed = true)
    private val metadata = mockk<BackupMetadataStore>(relaxed = true)
    private val logger = mockk<DiagnosticsLogger>(relaxed = true)
    private val syncMutex = SyncMutex()

    private val sessionFlow = MutableStateFlow<SessionStatus>(SessionStatus.Initializing)
    private val observeSession = mockk<ObserveSessionUseCase>()
    private val backgroundFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
    private val resumeFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 8)

    private val categoryRepository = mockk<CategoryRepository> {
        every { all() } returns flowOf(emptyList())
    }
    private val accountRepository = mockk<AccountRepository> {
        every { all() } returns flowOf(emptyList())
    }

    @Before
    fun setUp() {
        every { observeSession.invoke() } returns sessionFlow
        coEvery { backupRepository.exportToJson(any(), any()) } returns PAYLOAD
        coEvery { pruner.prune() } returns BackupPruneReport(kept = 1, deleted = 0, failedDeletes = emptyList())
        every { metadata.lastSuccessfulBackupAt(any()) } returns null
        every { metadata.destinationDisclosedAt(any()) } returns DISCLOSED_AT
        every { metadata.failureState(any()) } returns BackupFailureState.None
        every { metadata.recordFailure(any(), any()) } answers { BackupFailureState(1, secondArg()) }
    }

    @Test
    fun `an export the serializer could not encode does not blame the user`() = runTest(testDispatcher) {
        coEvery { backupRepository.exportToJson(any(), any()) } throws
            DomainException.SerializationError(RuntimeException("bad json"))

        assertEquals(
            "No pude armar el archivo del respaldo — es una falla de la app, no tuya.",
            backUpNow(),
            "a SerializationError must reach the user as ${BackupFailureReason.Serialization}",
        )
    }

    @Test
    fun `an upload that never left the phone tells the user to check the connection`() = runTest(testDispatcher) {
        coEvery { uploader.upload(any(), any(), any()) } throws
            DomainException.NetworkUnavailable(RuntimeException("connect timed out"))

        assertEquals(
            "No llegué a la nube — revisa tu conexión e intenta de nuevo.",
            backUpNow(),
            "a NetworkUnavailable must reach the user as ${BackupFailureReason.Network}",
        )
    }

    @Test
    fun `a snapshot the server refused is not the user's to fix`() = runTest(testDispatcher) {
        coEvery { uploader.upload(any(), any(), any()) } throws DomainException.RemoteRejected(
            "Snapshot backup failed: the payload could not be uploaded. The server answered HTTP 413.",
            statusCode = 413,
            cause = RuntimeException("rest"),
        )

        assertEquals(
            "El servidor rechazó tu respaldo — no depende de ti, lo reintento más tarde.",
            backUpNow(),
            "a RemoteRejected must reach the user as ${BackupFailureReason.RemoteRejected}",
        )
    }

    @Test
    fun `a snapshot whose owner no longer holds the session asks for a new sign-in`() = runTest(testDispatcher) {
        coEvery { uploader.upload(any(), any(), any()) } throws DomainException.Unauthorized(
            "Snapshot backup failed: the signed-in account changed while the snapshot was being taken.",
        )

        assertEquals(
            "Tu sesión ya no vale para respaldar — vuelve a iniciar sesión.",
            backUpNow(),
            "an Unauthorized must reach the user as ${BackupFailureReason.Unauthorized}",
        )
    }

    @Test
    fun `a lock another operation holds says wait, not try again`() = runTest(testDispatcher) {
        val holder = CompletableDeferred<Unit>()
        launch { syncMutex.withLock { holder.await() } }

        val shown: String = backUpNow()
        holder.complete(Unit)

        assertEquals(
            "Hay otra operación en curso — espera, el respaldo se reintenta solo.",
            shown,
            "the SyncMutex acquire timeout must reach the user as ${BackupFailureReason.Busy}",
        )
    }

    @Test
    fun `a payload that did not read back as uploaded says it was discarded`() = runTest(testDispatcher) {
        coEvery { uploader.upload(any(), any(), any()) } throws DomainException.ValidationError(
            "Snapshot backup failed: the payload read back does not match the digest its manifest states.",
            ValidationCode.BackupUploadUnverified,
        )

        assertEquals(
            "El respaldo no coincidió al verificarlo y lo descarté — lo reintento solo.",
            backUpNow(),
            "a BackupUploadUnverified must reach the user as ${BackupFailureReason.Unverified}",
        )
    }

    @Test
    fun `a ledger the device could not read names the phone, not the network`() = runTest(testDispatcher) {
        coEvery { backupRepository.exportToJson(any(), any()) } throws
            DomainException.DatabaseError(RuntimeException("disk I O error"))

        assertEquals(
            "No pude leer tus datos de este teléfono para armar el respaldo.",
            backUpNow(),
            "a DatabaseError must reach the user as ${BackupFailureReason.LocalDatabase}",
        )
    }

    @Test
    fun `a failure nothing classified still says a backup failed`() = runTest(testDispatcher) {
        coEvery { uploader.upload(any(), any(), any()) } throws IllegalStateException("nobody saw this coming")

        assertEquals(
            "El respaldo falló por algo inesperado — no es algo que hayas hecho mal.",
            backUpNow(),
            "an exception the orchestrator had to wrap must reach the user as ${BackupFailureReason.Unknown}",
        )
    }

    private suspend fun TestScope.backUpNow(): String {
        val orchestrator = buildOrchestrator()
        val vm = buildViewModel(orchestrator)
        orchestrator.start()
        sessionFlow.emit(SessionStatus.Authenticated(AuthUser(userId = USER_ID, email = "a@b.com")))
        advanceUntilIdle()

        val messages = mutableListOf<ProfileMessage>()
        val job = launch {
            vm.effect.collect { effect -> if (effect is ProfileEffect.Notify) messages.add(effect.message) }
        }
        advanceUntilIdle()

        vm.onIntent(ProfileIntent.BackUpNow)
        advanceUntilIdle()
        job.cancel()

        return messages.single().toText()
    }

    private fun TestScope.buildOrchestrator(): BackupOrchestrator = BackupOrchestrator(
        backupRepository = backupRepository,
        uploader = uploader,
        pruner = pruner,
        metadata = metadata,
        syncMutex = syncMutex,
        observeSession = observeSession,
        appVersion = APP_VERSION,
        clock = object : Clock {
            override fun now(): Instant = NOW
        },
        timeZone = TimeZone.UTC,
        externalScope = CoroutineScope(StandardTestDispatcher(testScheduler) + SupervisorJob()),
        backgroundEvents = backgroundFlow,
        resumeEvents = resumeFlow,
        logger = logger,
    )

    private fun buildViewModel(orchestrator: BackupOrchestrator): ProfileViewModel = ProfileViewModel(
        backupRepository = backupRepository,
        importData = mockk<ImportDataUseCase>(relaxed = true),
        signOut = mockk<SignOutUseCase>(relaxed = true),
        deleteUserAccount = mockk<DeleteUserAccountUseCase>(relaxed = true),
        backupController = orchestrator,
        backupVerifier = mockk<BackupVerifier>(relaxed = true),
        getBackupStaleness = mockk<GetBackupStalenessUseCase>(relaxed = true),
        logger = logger,
        categoryRepository = categoryRepository,
        accountRepository = accountRepository,
        observeSession = observeSession,
        appVersion = APP_VERSION,
        clock = object : Clock {
            override fun now(): Instant = NOW
        },
    )
}

private const val USER_ID = "user-1"
private const val APP_VERSION = "2.4.0"
private const val PAYLOAD = """{"schemaVersion":3}"""
private const val DISCLOSED_AT = 1_755_000_000_000L
private val NOW = Instant.parse("2026-08-16T10:00:00Z")
