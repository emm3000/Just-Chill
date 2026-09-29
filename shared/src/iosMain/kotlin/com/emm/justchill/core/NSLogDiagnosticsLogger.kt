package com.emm.justchill.core

import com.emm.justchill.core.domain.shared.logging.DiagnosticsLogger
import platform.Foundation.NSLog

class NSLogDiagnosticsLogger : DiagnosticsLogger {

    override fun warn(message: String, throwable: Throwable?) {
        val line: String = if (throwable == null) message else "$message: ${throwable.stackTraceToString()}"
        NSLog("$TAG ${line.replace("%", "%%")}")
    }

    private companion object {
        const val TAG: String = "JustChill"
    }
}
