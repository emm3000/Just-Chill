package com.emm.justchill.core

import android.util.Log
import com.emm.domain.shared.logging.DiagnosticsLogger
import com.google.firebase.crashlytics.FirebaseCrashlytics

private const val TAG = "JustChill"

/**
 * Android [DiagnosticsLogger]: Logcat during development, Crashlytics in the field.
 *
 * Crashlytics is what closes the observability gap this class exists for — sync, account deletion
 * and the snapshot-backup pipeline all swallow failures on purpose, so without a remote sink those
 * swallows are invisible on a user's device. Non-fatal `recordException` is the right channel:
 * these are degradations, not crashes.
 *
 * Safe in both flavors. `dev` sets `firebase_crashlytics_collection_enabled=false` in its manifest,
 * so these calls are no-ops there and the "build the dev flavor for a telemetry-free app" claim in
 * the privacy policy stays true.
 *
 * Never throws, per the port contract: a logger that can fail would defeat the resilience work it
 * is here to report on, so a Crashlytics failure (not initialised, no `google-services.json`)
 * degrades to Logcat only.
 */
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
