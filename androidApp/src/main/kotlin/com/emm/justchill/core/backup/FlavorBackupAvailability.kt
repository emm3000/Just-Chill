package com.emm.justchill.core.backup

import com.emm.justchill.BuildConfig
import com.emm.justchill.core.domain.shared.backup.BackupAvailability

class FlavorBackupAvailability : BackupAvailability {
    override val isAvailable: Boolean = BuildConfig.SNAPSHOT_BACKUP_ENABLED
}
