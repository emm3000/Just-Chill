package com.emm.justchill.core.domain.shared.backup

class ImportDataUseCase(private val backupRepository: BackupRepository) {

    suspend operator fun invoke(json: String): ImportStats = backupRepository.importFromJson(json)
}
