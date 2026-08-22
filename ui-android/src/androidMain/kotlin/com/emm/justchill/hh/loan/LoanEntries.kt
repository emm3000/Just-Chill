package com.emm.justchill.hh.loan

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.emm.justchill.hh.shared.AddEditLoanRoute
import com.emm.justchill.hh.shared.AppNavigator
import com.emm.justchill.hh.shared.LoansRoute
import com.emm.justchill.hh.shared.NavHostBindings
import com.emm.justchill.hh.shared.PersonLoansRoute
import com.emm.justchill.hh.shared.rememberAppNavigator
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

fun EntryProviderScope<NavKey>.loanEntries(bindings: NavHostBindings) {
    entry<LoansRoute> {
        val nav: AppNavigator = rememberAppNavigator(bindings.backStack)
        val vm: LoansViewModel = koinViewModel()
        val state by vm.state.collectAsStateWithLifecycle()

        LaunchedEffect(vm) {
            vm.effect.collect { effect ->
                when (effect) {
                    is LoansEffect.NavigateToPerson -> nav.push(PersonLoansRoute(effect.personKey))
                    LoansEffect.NavigateToAddLoan -> nav.push(AddEditLoanRoute())
                }
            }
        }

        LoansScreen(
            state = state,
            onIntent = vm::onIntent,
            onBack = { nav.pop() },
            modifier = Modifier.fillMaxSize(),
        )
    }

    entry<PersonLoansRoute> { key ->
        val nav: AppNavigator = rememberAppNavigator(bindings.backStack)
        val vm: PersonLoansViewModel = koinViewModel { parametersOf(key.personKey) }
        val state by vm.state.collectAsStateWithLifecycle()

        LaunchedEffect(vm) {
            vm.effect.collect { effect ->
                when (effect) {
                    is PersonLoansEffect.ShowError -> bindings.showMessage(effect.message)
                    is PersonLoansEffect.NavigateToEditLoan -> nav.push(AddEditLoanRoute(effect.loanId))
                }
            }
        }

        PersonLoansScreen(
            state = state,
            onIntent = vm::onIntent,
            onBack = { nav.pop() },
            modifier = Modifier.fillMaxSize(),
        )
    }

    entry<AddEditLoanRoute> { key ->
        val nav: AppNavigator = rememberAppNavigator(bindings.backStack)
        AddEditLoanScreen(
            onBack = { nav.pop() },
            snackbarHostState = bindings.snackbarHostState,
            loanId = key.loanId,
        )
    }
}
