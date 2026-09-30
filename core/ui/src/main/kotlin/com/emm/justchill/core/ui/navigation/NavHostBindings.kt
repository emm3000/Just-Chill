package com.emm.justchill.core.ui.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Stable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.emm.justchill.core.ui.atoms.EmmSnackbarTone

@Stable
class NavHostBindings(
    val backStack: NavBackStack<NavKey>,
    val snackbarHostState: SnackbarHostState,
    val showMessage: (String, EmmSnackbarTone) -> Unit,
    val platform: PlatformHostActions,
)
