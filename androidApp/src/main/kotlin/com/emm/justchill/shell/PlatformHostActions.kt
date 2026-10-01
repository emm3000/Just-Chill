package com.emm.justchill.shell

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.ManagedActivityResultLauncher
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
import androidx.core.content.FileProvider
import com.emm.justchill.core.ioDispatcher
import com.emm.justchill.core.ui.atoms.EmmSnackbarTone
import com.emm.justchill.core.ui.atoms.showEmmSnackbar
import com.emm.justchill.core.ui.navigation.PlatformHostActions
import com.emm.justchill.feature.profile.ProfileMessage
import com.emm.justchill.feature.profile.toText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

internal class PendingExport(val json: String, val onResult: (Boolean) -> Unit)

internal fun <D : Any> settleExportResult(
    document: D?,
    pending: PendingExport?,
    write: (D, String) -> Boolean,
    delete: (D) -> Unit,
    notify: (String, EmmSnackbarTone) -> Unit,
) {
    if (document == null) return
    if (pending != null) {
        pending.onResult(write(document, pending.json))
        return
    }
    delete(document)
    notify(ProfileMessage.ExportFailed.toText(), EmmSnackbarTone.Error)
}

// Call at the AppNavHost root, never inside an `entry<...> { }` body: a picker result arriving after
// its entry left composition is dropped.
@Composable
fun rememberPlatformHostActions(
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope,
    onImport: (String) -> Unit,
): PlatformHostActions {
    val context: Context = LocalContext.current
    val currentOnImport by rememberUpdatedState(onImport)

    var pendingExport by remember { mutableStateOf<PendingExport?>(null) }

    val exportLauncher: ManagedActivityResultLauncher<String, Uri?> = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        val pending: PendingExport? = pendingExport
        pendingExport = null
        settleExportResult(
            document = uri,
            pending = pending,
            write = { document, json ->
                runCatching {
                    context.contentResolver.openOutputStream(document)?.use { stream ->
                        stream.bufferedWriter().use { it.write(json) }
                    } != null
                }.getOrDefault(false)
            },
            delete = { document ->
                runCatching {
                    DocumentsContract.deleteDocument(
                        context.contentResolver,
                        document,
                    )
                }
            },
            notify = { message, tone ->
                scope.launch {
                    snackbarHostState.showEmmSnackbar(
                        message = message,
                        tone = tone,
                    )
                }
            },
        )
    }

    val importLauncher: ManagedActivityResultLauncher<Array<String>, Uri?> = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            val text: String? = context.contentResolver.openInputStream(uri)
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

            override val requestExport: (String, String, (Boolean) -> Unit) -> Unit = { fileName, json, onResult ->
                pendingExport = PendingExport(json, onResult)
                exportLauncher.launch(fileName)
            }

            override val requestImport: () -> Unit = {
                importLauncher.launch(arrayOf("application/json"))
            }

            override val shareCsv: (String, String, () -> Unit) -> Unit = { fileName, content, onFailed ->
                scope.launch {
                    val uri: Uri? = withContext(ioDispatcher) { writeSharedExport(context, fileName, content) }
                    if (uri == null || !startCsvChooser(context, fileName, uri)) onFailed()
                }
            }
        }
    }
}

private const val CSV_MIME_TYPE: String = "text/csv"
private const val SHARED_EXPORTS_DIR: String = "exports"

private fun startCsvChooser(context: Context, fileName: String, uri: Uri): Boolean = try {
    val send: Intent = Intent(Intent.ACTION_SEND)
        .setType(CSV_MIME_TYPE)
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    send.clipData = ClipData.newRawUri(fileName, uri)
    context.startActivity(Intent.createChooser(send, "Exportar movimientos"))
    true
} catch (_: ActivityNotFoundException) {
    false
}

private fun writeSharedExport(context: Context, fileName: String, content: String): Uri? = try {
    val directory: File = File(context.cacheDir, SHARED_EXPORTS_DIR).apply { mkdirs() }
    val file: File = File(directory, fileName).apply { writeText(content, Charsets.UTF_8) }
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
} catch (_: IOException) {
    null
} catch (_: IllegalArgumentException) {
    null
}
