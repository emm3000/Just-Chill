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
import org.koin.androidx.compose.koinViewModel

@Composable
fun DriversScreen(
    vm: DriversViewModel = koinViewModel(),
    navigateToSeeQuotas: (Long) -> Unit,
    navigateToAddQuotas: (Long) -> Unit,
    navigateToSeeLoans: (Long) -> Unit,
    navigateToAddLoans: (Long) -> Unit,
) {

    val a by vm.drivers.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        items(a) {
            DriverItem(
                driver = it,
                navigateToSeeQuotas = navigateToSeeQuotas,
                navigateToAddQuotas = navigateToAddQuotas,
                navigateToSeeLoans = navigateToSeeLoans,
                navigateToAddLoans = navigateToAddLoans,
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