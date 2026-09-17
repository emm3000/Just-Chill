package com.emm.justchill.core

import android.util.Log
import com.emm.domain.shared.logging.DiagnosticsLogger
import com.google.firebase.crashlytics.FirebaseCrashlytics

private const val TAG = "JustChill"

// Never throws, per the port contract: a logger that can fail would defeat the resilience work it
// exists to report on, so a Crashlytics failure (not initialised, no google-services.json) degrades
// to Logcat only.
class CrashReportingDiagnosticsLogger : DiagnosticsLogger {

    // Intentional broad catch: the port forbids throwing, and there is no useful recovery beyond
    // keeping the Logcat line that was already written above.
    @Suppress("TooGenericExceptionCaught")
    override fun warn(message: String, throwable: Throwable?) {
        Log.w(TAG, message, throwable)
        try {
            val crashlytics = FirebaseCrashlytics.getInstance()
            crashlytics.log(message)
            // A message-only warning has nothing to attach; the breadcrumb above is the record.
            if (throwable != null) crashlytics.recordException(throwable)
        } catch (e: Exception) {
            Log.w(TAG, "Crashlytics unavailable; report kept local", e)
        }
    }
}
