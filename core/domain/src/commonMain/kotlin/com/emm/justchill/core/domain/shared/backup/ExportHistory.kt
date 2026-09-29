package com.emm.justchill.core.domain.shared.backup

import kotlinx.datetime.LocalDate

interface ExportHistory {

    fun daysSinceLastExport(today: LocalDate): Int?

    fun recordExport()
}
