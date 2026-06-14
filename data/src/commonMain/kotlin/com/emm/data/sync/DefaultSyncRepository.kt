package com.emm.data.sync

import com.emm.domain.auth.ObserveSessionUseCase
import com.emm.domain.auth.SessionStatus
import com.emm.domain.shared.error.DomainException
import com.emm.domain.sync.ConflictResolver
import com.emm.domain.sync.SyncCursorStore
import com.emm.domain.sync.SyncRepository
import io.github.jan.supabase.auth.exception.SessionRequiredException
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.exceptions.UnauthorizedRestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.Instant

private const val SESSION_RESOLVE_TIMEOUT_MS = 10_000L

/**
 * Orchestrates push+pull sync across all four tables.
 *
 * Pull order is FK-safe: accounts → categories → transactions → recurring_movements.
 * LocalDataSource upserts hit FK constraints (transactions.accountId, etc.) so parents
 * must be pulled before children.
 *
 * Cursor strategy (ADR 002):
 * - One shared cursor per user, stored as ISO-8601 UTC string in [cursorStore].
 * - Pull with overlap window (cursor - 10s) to cover commit-ordering races.
 * - Advance cursor to the max server_updated_at seen across all four pulls.
 * - Empty/lost cursor → full re-pull (safe and idempotent).
 *
 * At-least-once / cursor-hold semantics:
 * - Pulls are idempotent LWW merges, so re-pulling the same window is always safe.
 * - When a table skips rows this cycle (e.g. an orphan child row whose parent has not arrived
 *   yet), the global cursor is NOT advanced. Holding the cursor guarantees the skipped rows are
 *   re-pulled next cycle and applied once their parent lands — re-applying already-merged rows is
 *   a no-op under LWW.
 */
class DefaultSyncRepository(
    private val observeSession: ObserveSessionUseCase,
    private val cursorStore: SyncCursorStore,
    private val conflictResolver: ConflictResolver,
    private val accountSync: TableSync,
    private val categorySync: TableSync,
    private val transactionSync: TableSync,
    private val recurringSync: TableSync,
) : SyncRepository {

    override fun observePendingCount(): Flow<Long> = combine(
        accountSync.pendingCount(),
        categorySync.pendingCount(),
        transactionSync.pendingCount(),
        recurringSync.pendingCount(),
    ) { acc, cat, txn, rec -> acc + cat + txn + rec }

    // Intentional broad catch: maps remote/db throwables to DomainException.
    // ThrowsCount: three distinct re-throw paths are required — CancellationException must
    // not be swallowed, DomainException must pass through, Throwable must be mapped.
    @Suppress("TooGenericExceptionCaught", "ThrowsCount")
    override suspend fun sync() {
        val userId = currentUserId() ?: return // not authenticated — no-op

        try {
            push(userId)
            pull(userId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: DomainException) {
            throw e
        } catch (e: Throwable) {
            throw e.toSyncDomainException()
        }
    }

    private suspend fun push(userId: String) {
        accountSync.push(userId)
        categorySync.push(userId)
        transactionSync.push(userId)
        recurringSync.push(userId)
    }

    private suspend fun pull(userId: String) {
        val cursor = cursorStore.lastPulledAt(userId)

        // FK-safe pull order: accounts and categories before their child tables.
        val accountResult = accountSync.pull(userId, cursor, conflictResolver)
        val categoryResult = categorySync.pull(userId, cursor, conflictResolver)
        val transactionResult = transactionSync.pull(userId, cursor, conflictResolver)
        val recurringResult = recurringSync.pull(userId, cursor, conflictResolver)

        val results = listOf(accountResult, categoryResult, transactionResult, recurringResult)

        // If any table skipped rows (e.g. orphan child rows), hold the cursor so the skipped
        // window is re-pulled next cycle once parents arrive. LWW makes re-pulls idempotent.
        if (results.any { it.skippedRows }) return

        // All per-table returns are already canonical 'Z' strings (Instant.toString()) but may have
        // varying fractional-second precision, so compare as Instant — not as String.
        val globalMax = results
            .mapNotNull { it.maxServerUpdatedAt }
            .maxOfOrNull { Instant.parse(it) }
            ?.toString()

        if (globalMax != null) {
            cursorStore.setLastPulledAt(userId, globalMax)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun currentUserId(): String? = try {
        // Bound the wait for session resolution: an unresolved session must not block the
        // mutex held by SyncDataUseCase forever. On timeout, surface as a network error.
        val status = withTimeout(SESSION_RESOLVE_TIMEOUT_MS) {
            // Block until the auth provider has finished loading any persisted session BEFORE
            // resolving the user. Postgrest reads the request JWT synchronously from the auth
            // session StateFlow; on Kotlin/Native that StateFlow is populated asynchronously after
            // the client is built, so a push fired before it settles goes out without a token and
            // is silently downgraded to the anon key (HTTP 403 under RLS). Awaiting here closes that
            // window. On JVM the session is already settled, so this is a no-op.
            observeSession.awaitInitialization()
            observeSession().first { it !is SessionStatus.Initializing }
        }
        when (status) {
            is SessionStatus.Authenticated -> status.user.userId
            else -> null
        }
    } catch (e: TimeoutCancellationException) {
        throw DomainException.NetworkUnavailable(e)
    }
}

// ---------------------------------------------------------------------------
// Error mapping
// ---------------------------------------------------------------------------

/**
 * Maps supabase / ktor throwables to [DomainException]. Mirrors the style in
 * [com.emm.data.auth.DefaultAuthRepository.toAuthDomainException].
 */
fun Throwable.toSyncDomainException(): DomainException = when (this) {
    is UnauthorizedRestException -> DomainException.Unauthorized(
        message = description ?: "Unauthorized",
        cause = this,
    )

    // Postgrest has requireValidSession = true: when the auth session is not yet resolved, the
    // request throws SessionRequiredException instead of silently downgrading to the anon key.
    // This is a transient "session not ready" condition, not a revoked session — map it to a
    // retryable network error (NOT Unauthorized, which would sign the user out). The
    // awaitSessionInitialization() gate in currentUserId() makes this essentially unreachable; it
    // remains as a safety net so the failure stays loud and retryable rather than a confusing 403.
    is SessionRequiredException -> DomainException.NetworkUnavailable(this)

    is RestException -> DomainException.Unknown(this)

    is HttpRequestException,
    is HttpRequestTimeoutException,
    -> DomainException.NetworkUnavailable(this)

    else -> DomainException.Unknown(this)
}
