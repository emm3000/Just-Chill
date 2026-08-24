package com.emm.justchill.hh.seetransactions

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.emm.justchill.core.ui.atoms.EmmSnackbarTone
import com.emm.justchill.core.ui.atoms.showEmmSnackbar
import com.emm.justchill.hh.shared.AppNavigator
import com.emm.justchill.hh.shared.EditTransactionRoute
import com.emm.justchill.hh.shared.NavHostBindings
import com.emm.justchill.hh.shared.SeeTransactionRoute
import com.emm.justchill.hh.shared.rememberAppNavigator
import org.koin.compose.viewmodel.koinViewModel

fun EntryProviderScope<NavKey>.seeTransactionsEntries(bindings: NavHostBindings) {
    entry<SeeTransactionRoute> {
        val nav: AppNavigator = rememberAppNavigator(bindings.backStack)
        val vm: SeeTransactionsViewModel = koinViewModel()
        var confirmSheetOpen by remember { mutableStateOf(false) }

        LaunchedEffect(vm) {
            vm.effect.collect { effect ->
                when (effect) {
                    SeeTransactionsEffect.CloseConfirmSheet -> confirmSheetOpen = false

                    is SeeTransactionsEffect.ShowError -> bindings.snackbarHostState.showEmmSnackbar(
                        message = effect.message,
                        tone = EmmSnackbarTone.Error,
                    )
                }
            }
        }

        SeeTransactionsScreen(
            onEditTransaction = { id ->
                nav.push(EditTransactionRoute(id))
            },
            confirmSheetOpen = confirmSheetOpen,
            onConfirmSheetOpenChange = { open -> confirmSheetOpen = open },
            vm = vm,
        )
    }
}
