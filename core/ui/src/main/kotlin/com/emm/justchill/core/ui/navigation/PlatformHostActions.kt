package com.emm.justchill.core.ui.navigation

import androidx.compose.runtime.Stable

@Stable
interface PlatformHostActions {
    val showGoogleSignIn: Boolean
    val supportsPrivacyPolicy: Boolean
    val supportsBackup: Boolean
    val onShareText: (String) -> Unit
    val onOpenEmailApp: () -> Unit
    val requestExport: (fileName: String, json: String, onResult: (saved: Boolean) -> Unit) -> Unit
    val requestImport: () -> Unit
    val shareCsv: (fileName: String, content: String, onFailed: () -> Unit) -> Unit
}
