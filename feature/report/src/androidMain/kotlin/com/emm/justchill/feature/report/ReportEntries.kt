package com.emm.justchill.feature.report

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.emm.justchill.core.ui.atoms.EmmSnackbarTone
import com.emm.justchill.core.ui.atoms.showEmmSnackbar
import com.emm.justchill.core.ui.navigation.AppNavigator
import com.emm.justchill.core.ui.navigation.NavHostBindings
import com.emm.justchill.core.ui.navigation.rememberAppNavigator
import org.koin.compose.viewmodel.koinViewModel

fun EntryProviderScope<NavKey>.reportEntries(bindings: NavHostBindings, onAddTransaction: (AppNavigator) -> Unit) {
    entry<ReportRoute> {
        val nav: AppNavigator = rememberAppNavigator(bindings.backStack)
        ReportEntry(
            onAddTransaction = { onAddTransaction(nav) },
            onShareText = bindings.platform.onShareText,
            snackbarHostState = bindings.snackbarHostState,
        )
    }
}

@Composable
private fun ReportEntry(
    onAddTransaction: () -> Unit,
    onShareText: (String) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val vm: ReportViewModel = koinViewModel()
    val currentOnShareText by rememberUpdatedState(onShareText)

    LaunchedEffect(vm) {
        vm.effect.collect { effect ->
            when (effect) {
                is ReportEffect.ShowError -> snackbarHostState.showEmmSnackbar(
                    message = effect.message,
                    tone = EmmSnackbarTone.Error,
                )

                is ReportEffect.ShareReport -> currentOnShareText(effect.text)
            }
        }
    }

    ReportScreen(vm = vm, onAddTransaction = onAddTransaction)
}
