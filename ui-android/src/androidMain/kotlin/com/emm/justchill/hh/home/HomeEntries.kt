package com.emm.justchill.hh.home

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.emm.justchill.core.ui.atoms.EmmSnackbarTone
import com.emm.justchill.core.ui.atoms.showEmmSnackbar
import com.emm.justchill.hh.shared.AddTransactionRoute
import com.emm.justchill.hh.shared.AppNavigator
import com.emm.justchill.hh.shared.EditTransactionRoute
import com.emm.justchill.hh.shared.HomeRoute
import com.emm.justchill.hh.shared.LoansRoute
import com.emm.justchill.hh.shared.NavHostBindings
import com.emm.justchill.hh.shared.ReportRoute
import com.emm.justchill.hh.shared.SeeTransactionRoute
import com.emm.justchill.hh.shared.rememberAppNavigator
import org.koin.compose.viewmodel.koinViewModel

fun EntryProviderScope<NavKey>.homeEntries(bindings: NavHostBindings) {
    entry<HomeRoute> {
        val nav: AppNavigator = rememberAppNavigator(bindings.backStack)
        val vm: HomeViewModel = koinViewModel()
        var confirmSheetOpen by remember { mutableStateOf(false) }

        LaunchedEffect(vm) {
            vm.effect.collect { effect ->
                when (effect) {
                    HomeEffect.CloseConfirmSheet -> confirmSheetOpen = false

                    is HomeEffect.ShowError -> bindings.snackbarHostState.showEmmSnackbar(
                        message = effect.message,
                        tone = EmmSnackbarTone.Error,
                    )
                }
            }
        }

        HomeScreen(
            homeViewModel = vm,
            confirmSheetOpen = confirmSheetOpen,
            onConfirmSheetOpenChange = { open -> confirmSheetOpen = open },
            navigateToAll = { nav.switchTab(SeeTransactionRoute) },
            navigateToAdd = { nav.push(AddTransactionRoute) },
            navigateToEdit = { id -> nav.push(EditTransactionRoute(id)) },
            navigateToReport = { nav.push(ReportRoute) },
            navigateToLoans = { nav.push(LoansRoute) },
        )
    }
}
