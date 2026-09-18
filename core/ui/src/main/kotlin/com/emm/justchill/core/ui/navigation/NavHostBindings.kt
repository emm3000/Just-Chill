package com.emm.justchill.core.ui.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Stable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

@Stable
class NavHostBindings(
    val backStack: NavBackStack<NavKey>,
    val startTab: BottomBarRoute,
    val snackbarHostState: SnackbarHostState,
    val showMessage: (String) -> Unit,
    val platform: PlatformHostActions,
)
