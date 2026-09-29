package com.emm.justchill.core

import com.emm.justchill.core.domain.shared.backup.BackupAvailability

class FixedBackupAvailability(override val isAvailable: Boolean) : BackupAvailability
