package com.emm.domain.auth

/**
 * Signs the user out by invalidating the remote session.
 *
 * Local data is NOT wiped: rows are kept on-device and remain accessible while offline.
 * Removing or re-anonymising local rows on sign-out is a :data / :app concern and must
 * not be done here.
 */
class SignOutUseCase(private val authRepository: AuthRepository) {

    suspend operator fun invoke() {
        authRepository.signOut()
    }
}
