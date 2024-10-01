package com.emm.justchill.quota

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.emm.justchill.core.theme.BackgroundColor
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.loans.presentation.LoanUi
import com.emm.justchill.quota.domain.Driver
import org.koin.androidx.compose.koinViewModel

@Composable
fun DriversScreen(
    vm: DriversViewModel = koinViewModel(),
    navigateToSeeQuotas: (Long) -> Unit,
    navigateToAddLoans: (Long) -> Unit,
    navigateToSeePayments: (String, String) -> Unit,
) {

    val a: Map<Driver, List<LoanUi>> by vm.drivers.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        items(a.keys.toList(), key = Driver::driverId) {
            DriverItem(
                driver = it,
                loans = a[it].orEmpty(),
                navigateToSeeQuotas = navigateToSeeQuotas,
                navigateToAddLoans = navigateToAddLoans,
                addQuota = vm::addQuota,
                navigateToSeePayments = navigateToSeePayments,
                deleteLoan = vm::deleteLoan,
            )
        }
    }

}

@Preview(showBackground = true)
@Composable
fun QuotaScreenPreview(modifier: Modifier = Modifier) {
    EmmTheme {
//        QuotaScreen()
    }
}