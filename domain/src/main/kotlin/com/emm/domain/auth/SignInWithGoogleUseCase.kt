package com.emm.domain.auth

import com.emm.domain.shared.error.DomainException

/**
 * Exchanges a Google ID token for a Supabase session.
 * Claiming anonymous-local rows is handled by [ClaimLocalDataOnAuthenticationUseCase],
 * which reacts to the session becoming Authenticated — same as email sign-in.
 */
class SignInWithGoogleUseCase(private val authRepository: AuthRepository) {

    suspend operator fun invoke(idToken: String, rawNonce: String): AuthUser {
        ensure(idToken.isNotBlank(), DomainException.ValidationError("Google ID token must not be blank"))
        ensure(rawNonce.isNotBlank(), DomainException.ValidationError("Nonce must not be blank"))
        return authRepository.signInWithGoogle(idToken, rawNonce)
    }
}
