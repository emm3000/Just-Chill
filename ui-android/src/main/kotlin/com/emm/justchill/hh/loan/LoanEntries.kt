package com.emm.justchill.hh.loan

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.emm.justchill.hh.shared.AddEditLoanRoute
import com.emm.justchill.hh.shared.AppNavigator
import com.emm.justchill.hh.shared.LoanDetailRoute
import com.emm.justchill.hh.shared.LoansRoute
import com.emm.justchill.hh.shared.NavHostBindings
import com.emm.justchill.hh.shared.PersonLoansRoute
import com.emm.justchill.hh.shared.rememberAppNavigator
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

fun EntryProviderScope<NavKey>.loanEntries(bindings: NavHostBindings) {
    entry<LoansRoute> {
        val nav: AppNavigator = rememberAppNavigator(bindings.backStack)
        LoansEntry(
            onNavigateToPerson = { personKey -> nav.push(PersonLoansRoute(personKey)) },
            onNavigateToAddLoan = { nav.push(AddEditLoanRoute()) },
            onBack = { nav.pop() },
        )
    }

    entry<PersonLoansRoute> { key ->
        val nav: AppNavigator = rememberAppNavigator(bindings.backStack)
        PersonLoansEntry(
            personKey = key.personKey,
            onNavigateToLoanDetail = { loanId -> nav.push(LoanDetailRoute(loanId)) },
            onBack = { nav.pop() },
        )
    }

    entry<LoanDetailRoute> { key ->
        val nav: AppNavigator = rememberAppNavigator(bindings.backStack)
        LoanDetailEntry(
            loanId = key.loanId,
            onNavigateToEditLoan = { nav.push(AddEditLoanRoute(key.loanId)) },
            onShowError = bindings.showMessage,
            onLoanDelete = { nav.pop() },
            onBack = { nav.pop() },
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

@Composable
private fun LoansEntry(onNavigateToPerson: (String) -> Unit, onNavigateToAddLoan: () -> Unit, onBack: () -> Unit) {
    val vm: LoansViewModel = koinViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val currentNavigateToPerson by rememberUpdatedState(onNavigateToPerson)
    val currentNavigateToAddLoan by rememberUpdatedState(onNavigateToAddLoan)

    LaunchedEffect(vm) {
        vm.effect.collect { effect ->
            when (effect) {
                is LoansEffect.NavigateToPerson -> currentNavigateToPerson(effect.personKey)
                LoansEffect.NavigateToAddLoan -> currentNavigateToAddLoan()
            }
        }
    }

    LoansScreen(state = state, onIntent = vm::onIntent, onBack = onBack, modifier = Modifier.fillMaxSize())
}

@Composable
private fun PersonLoansEntry(personKey: String, onNavigateToLoanDetail: (String) -> Unit, onBack: () -> Unit) {
    val vm: PersonLoansViewModel = koinViewModel { parametersOf(personKey) }
    val state by vm.state.collectAsStateWithLifecycle()
    val currentNavigateToLoanDetail by rememberUpdatedState(onNavigateToLoanDetail)

    LaunchedEffect(vm) {
        vm.effect.collect { effect ->
            when (effect) {
                is PersonLoansEffect.NavigateToLoanDetail -> currentNavigateToLoanDetail(effect.loanId)
            }
        }
    }

    PersonLoansScreen(state = state, onIntent = vm::onIntent, onBack = onBack, modifier = Modifier.fillMaxSize())
}

@Composable
private fun LoanDetailEntry(
    loanId: String,
    onNavigateToEditLoan: () -> Unit,
    onShowError: (String) -> Unit,
    onLoanDelete: () -> Unit,
    onBack: () -> Unit,
) {
    val vm: LoanDetailViewModel = koinViewModel { parametersOf(loanId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val currentNavigateToEditLoan by rememberUpdatedState(onNavigateToEditLoan)
    val currentShowError by rememberUpdatedState(onShowError)
    val currentLoanDelete by rememberUpdatedState(onLoanDelete)

    LaunchedEffect(vm) {
        vm.effect.collect { effect ->
            when (effect) {
                is LoanDetailEffect.ShowError -> currentShowError(effect.message)
                LoanDetailEffect.NavigateToEditLoan -> currentNavigateToEditLoan()
                LoanDetailEffect.LoanDeleted -> currentLoanDelete()
            }
        }
    }

    LoanDetailScreen(state = state, onIntent = vm::onIntent, onBack = onBack, modifier = Modifier.fillMaxSize())
}
