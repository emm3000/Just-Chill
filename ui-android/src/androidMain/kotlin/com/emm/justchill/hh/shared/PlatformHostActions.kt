package com.emm.justchill.hh.shared

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.emm.justchill.core.ui.atoms.EmmSnackbarTone
import com.emm.justchill.core.ui.atoms.showEmmSnackbar
import com.emm.justchill.hh.profile.ProfileMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// The host's platform seams. This was an expect/actual pair while the module still compiled for iOS;
// the module is Android-only, so the two halves collapsed into this single declaration. Behavior is
// unchanged: SAF CreateDocument for export (with success/failure snackbar), SAF OpenDocument for
// import (feeds onImport), ACTION_SEND share chooser, and ACTION_MAIN/CATEGORY_APP_EMAIL with a
// missing-app snackbar fallback.

/**
 * Holder of the host actions + capability flags consumed by [AppNavHost] entries.
 *
 * The capability flags gate navigation / view-model calls that a platform may not be able to
 * complete. With Android as the only target they are all constant — the seam survives because the
 * entries read it, not because it still varies. Collapsing it is a separate change.
 */
@Stable
interface PlatformHostActions {
    /** Whether the debug-only Profile section is shown (Android debug build). */
    val isDebug: Boolean

    /** Whether the "Continuar con Google" button is shown on the auth screen. */
    val showGoogleSignIn: Boolean

    /** Whether the privacy-policy screen can be opened. */
    val supportsPrivacyPolicy: Boolean

    /** Whether backup export/import is available (Android SAF). */
    val supportsBackup: Boolean

    /** Shares plain text via the platform share sheet (Android chooser). */
    val onShareText: (String) -> Unit

    /** Opens the platform email app (Android intent + missing-app snackbar). */
    val onOpenEmailApp: () -> Unit

    /** Writes the backup [json] to a user-picked destination and reports success/failure (Android SAF). */
    val requestExport: (json: String) -> Unit

    /** Opens the platform document picker and feeds the chosen file's contents to `onImport`. */
    val requestImport: () -> Unit
}

/**
 * Builds the [PlatformHostActions] for the app. Called once at the [AppNavHost] root so the SAF
 * launchers (which must be registered in composition) live above the [androidx.navigation3]
 * NavDisplay and survive entry recomposition.
 *
 * @param onImport invoked with the imported file contents.
 */
@Composable
fun rememberPlatformHostActions(
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope,
    onImport: (String) -> Unit,
): PlatformHostActions {
    val context = LocalContext.current
    val currentOnImport by rememberUpdatedState(onImport)

    // Holds the backup JSON produced by the VM until the SAF picker returns a destination.
    var pendingExportJson by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        val json = pendingExportJson
        pendingExportJson = null
        if (uri != null && json != null) {
            // Platform owns the SAF write; the VM only generated the JSON (commonMain, no IO).
            val ok = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.bufferedWriter().use { it.write(json) }
                } != null
            }.getOrDefault(false)
            val message = if (ok) ProfileMessage.ExportDone else ProfileMessage.ExportFailed
            scope.launch {
                snackbarHostState.showEmmSnackbar(
                    message = message.toText(),
                    tone = if (ok) EmmSnackbarTone.Success else EmmSnackbarTone.Error,
                )
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            val text = context.contentResolver.openInputStream(uri)
                ?.bufferedReader()
                ?.use { it.readText() }
            if (text != null) currentOnImport(text)
        }
    }

    val debuggable = remember(context) {
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    return remember(context, scope, snackbarHostState, exportLauncher, importLauncher, debuggable) {
        object : PlatformHostActions {
            override val isDebug: Boolean = debuggable
            override val showGoogleSignIn: Boolean = true
            override val supportsPrivacyPolicy: Boolean = true
            override val supportsBackup: Boolean = true

            override val onShareText: (String) -> Unit = { text ->
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                context.startActivity(Intent.createChooser(intent, "Compartir reporte"))
            }

            override val onOpenEmailApp: () -> Unit = {
                try {
                    val intent = Intent(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_APP_EMAIL)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                } catch (_: ActivityNotFoundException) {
                    scope.launch {
                        snackbarHostState.showEmmSnackbar(
                            message = "No encontramos una app de correo en tu teléfono.",
                            tone = EmmSnackbarTone.Error,
                        )
                    }
                }
            }

            override val requestExport: (String) -> Unit = { json ->
                pendingExportJson = json
                exportLauncher.launch(suggestedExportFilename())
            }

            override val requestImport: () -> Unit = {
                importLauncher.launch(arrayOf("application/json"))
            }
        }
    }
}

/**
 * The bottom-bar tab the back stack is rooted at (the "exit through home" base) and the route the
 * first-launch Manifesto gate lands on.
 */
val startTab: BottomBarRoute = SeeTransactionRoute

private fun suggestedExportFilename(): String {
    val date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    return "justchill-backup-$date.json"
}
