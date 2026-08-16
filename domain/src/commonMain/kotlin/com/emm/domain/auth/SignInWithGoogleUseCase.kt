package com.emm.domain.auth

import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.error.ValidationCode

class SignInWithGoogleUseCase(private val authRepository: AuthRepository) {

    suspend operator fun invoke(idToken: String, rawNonce: String): AuthUser {
        ensure(
            idToken.isNotBlank(),
            DomainException.ValidationError("Google ID token must not be blank", ValidationCode.GoogleTokenInvalid),
        )
        ensure(
            rawNonce.isNotBlank(),
            DomainException.ValidationError("Nonce must not be blank", ValidationCode.GoogleTokenInvalid),
        )
        return authRepository.signInWithGoogle(idToken, rawNonce)
    }
}
