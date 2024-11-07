package com.emm.justchill.hh.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emm.justchill.core.theme.DeleteButtonColor
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.hh.account.domain.Account
import com.emm.justchill.hh.shared.fromCentsToSolesWith
import com.emm.justchill.hh.shared.shared.EmmDropDown
import org.koin.androidx.compose.koinViewModel

@Composable
fun Home(
    homeViewModel: HomeViewModel = koinViewModel(),
    navigateToCreateAccount: () -> Unit,
    navigateToCreateCategory: () -> Unit,
) {

    val accounts: List<Account> by homeViewModel.accounts.collectAsStateWithLifecycle()
    val homeState: HomeState by homeViewModel.calculators.collectAsStateWithLifecycle()

    Home(
        accounts = accounts,
        homeState = homeState,
        accountSelected = homeViewModel.accountSelected,
        navigateToCreateAccount = navigateToCreateAccount,
        navigateToCreateCategory = navigateToCreateCategory,
        onAccountChange = homeViewModel::updateAccountSelected,
    )
}

@Composable
fun Home(
    accounts: List<Account> = emptyList(),
    homeState: HomeState,
    accountSelected: Account? = null,
    navigateToCreateAccount: () -> Unit,
    navigateToCreateCategory: () -> Unit,
    onAccountChange: (Account) -> Unit,
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = 40.dp, horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            EmmSecondaryButton(
                text = "Agregar cuenta",
                imageVector = Icons.Filled.AccountBalance,
                onClick = navigateToCreateAccount,
                modifier = Modifier.fillMaxWidth(),
            )
            EmmSecondaryButton(
                text = "Agregar categoría",
                imageVector = Icons.Filled.Category,
                onClick = navigateToCreateCategory,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(20.dp))

        EmmDropDown(
            textLabel = "Accounts",
            textPlaceholder = "Selecciona una cuenta",
            items = accounts,
            itemSelected = accountSelected,
            onItemSelected = onAccountChange,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(50.dp))

        accountSelected?.let { BalanceSection(it) }

        Spacer(modifier = Modifier.height(16.dp))

        EmmHeading(text = "Ingreso")
        
        EmmHeadlineMedium(text = "S/ ${homeState.income}")

        Spacer(modifier = Modifier.height(40.dp))

        EmmHeading(
            text = "Gasto",
            textColor = DeleteButtonColor,
        )
        EmmHeadlineMedium(
            text = "S/ ${homeState.spend}",
            textColor = DeleteButtonColor,
        )

        Spacer(modifier = Modifier.height(20.dp))

        val pickColorBy = if (homeState.difference.contains("-")) {
            DeleteButtonColor
        } else {
            MaterialTheme.colorScheme.onBackground
        }
        EmmHeading(
            text = "Diferencia",
            textColor = pickColorBy,
        )

        EmmHeadlineMediumLight(
            text = "S/ ${homeState.difference}",
            textColor = pickColorBy
        )
    }
}

@Composable
private fun BalanceSection(account: Account) {
    val balance = remember(account) {
        fromCentsToSolesWith(account.balance)
    }

    val pickColorByBalance = if (balance.contains("-")) {
        DeleteButtonColor
    } else {
        MaterialTheme.colorScheme.onBackground
    }
    EmmHeading(
        text = "Balance",
        textColor = pickColorByBalance
    )
    EmmHeadlineMedium(
        text = "S/ $balance",
        textColor = pickColorByBalance
    )
}

@PreviewLightDark
@Composable
fun HomePreview(modifier: Modifier = Modifier) {
    EmmTheme {
        Home(
            homeState = HomeState(
                difference = "202.00",
                income = "300.00",
                spend = "404.00"
            ),
            navigateToCreateAccount = {},
            navigateToCreateCategory = {},
            onAccountChange = {}
        )
    }
}