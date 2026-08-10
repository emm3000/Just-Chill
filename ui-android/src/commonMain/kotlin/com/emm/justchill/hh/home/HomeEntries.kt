package com.emm.justchill.hh.home

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.emm.justchill.hh.shared.AddTransactionRoute
import com.emm.justchill.hh.shared.AppNavigator
import com.emm.justchill.hh.shared.EditTransactionRoute
import com.emm.justchill.hh.shared.HomeRoute
import com.emm.justchill.hh.shared.NavHostBindings
import com.emm.justchill.hh.shared.ReportRoute
import com.emm.justchill.hh.shared.SeeTransactionRoute
import com.emm.justchill.hh.shared.rememberAppNavigator
import org.koin.compose.viewmodel.koinViewModel

/** Registers the home entries on the host: [HomeRoute]. */
fun EntryProviderScope<NavKey>.homeEntries(bindings: NavHostBindings) {
    entry<HomeRoute> {
        val nav: AppNavigator = rememberAppNavigator(bindings.backStack)
        HomeEntry(
            navigateToAll = { nav.switchTab(SeeTransactionRoute) },
            navigateToAdd = { nav.push(AddTransactionRoute) },
            navigateToEdit = { id -> nav.push(EditTransactionRoute(id)) },
            navigateToReport = { nav.push(ReportRoute) },
            snackbarHostState = bindings.snackbarHostState,
        )
    }
}

@Composable
private fun HomeEntry(
    navigateToAll: () -> Unit,
    navigateToAdd: () -> Unit,
    navigateToEdit: (String) -> Unit,
    navigateToReport: () -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val vm: HomeViewModel = koinViewModel()
    var confirmSheetOpen by remember { mutableStateOf(false) }

    LaunchedEffect(vm) {
        vm.effect.collect { effect ->
            when (effect) {
                HomeEffect.CloseConfirmSheet -> confirmSheetOpen = false
                is HomeEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    HomeScreen(
        homeViewModel = vm,
        confirmSheetOpen = confirmSheetOpen,
        onConfirmSheetOpenChange = { confirmSheetOpen = it },
        navigateToAll = navigateToAll,
        navigateToAdd = navigateToAdd,
        navigateToEdit = navigateToEdit,
        navigateToReport = navigateToReport,
    )
}
