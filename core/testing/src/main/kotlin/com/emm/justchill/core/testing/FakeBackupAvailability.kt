package com.emm.justchill.core.testing

import com.emm.justchill.core.domain.shared.backup.BackupAvailability

class FakeBackupAvailability(override val isAvailable: Boolean) : BackupAvailability
