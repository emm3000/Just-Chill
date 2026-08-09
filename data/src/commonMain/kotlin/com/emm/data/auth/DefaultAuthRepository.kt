package com.emm.data.auth

import com.emm.data.shared.ioDispatcher
import com.emm.domain.auth.AuthRepository
import com.emm.domain.auth.AuthUser
import com.emm.domain.auth.SessionStatus
import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.error.ValidationCode
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.SignOutScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.exceptions.HttpRequestException
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

    override suspend fun signOut(): Unit = authCall {
        client.auth.signOut()
    }

    override suspend fun resendConfirmationEmail(email: String): Unit = authCall {
        client.auth.resendEmail(OtpType.Email.SIGNUP, email)
    }

    /**
     * Calls the Supabase `delete_account` RPC to remove the auth user and all remote rows,
     * then clears the on-device session via [SignOutScope.LOCAL].
     *
     * [SignOutScope.LOCAL] is intentional: the auth user no longer exists on the server after
     * the RPC, so a server-side sign-out call would fail. LOCAL clears the on-device session only.
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
 * [SupabaseSessionStatus.RefreshFailure] decision:
 * The supabase status carries the last-known session inside [SessionStatus.Authenticated],
 * but RefreshFailure itself is not Authenticated — the library emits it while the session
 * may still be structurally present in memory yet not safely usable (the token could be
 * expired). Mapping it to domain Authenticated would misrepresent the state; mapping it to
 * NotAuthenticated would force a premature sign-out for a transient network error. We
 * therefore map it to NotAuthenticated as the safer UX choice (the user can re-sign-in).
 *
 * TODO slice 4: revisit RefreshFailure posture — consider a distinct domain status that
 * allows offline-read access while blocking sync operations.
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
 * - [io.github.jan.supabase.exceptions.RestException] — any other structured server error.
 * - [HttpRequestException] extends IOException — network-level failure.
 * - [HttpRequestTimeoutException] (ktor) — request timed out (network category).
 *
 * [AuthRestException] and [UnauthorizedRestException] are siblings (both extend [RestException]
 * directly), so their relative order is irrelevant — the branches are disjoint types.
 *
 * Sign-up/credential rejections that are the user's fault (weak password, email already taken,
 * invalid email) carry an [AuthErrorCode] in [VALIDATION_AUTH_CODES] and map to [ValidationError]
 * so the UI shows a corrective hint instead of the misleading "wrong credentials" message. The
 * server's `errorDescription` is English, so the [ValidationCode] — not the message — is what the
 * UI translates.
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

    is HttpRequestException,
    is HttpRequestTimeoutException,
    -> DomainException.NetworkUnavailable(this)

    else -> DomainException.Unknown(this)
}
