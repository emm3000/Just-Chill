package com.emm.justchill.hh.shared

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import com.emm.justchill.core.sync.SyncController
import com.emm.justchill.core.sync.SyncEvent
import com.emm.justchill.core.ui.atoms.EmmSnackbarTone
import com.emm.justchill.core.ui.atoms.showEmmSnackbar

/**
 * Handles one-shot [SyncEvent]s from the [SyncController] by showing snackbars and, when the user
 * taps an action, dispatching side-effects (retry or navigate to sign-in).
 *
 * Lives in commonMain so both nav hosts (Android [Hh] / iOS IosApp) share the same retry/session
 * snackbar behavior. It depends only on the [SyncController] port, so any implementation that emits
 * [SyncEvent]s drives it.
 *
 * Dual-channel contract: [com.emm.justchill.core.sync.SyncStatus.lastSyncFailed] drives the
 * persistent Perfil row badge; [SyncEvent] drives these one-shot snackbars. Both can fire for the
 * same manual failure BY DESIGN — the snackbar is immediate feedback, the row is the persistent
 * recovery point.
 *
 * Navigation stays injected via [onNavigateToSignIn] so this composable has no direct dependency
 * on the back stack.
 */
@Composable
fun SyncEventsHandler(
    syncController: SyncController,
    snackbarHostState: SnackbarHostState,
    onNavigateToSignIn: () -> Unit,
) {
    val currentOnNavigateToSignIn by rememberUpdatedState(onNavigateToSignIn)

    LaunchedEffect(syncController) {
        syncController.events.collect { event ->
            when (event) {
                is SyncEvent.SyncFailed -> {
                    val result = snackbarHostState.showEmmSnackbar(
                        message = "No se pudo sincronizar",
                        tone = EmmSnackbarTone.Error,
                        actionLabel = "REINTENTAR",
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        syncController.requestSync(manual = true)
                    }
                }

                SyncEvent.SessionExpired -> {
                    val result = snackbarHostState.showEmmSnackbar(
                        message = "Tu sesión expiró. Inicia sesión de nuevo.",
                        tone = EmmSnackbarTone.Error,
                        actionLabel = "ENTRAR",
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        currentOnNavigateToSignIn()
                    }
                }
            }
        }
    }
}
