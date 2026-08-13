package com.emm.domain.auth

/**
 * Outcome of [AuthRepository.signOut]. Two things can fail there, and only one of them is reported
 * through this type.
 *
 * **A failed server-side revoke returns [LocalOnly].** It is a qualified success, never a thrown
 * error: the user's intent — leave this account, on this device — is honored either way, so
 * surfacing it as an exception would report a sign-out failure to someone who is, in fact, signed
 * out. The caller is expected to tell [Revoked] and [LocalOnly] apart and word the confirmation
 * accordingly.
 *
 * **A failed local clear throws.** [AuthRepository.signOut] runs the local clear OUTSIDE the
 * swallow that covers the revoke, so if the session store itself breaks, the exception propagates
 * and no value of this type is ever produced — the user is still signed in and has to hear about it.
 * Receiving a [SignOutResult] at all therefore means the device is signed out; it never means "the
 * clear may or may not have happened".
 */
sealed interface SignOutResult {

    /**
     * The server-side revoke request did not fail. This is NOT a guarantee that a live token was
     * destroyed: supabase-kt's `AuthImpl.signOut` treats a 401/403/404 response the same as success
     * (its `SIGN_OUT_IGNORE_CODES`), because that shape means the session was already invalid or
     * gone server-side — the local clear still runs either way, and this result is still reported as
     * [Revoked]. Read [Revoked] as "the server did not refuse the sign-out", not as "a live token
     * was destroyed".
     */
    data object Revoked : SignOutResult

    /**
     * The local session is cleared, but the server-side revoke could not be reached (offline, a
     * timeout, or any other transport failure). The refresh token stays valid remotely until it
     * expires naturally — the accepted cost documented on [AuthRepository.signOut].
     */
    data object LocalOnly : SignOutResult
}
