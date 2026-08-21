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
     * Postgrest resolves the request JWT synchronously from auth.sessionStatus.value, which on
     * Kotlin/Native is populated asynchronously after the client is built. A call fired before that
     * read settles attaches no token and is silently downgraded to the anon key (HTTP 403 under RLS).
     */
    override suspend fun awaitSessionInitialization(): Unit = withContext(ioDispatcher) {
        client.auth.awaitInitialization()
    }

    override suspend fun signIn(email: String, password: String): AuthUser = authCall {
        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        val user = client.auth.currentUserOrNull()
            ?: throw DomainException.Unauthorized("Sign-in succeeded but no session was established")
        user.toDomain()
    }

    override suspend fun signUp(email: String, password: String): AuthUser? = authCall {
        client.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
        // signUpWith's own UserInfo? carries the same answer, but only the live session is
        // guaranteed consistent with sessionStatus.
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
     * clearSession sits outside the swallow on purpose: it cannot fail on network, so a failure of
     * it means the session store itself broke and the user is still signed in — the caller has to
     * hear that. Running it after a successful revoke is harmless rather than a no-op, and costs
     * no extra emission only because MutableStateFlow conflates the equal NotAuthenticated value.
     */
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    override suspend fun signOut(): SignOutResult = authCall {
        val revoked = try {
            client.auth.signOut(SignOutScope.LOCAL)
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        }
        client.auth.clearSession()
        if (revoked) SignOutResult.Revoked else SignOutResult.LocalOnly
    }

    override suspend fun resendConfirmationEmail(email: String): Unit = authCall {
        client.auth.resendEmail(OtpType.Email.SIGNUP, email)
    }

    /**
     * Fires the only Postgrest call in this class without awaiting initialization, because its only
     * caller already did: DeleteUserAccountUseCase.resolveAuthenticatedUserId waits out Initializing
     * under the same lock and refuses unless the session is Authenticated. Move that wait and this
     * RPC starts attaching the anon key on a cold start, which RLS answers with 403.
     *
     * The RPC runs first and unguarded: if it throws, the account still exists and nothing else
     * should. Once it succeeds the account is already gone server-side, so the revoke below is best
     * effort — its swallow-then-clear copies signOut()'s shape, with clearSession() unconditional
     * because a deleted account can never again be revoked from this device.
     */
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    override suspend fun deleteAccount(): Unit = authCall {
        client.postgrest.rpc("delete_account")
        try {
            client.auth.signOut(SignOutScope.LOCAL)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Silently swallowed: this class holds no DiagnosticsLogger, and the caller already
            // logs this failure — DeleteUserAccountUseCase.withStepLogging wraps this call under
            // the "remote delete" step regardless of outcome.
        }
        client.auth.clearSession()
    }

    /**
     * Catches Throwable because this is the only funnel; anything supabase-kt or ktor can raise has
     * to leave as a DomainException.
     */
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

/**
 * RefreshFailure maps to NotAuthenticated because the session is still structurally present but its
 * token may be expired, and every consumer of this flow gates a call that would then be rejected.
 */
internal fun SupabaseSessionStatus.toDomain(): SessionStatus = when (this) {
    is SupabaseSessionStatus.Authenticated -> {
        val user = session.user
        if (user != null) {
            SessionStatus.Authenticated(AuthUser(userId = user.id, email = user.email))
        } else {
            SessionStatus.NotAuthenticated
        }
    }

    is SupabaseSessionStatus.Initializing -> SessionStatus.Initializing

    is SupabaseSessionStatus.NotAuthenticated -> SessionStatus.NotAuthenticated

    is SupabaseSessionStatus.RefreshFailure -> SessionStatus.NotAuthenticated
}

internal fun UserInfo.toDomain(): AuthUser = AuthUser(userId = id, email = email)

/**
 * These reach the UI as a corrective hint instead of Unauthorized's "wrong credentials", which on
 * sign-up would be nonsense. The server's errorDescription is English, so the ValidationCode — not
 * the message — is what the UI translates.
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
 * Branch order is load-bearing: on supabase-kt 3.7.0 both AuthRestException and
 * UnauthorizedRestException extend RestException, so the plain RestException branch must stay below
 * them. SessionRequiredException, despite the name, extends Exception directly and never competes.
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
