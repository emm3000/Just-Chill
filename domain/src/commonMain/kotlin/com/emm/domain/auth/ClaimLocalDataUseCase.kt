package com.emm.domain.auth

class ClaimLocalDataUseCase(private val claimLocalDataRepository: ClaimLocalDataRepository) {

    suspend operator fun invoke(userId: String) {
        claimLocalDataRepository.claimAll(userId)
    }
}
