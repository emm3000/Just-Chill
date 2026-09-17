package com.emm.justchill.core.domain.shared.logging

interface DiagnosticsLogger {
    fun warn(message: String, throwable: Throwable? = null)
}
