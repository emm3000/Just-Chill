package com.emm.justchill.loans.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.emm.justchill.core.theme.BackgroundColor
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.daily.DailyUi
import com.emm.justchill.daily.domain.Driver

@Composable
fun DriverViewScreen(
    driver: Driver,
    driversLoansAndDailies: Pair<List<LoanUi>, List<DailyUi>>,
    navigateToSeeDailies: (Long) -> Unit,
    navigateToSeePayments: (String, String) -> Unit,
    navigateToAddLoans: (Long) -> Unit,
    addDaily: (Long, String) -> Unit,
    deleteLoan: (String) -> Unit,
    deleteDaily: (String) -> Unit,
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {

        DriverViewItem(
            driver = driver,
            loans = driversLoansAndDailies.first,
            dailies = driversLoansAndDailies.second,
            navigateToSeeDailies = navigateToSeeDailies,
            navigateToSeePayments = navigateToSeePayments,
            navigateToAddLoans = navigateToAddLoans,
            addDaily = addDaily,
            deleteLoan = deleteLoan,
            deleteDaily = deleteDaily
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DriverViewScreenPreview(modifier: Modifier = Modifier) {
    EmmTheme {
        DriverViewScreen(
            driver = Driver(2, ""),
            driversLoansAndDailies = Pair(emptyList(), emptyList()),
            navigateToSeeDailies = {},
            navigateToSeePayments = { s: String, s1: String -> },
            navigateToAddLoans = {},
            addDaily = { l: Long, s: String -> },
            deleteLoan = {}, deleteDaily = {}
        )
    }
}