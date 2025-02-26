package com.emm.justchill.hh.me.loan.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.theme.BackgroundColor
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun LoansScreen(
    driverId: Long,
    navigateToPayments: (String) -> Unit,
    vm: LoansViewModel = koinViewModel(
        parameters = { parametersOf(driverId) }
    ),
) {

    val loans: List<LoanUi> by vm.loans.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        items(loans, key = LoanUi::loanId) {
            LoanItem(it, navigateToPayments, vm::delete)
        }
    }
}