package com.emm.data.auth

import com.emm.data.shared.ioDispatcher
import com.emm.domain.auth.AuthRepository
import com.emm.domain.auth.AuthUser
import com.emm.domain.auth.SessionStatus
import com.emm.domain.auth.SignOutResult
import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.error.ValidationCode
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.SignOutScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.exception.SessionRequiredException
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.exceptions.UnauthorizedRestException
import io.github.jan.supabase.postgrest.postgrest
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import io.github.jan.supabase.auth.status.SessionStatus as SupabaseSessionStatus

class DefaultAuthRepository(private val client: SupabaseClient) : AuthRepository {

    override val sessionStatus: Flow<SessionStatus>
        get() = client.auth.sessionStatus
            .map { it.toDomain() }
            .flowOn(ioDispatcher)

    /**
     * Delegates to supabase-kt's [io.github.jan.supabase.auth.Auth.awaitInitialization], which
     * suspends until [io.github.jan.supabase.auth.Auth.sessionStatus] leaves
     * [io.github.jan.supabase.auth.status.SessionStatus.Initializing].
     *
     * Postgrest resolves the request JWT synchronously from `auth.sessionStatus.value`. On
     * Kotlin/Native the session is loaded asynchronously after the client is built, so a push fired
     * before that read settles attaches no token and the request is silently downgraded to the anon
     * key (HTTP 403 under RLS `with check (user_id = auth.uid())`). Awaiting initialization on the
     * same StateFlow the resolver reads closes that gap. On JVM the session is already settled after
     * sign-in, so this returns immediately.
     */
    override suspend fun awaitSessionInitialization(): Unit = withContext(ioDispatcher) {
        client.auth.awaitInitialization()
    }

    override suspend fun signIn(email: String, password: String): AuthUser = authCall {
        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        // signInWith does not return the user directly; read it from the live session.
        val user = client.auth.currentUserOrNull()
            ?: throw DomainException.Unauthorized("Sign-in succeeded but no session was established")
        user.toDomain()
    }

    override suspend fun signUp(email: String, password: String): AuthUser? = authCall {
        // signUpWith(Email) returns UserInfo? — non-null means the server established a session
        // immediately (auto-confirm on); null means email confirmation is pending.
        // We ignore the return value and derive the result from the live session instead, which
        // is the same information but guaranteed to be consistent with sessionStatus.
        client.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
        // If a session exists the user was auto-confirmed; otherwise confirmation is pending.
        client.auth.currentUserOrNull()?.toDomain()
    }

    override suspend fun signInWithGoogle(idToken: String, rawNonce: String): AuthUser = authCall {
        client.auth.signInWith(IDToken) {
            this.idToken = idToken
            provider = Google
            nonce = rawNonce
        }
        val user = client.auth.currentUserOrNull()
            ?: throw DomainException.Unauthorized("Sign-in succeeded but no session was established")
        user.toDomain()
    }

    /**
     * Two steps with deliberately different failure contracts — see [AuthRepository.signOut] for
     * why. Neither step is a guarantee; what differs is which failure the caller hears about.
     *
     * The server-side revoke ([SignOutScope.LOCAL] — it still narrows which sessions the server
     * invalidates even though it does not shield the caller from a network failure) is attempted
     * and any failure of it is swallowed, then [io.github.jan.supabase.auth.Auth.clearSession] runs
     * UNCONDITIONALLY. That call is purely local: it deletes the code verifier, deletes the stored
     * session, stops the auto-refresh job for the current session, and sets the status to
     * NotAuthenticated. Nothing in it can fail on network.
     *
     * [io.github.jan.supabase.auth.Auth.clearSession] is deliberately OUTSIDE the swallow: if the
     * session store itself breaks, the caller has to hear about it (it still propagates through
     * [authCall]), because the user is still signed in.
     *
     * Calling `clearSession` again after a successful `signOut` re-runs all of the above — it is
     * harmless, not literally a no-op. Nothing observes the duplicate: `SessionStatus.NotAuthenticated`
     * is a data class and the session status is a `MutableStateFlow`, which conflates equal values,
     * so setting it to an already-equal `NotAuthenticated` a second time does not re-emit to
     * observers. The happy path pays nothing extra for this shape only because of that conflation.
     */
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    override suspend fun signOut(): SignOutResult = authCall {
        val revoked = try {
            client.auth.signOut(SignOutScope.LOCAL)
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Swallowed on purpose — see the KDoc above. The local clear below must run whether or
            // not the server-side revoke was reachable, which is why it sits outside this try.
            false
        }
        client.auth.clearSession()
        if (revoked) SignOutResult.Revoked else SignOutResult.LocalOnly
    }

    override suspend fun resendConfirmationEmail(email: String): Unit = authCall {
        client.auth.resendEmail(OtpType.Email.SIGNUP, email)
    }

    /**
     * Calls the Supabase `delete_account` RPC to remove the auth user and all remote rows, then
     * ATTEMPTS to clear the on-device session via [SignOutScope.LOCAL]. The clear is not guaranteed
     * here the way it is in [signOut] — see below.
     *
     * [SignOutScope.LOCAL] is intentional: it is the narrowest scope, and the auth user no longer
     * exists on the server after the RPC. What it does NOT do is make the call local. Exactly as
     * documented on [signOut], supabase-kt 3.7.0's `AuthImpl.signOut` posts `logout` whenever a
     * session exists — every scope, `LOCAL` included — and catches only
     * [io.github.jan.supabase.exceptions.RestException]. The deleted user's JWT answers 401/403/404,
     * which sits in the provider's `SIGN_OUT_IGNORE_CODES`, so the ordinary case does reach
     * `clearSession()`. A network failure does not: [HttpRequestException] is an `IOException`, not
     * a `RestException`, so it escapes the provider before the clear runs and [authCall] maps it to
     * [DomainException.NetworkUnavailable] — leaving the device holding a session for a user that no
     * longer exists server-side.
     *
     * Deliberately NOT repaired with [signOut]'s swallow-then-clear shape: whether a delete whose
     * remote half already succeeded should still report a qualified success is its own contract
     * question, deferred to `docs/work/epics/E01-snapshot-backup.md`. And unlike [signOut], no test
     * covers this call shape — `DefaultAuthRepositorySignOutTest` exercises [signOut] only, so the
     * defect above is read off the provider's source rather than off a red test.
     */
    override suspend fun deleteAccount(): Unit = authCall {
        client.postgrest.rpc("delete_account")
        client.auth.signOut(SignOutScope.LOCAL)
    }

    // ---------------------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------------------

    // Single funnel for supabase-kt/ktor throwables → DomainException (see toAuthDomainException).
    @Suppress("TooGenericExceptionCaught")
    private suspend inline fun <T> authCall(crossinline block: suspend () -> T): T = withContext(ioDispatcher) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: DomainException) {
            throw e
        } catch (e: Throwable) {
            throw e.toAuthDomainException()
        }
    }
}

// ---------------------------------------------------------------------------
// Mappers
// ---------------------------------------------------------------------------

/**
 * Maps supabase [SupabaseSessionStatus] to the domain [SessionStatus].
 *
 * [SupabaseSessionStatus.RefreshFailure] decision (settled — the slice-4 TODO that used to sit
 * here is resolved by keeping this mapping):
 * RefreshFailure is not Authenticated — the library emits it while the session may still be
 * structurally present in memory yet not safely usable (the token could be expired). Mapping it
 * to Authenticated would let sync fire with a token the server may reject; NotAuthenticated is
 * the honest state for everything this flow gates (sync triggers, the claim observer, the Perfil
 * session row). The cost is cosmetic and transient: while a refresh keeps failing the UI shows
 * signed-out, and the status flips back by itself if the provider later refreshes successfully.
 * No local data is touched either way — the app is local-first and fully usable, and the user can
 * always re-sign-in manually.
 *
 * A distinct "degraded" domain status (offline reads allowed, sync blocked) was considered and
 * rejected: sync is the only authenticated feature, so a third status would gate nothing that
 * NotAuthenticated does not already gate, at the price of a fourth branch in every consumer.
 */
internal fun SupabaseSessionStatus.toDomain(): SessionStatus = when (this) {
    is SupabaseSessionStatus.Authenticated -> {
        val user = session.user
        if (user != null) {
            SessionStatus.Authenticated(AuthUser(userId = user.id, email = user.email))
        } else {
            // Session token exists but user payload is null (e.g. imported token without retrieveUser).
            // Treat as not-authenticated to avoid a null AuthUser propagating to the UI.
            SessionStatus.NotAuthenticated
        }
    }

    is SupabaseSessionStatus.Initializing -> SessionStatus.Initializing

    is SupabaseSessionStatus.NotAuthenticated -> SessionStatus.NotAuthenticated

    is SupabaseSessionStatus.RefreshFailure -> SessionStatus.NotAuthenticated
}

internal fun UserInfo.toDomain(): AuthUser = AuthUser(userId = id, email = email)

/**
 * Auth error codes that represent invalid user input (not a failed authentication), so they map to
 * [DomainException.ValidationError] instead of [DomainException.Unauthorized]. Relevant mostly on
 * sign-up, where "wrong credentials" would be a nonsensical message.
 */
private val VALIDATION_AUTH_CODES: Map<AuthErrorCode, ValidationCode> = mapOf(
    AuthErrorCode.WeakPassword to ValidationCode.PasswordTooWeak,
    AuthErrorCode.EmailExists to ValidationCode.EmailAlreadyRegistered,
    AuthErrorCode.UserAlreadyExists to ValidationCode.EmailAlreadyRegistered,
    AuthErrorCode.EmailAddressInvalid to ValidationCode.EmailInvalid,
    AuthErrorCode.ValidationFailed to ValidationCode.Unspecified,
    AuthErrorCode.SamePassword to ValidationCode.PasswordUnchanged,
)

/**
 * Translates a supabase / ktor throwable into the appropriate [DomainException] subtype.
 *
 * Exception hierarchy verified against supabase-kt 3.6.0 sources:
 * - [AuthRestException] extends [io.github.jan.supabase.exceptions.RestException] — auth-specific 4xx/5xx responses.
 *   Credential/authentication errors: [io.github.jan.supabase.auth.exception.AuthErrorCode]
 *   values UserNotFound, SessionNotFound, NoAuthorization, EmailNotConfirmed, etc.
 * - [UnauthorizedRestException] extends [io.github.jan.supabase.exceptions.RestException] — HTTP 401.
 * - [io.github.jan.supabase.exceptions.RestException] — any other structured server error, mapped to
 *   [DomainException.RemoteRejected] carrying `statusCode`, the same way
 *   `BackupFailures.asBackupFailure` maps the identical family on the backup path.
 * - [HttpRequestException] extends IOException — network-level failure.
 * - [HttpRequestTimeoutException] (ktor) — request timed out (network category).
 *
 * [AuthRestException] and [UnauthorizedRestException] are siblings (both extend [RestException]
 * directly), so their relative order is irrelevant — the branches are disjoint types. The plain
 * [RestException] branch below must still sit after both, or it would catch their instances first;
 * [SessionRequiredException], despite the name, does NOT extend [RestException] — it extends
 * `Exception` directly — so it never competes with that branch, but it stays ahead of it anyway to
 * read the same top-to-bottom order as `BackupFailures.asBackupFailure`.
 *
 * Sign-up/credential rejections that are the user's fault (weak password, email already taken,
 * invalid email) carry an [AuthErrorCode] in [VALIDATION_AUTH_CODES] and map to [ValidationError]
 * so the UI shows a corrective hint instead of the misleading "wrong credentials" message. The
 * server's `errorDescription` is English, so the [ValidationCode] — not the message — is what the
 * UI translates.
 *
 * [SessionRequiredException] maps to [DomainException.Unauthorized] here, which diverges from
 * `toSyncDomainException` in `DefaultSyncRepository`, where the same exception maps to
 * [DomainException.NetworkUnavailable] because it can only mean "session not ready yet" on that
 * path. The delete path does gate on the session leaving [com.emm.domain.auth.SessionStatus.Initializing]
 * before this call (`resolveAuthenticatedUserId` in `DeleteUserAccountUseCase`), but it does not
 * additionally call `observeSession.awaitInitialization()` the way `currentUserId` in
 * `DefaultSyncRepository` does — the guard against the Kotlin/Native race documented there, where
 * postgrest reads the JWT synchronously from a session `StateFlow` that is populated asynchronously.
 * That race is still open on the delete path, so this mapping can surface it as a credentials error
 * instead of something retryable. Tracked as a follow-up in `docs/PROGRESS.md`; the sync mapper is
 * not changed by this commit.
 */
internal fun Throwable.toAuthDomainException(): DomainException = when (this) {
    is AuthRestException -> VALIDATION_AUTH_CODES[errorCode]?.let { validationCode ->
        DomainException.ValidationError(
            message = errorDescription,
            code = validationCode,
            cause = this,
        )
    } ?: DomainException.Unauthorized(
        message = "Authentication error: $errorDescription",
        cause = this,
    )

    is UnauthorizedRestException -> DomainException.Unauthorized(
        message = description ?: "Unauthorized",
        cause = this,
    )

    is SessionRequiredException -> DomainException.Unauthorized(
        message = "Postgrest call required a session but none was attached",
        cause = this,
    )

    is RestException -> DomainException.RemoteRejected(
        message = "The auth server rejected the request: HTTP $statusCode: $error.",
        statusCode = statusCode,
        cause = this,
    )

    is HttpRequestException,
    is HttpRequestTimeoutException,
    -> DomainException.NetworkUnavailable(this)

    else -> DomainException.Unknown(this)
}
