package com.emm.justchill.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.emm.justchill.core.CommitHash
import com.emm.justchill.core.preferences.AppPreferences
import com.emm.justchill.core.ui.atoms.EmmSnackbarHost
import com.emm.justchill.core.ui.atoms.showEmmSnackbar
import com.emm.justchill.core.ui.category.SelectableCategory
import com.emm.justchill.core.ui.navigation.AppNavigator
import com.emm.justchill.core.ui.navigation.NavHostBindings
import com.emm.justchill.core.ui.navigation.PlatformHostActions
import com.emm.justchill.core.ui.navigation.rememberAppNavigator
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.EmmTheme
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.feature.onboarding.ManifestoRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.core.qualifier.named

@Composable
fun AppNavHost(modifier: Modifier = Modifier, shortcut: ShortcutIntent = ShortcutIntent(), shortcutRequestId: Int = 0) {
    EmmTheme {
        val colors: EmmColors = LocalEmmColors.current
        val appPrefs: AppPreferences = koinInject()
        val appVersion: String = koinInject(named("appVersion"))
        val commitHash: String = koinInject<CommitHash>().value

        val startRoute: NavKey = remember {
            if (appPrefs.firstLaunchSeen) HOME_ROUTE else ManifestoRoute()
        }
        val backStack: NavBackStack<NavKey> = rememberNavBackStack(startRoute)
        val hostNav: AppNavigator = rememberAppNavigator(backStack)
        LaunchedEffect(shortcutRequestId) {
            shortcutRouteToPush(shortcut, appPrefs.firstLaunchSeen, backStack.lastOrNull())?.let(hostNav::pushToTop)
        }
        var pendingCategory by remember { mutableStateOf<SelectableCategory?>(null) }
        var pendingImportJson by remember { mutableStateOf<String?>(null) }
        val snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
        val rootScope: CoroutineScope = rememberCoroutineScope()
        val showRootMessage: (String) -> Unit = { message ->
            rootScope.launch { snackbarHostState.showEmmSnackbar(message) }
        }
        val platform: PlatformHostActions = rememberPlatformHostActions(
            snackbarHostState = snackbarHostState,
            scope = rootScope,
            onImport = { json -> pendingImportJson = json },
        )

        val bindings: NavHostBindings = NavHostBindings(
            backStack = backStack,
            snackbarHostState = snackbarHostState,
            showMessage = showRootMessage,
            platform = platform,
        )
        val channels: HostResultChannels = HostResultChannels(
            pendingCategory = { pendingCategory },
            onCategoryCaptured = { created -> pendingCategory = created },
            onPendingCategoryConsumed = { pendingCategory = null },
            pendingImportJson = { pendingImportJson },
            onImportHandled = { pendingImportJson = null },
        )

        Scaffold(
            modifier = modifier.background(colors.bg),
            snackbarHost = { EmmSnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = WindowInsets.safeDrawing,
        ) { padding ->

            NavDisplay(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.bg)
                    .padding(padding)
                    .consumeWindowInsets(padding),
                backStack = backStack,
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                entryProvider = entryProvider {
                    appEntryGraph(
                        bindings = bindings,
                        channels = channels,
                        appVersion = appVersion,
                        commitHash = commitHash,
                        onFirstLaunchSeen = { appPrefs.firstLaunchSeen = true },
                    )
                },
            )
        }
    }
}
