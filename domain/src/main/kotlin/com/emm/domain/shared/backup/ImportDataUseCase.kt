package com.emm.domain.shared.backup

class ImportDataUseCase(private val backupRepository: BackupRepository) {

    suspend operator fun invoke(json: String): ImportStats = backupRepository.importFromJson(json)
}
