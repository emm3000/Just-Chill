package com.emm.justchill.core.ui.navigation

import androidx.compose.runtime.Stable

@Stable
interface PlatformHostActions {
    val showGoogleSignIn: Boolean
    val supportsPrivacyPolicy: Boolean
    val supportsBackup: Boolean
    val onShareText: (String) -> Unit
    val onOpenEmailApp: () -> Unit

    /**
     * `onResult` fires with `true` only once the bytes are on disk, and with `false` when the write
     * failed. A picker the user backed out of is silent on this channel, so the caller never hears
     * about an export nobody asked to finish.
     */
    val requestExport: (json: String, onResult: (saved: Boolean) -> Unit) -> Unit
    val requestImport: () -> Unit
}
