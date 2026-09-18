package com.emm.justchill.hh.shared

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.emm.justchill.core.ui.atoms.EmmSnackbarTone
import com.emm.justchill.core.ui.atoms.showEmmSnackbar
import com.emm.justchill.core.ui.navigation.PlatformHostActions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private class PendingExport(val json: String, val onResult: (Boolean) -> Unit)

/**
 * Call at the [AppNavHost] root, never inside an `entry<...> { }` body: the SAF launchers must be
 * registered in a composition that outlives NavDisplay's entries, or a picker result arriving after
 * its entry left composition is dropped.
 */
@Composable
fun rememberPlatformHostActions(
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope,
    onImport: (String) -> Unit,
): PlatformHostActions {
    val context = LocalContext.current
    val currentOnImport by rememberUpdatedState(onImport)

    var pendingExport by remember { mutableStateOf<PendingExport?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        val pending = pendingExport
        pendingExport = null
        if (uri != null && pending != null) {
            val saved: Boolean = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.bufferedWriter().use { it.write(pending.json) }
                } != null
            }.getOrDefault(false)
            pending.onResult(saved)
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

    return remember(context, scope, snackbarHostState, exportLauncher, importLauncher) {
        object : PlatformHostActions {
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

            override val requestExport: (String, (Boolean) -> Unit) -> Unit = { json, onResult ->
                pendingExport = PendingExport(json, onResult)
                exportLauncher.launch(suggestedExportFilename())
            }

            override val requestImport: () -> Unit = {
                importLauncher.launch(arrayOf("application/json"))
            }
        }
    }
}

private fun suggestedExportFilename(): String {
    val date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    return "justchill-backup-$date.json"
}
