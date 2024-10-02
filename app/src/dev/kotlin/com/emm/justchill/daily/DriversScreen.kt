package com.emm.justchill.daily

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
import com.emm.justchill.daily.domain.Driver
import org.koin.androidx.compose.koinViewModel

@Composable
fun DriversScreen(
    vm: DriversViewModel = koinViewModel(),
    navigateToSeeDailies: (Long) -> Unit,
    navigateToAddLoans: (Long) -> Unit,
    navigateToSeePayments: (String, String) -> Unit,
) {

    val a: Map<Driver, DriversScreenUi> by vm.drivers.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        items(a.keys.toList(), key = Driver::driverId) {
            DriverItem(
                driver = it,
                loans = a[it]?.loans.orEmpty(),
                navigateToSeeDailies = navigateToSeeDailies,
                navigateToAddLoans = navigateToAddLoans,
                addDaily = vm::addDaily,
                navigateToSeePayments = navigateToSeePayments,
                deleteLoan = vm::deleteLoan,
                dailies = a[it]?.dailies.orEmpty(),
            )
        }
    }

}

@Preview(showBackground = true)
@Composable
fun DailyScreenPreview(modifier: Modifier = Modifier) {
    EmmTheme {
    }
}