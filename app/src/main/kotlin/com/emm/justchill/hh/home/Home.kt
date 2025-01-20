package com.emm.justchill.hh.home

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emm.justchill.core.theme.DeleteButtonColor
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.HhBackgroundColor
import com.emm.justchill.core.theme.HhOnBackgroundColor
import com.emm.justchill.hh.account.domain.Account
import com.emm.justchill.hh.shared.fromCentsToSolesWith
import com.emm.justchill.hh.shared.shared.EmmDropDown
import com.emm.justchill.hh.transaction.presentation.EmmCenteredToolbar
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
            .background(HhBackgroundColor)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        EmmCenteredToolbar(
            title = "Just Chill",
            actions = {
                var showMenu by remember { mutableStateOf(false) }
                IconButton(onClick = { showMenu = !showMenu }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = null,
                        tint = HhOnBackgroundColor,
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        onClick = {
                            showMenu = !showMenu
                            navigateToCreateAccount()
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.AccountBalance, contentDescription = null)
                        },
                        text = {
                            Text("Agregar cuentas")
                        }
                    )
                    DropdownMenuItem(
                        onClick = {
                            showMenu = !showMenu
                            navigateToCreateCategory()
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.Category, contentDescription = null)
                        },
                        text = {
                            Text("Agregar categorías")
                        }
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
        )

        EmmDropDown(
            textLabel = "Accounts",
            textPlaceholder = "Selecciona una cuenta",
            items = accounts,
            itemSelected = accountSelected,
            onItemSelected = onAccountChange,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(40.dp))

        accountSelected?.let { BalanceSection(it) }

        Spacer(modifier = Modifier.height(20.dp))

        EmmHeading(
            text = "Ingreso",
            textColor = HhOnBackgroundColor,
        )
        EmmHeadlineMedium(
            text = "S/ ${homeState.income}",
            textColor = HhOnBackgroundColor,
        )

        Spacer(modifier = Modifier.height(20.dp))

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
            HhOnBackgroundColor
        }
        EmmHeading(
            text = "Diferencia",
            textColor = pickColorBy,
        )

        EmmHeadlineMediumLight(
            text = "S/ ${homeState.difference}",
            textColor = pickColorBy
        )

        Spacer(modifier = Modifier.height(16.dp))
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

@Preview(showBackground = true)
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