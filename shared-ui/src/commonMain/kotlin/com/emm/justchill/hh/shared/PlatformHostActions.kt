package com.emm.justchill.hh.shared

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import kotlinx.coroutines.CoroutineScope

// Platform seams for the unified nav host (AppNavHost). Five capabilities differ between Android and
// iOS and are preserved EXACTLY via expect/actual: backup export/import, share, open-email, and the
// privacy-policy click. Android wires real intents / SAF launchers; iOS no-ops (those integrations are
// deferred post-v1). Keeping every android.*/SAF import inside the androidMain actual is what lets the
// host itself live in commonMain with zero platform leakage.

/**
 * Holder of the platform-specific host actions + capability flags consumed by [AppNavHost] entries.
 *
 * The action lambdas are no-ops on iOS. The capability flags gate shared navigation / view-model calls
 * that must stay inert on iOS (e.g. pushing [PrivacyPolicyRoute] or triggering the backup export, which
 * would otherwise run a use case the platform cannot complete).
 */
@Stable
interface PlatformHostActions {
    /** Whether the debug-only Profile section is shown (Android debug build; false on iOS). */
    val isDebug: Boolean

    /** Whether the "Continuar con Google" button is shown on the auth screen (Android only). */
    val showGoogleSignIn: Boolean

    /** Whether the privacy-policy screen can be opened (Android only; iOS keeps the privacy TODO). */
    val supportsPrivacyPolicy: Boolean

    /** Whether backup export/import is available (Android SAF; iOS deferred post-v1). */
    val supportsBackup: Boolean

    /** Shares plain text via the platform share sheet (Android chooser; iOS no-op). */
    val onShareText: (String) -> Unit

    /** Opens the platform email app (Android intent + missing-app snackbar; iOS no-op). */
    val onOpenEmailApp: () -> Unit

    /** Writes the backup [json] to a user-picked destination and reports success/failure (Android SAF). */
    val requestExport: (json: String) -> Unit

    /** Opens the platform document picker and feeds the chosen file's contents to `onImport`. */
    val requestImport: () -> Unit
}

/**
 * Builds the [PlatformHostActions] for the current platform. Called once at the [AppNavHost] root so the
 * Android SAF launchers (which must be registered in composition) live above the [androidx.navigation3]
 * NavDisplay and survive entry recomposition.
 *
 * @param onImport invoked with the imported file contents (Android only; never fires on iOS).
 */
@Composable
expect fun rememberPlatformHostActions(
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope,
    onImport: (String) -> Unit,
): PlatformHostActions

/**
 * The bottom-bar tab the back stack is rooted at (the "exit through home" base) and the route the
 * first-launch Manifesto gate lands on. Differs per platform: Android starts on [SeeTransactionRoute],
 * iOS on [HomeRoute] — preserved from the two original hosts.
 */
expect val startTab: BottomBarRoute
