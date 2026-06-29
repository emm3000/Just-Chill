package com.emm.justchill.hh.shared

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import kotlinx.coroutines.CoroutineScope

// iOS actual of the unified host's platform seams. All platform integrations (backup export/import,
// share sheet, open-mail) are deferred post-v1, so the action lambdas are inert no-ops and the
// capability flags are off — exactly the behavior the former IosApp.kt had (TODO lambdas + hidden
// Google button + no privacy screen). startTab differs from Android (iOS lands on Home).

private object IosHostActions : PlatformHostActions {
    override val isDebug: Boolean = false
    override val showGoogleSignIn: Boolean = false
    override val supportsPrivacyPolicy: Boolean = false
    override val supportsBackup: Boolean = false
    override val onShareText: (String) -> Unit = { /* TODO post-v1: iOS share sheet */ }
    override val onOpenEmailApp: () -> Unit = { /* TODO post-v1: open iOS Mail app (UIApplication.openURL) */ }
    override val requestExport: (String) -> Unit = { /* TODO post-v1: iOS export (SAF equivalent) */ }
    override val requestImport: () -> Unit = { /* TODO post-v1: iOS import */ }
}

@Composable
actual fun rememberPlatformHostActions(
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope,
    onImported: (String) -> Unit,
): PlatformHostActions = IosHostActions

actual val startTab: BottomBarRoute = HomeRoute
