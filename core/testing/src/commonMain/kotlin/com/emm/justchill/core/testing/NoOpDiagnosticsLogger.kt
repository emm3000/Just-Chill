package com.emm.justchill.core.testing

import com.emm.justchill.core.domain.shared.logging.DiagnosticsLogger

class NoOpDiagnosticsLogger : DiagnosticsLogger {
    override fun warn(message: String, throwable: Throwable?) = Unit
}
