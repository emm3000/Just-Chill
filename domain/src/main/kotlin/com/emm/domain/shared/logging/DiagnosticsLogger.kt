package com.emm.domain.shared.logging

interface DiagnosticsLogger {
    fun warn(message: String, throwable: Throwable? = null)
}
