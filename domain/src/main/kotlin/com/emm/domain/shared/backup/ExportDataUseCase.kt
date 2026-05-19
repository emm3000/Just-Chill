package com.emm.domain.shared.backup

class ExportDataUseCase(
    private val backupRepository: BackupRepository,
) {

    suspend operator fun invoke(exportedAt: Long, appVersion: String): String {
        return backupRepository.exportToJson(exportedAt, appVersion)
    }
}
