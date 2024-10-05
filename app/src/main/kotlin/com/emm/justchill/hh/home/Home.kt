package com.emm.justchill.hh.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.dropUnlessResumed
import com.emm.justchill.core.theme.BackgroundColor
import com.emm.justchill.core.theme.DeleteButtonColor
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LatoFontFamily
import com.emm.justchill.core.theme.PrimaryButtonColor
import com.emm.justchill.core.theme.TextColor
import com.emm.justchill.hh.account.domain.Account
import com.emm.justchill.hh.shared.fromCentsToSolesWith
import com.emm.justchill.hh.shared.shared.DropDownContainer
import org.koin.androidx.compose.koinViewModel

@Composable
fun Home(
    homeViewModel: HomeViewModel = koinViewModel(),
    navigateToCreateAccount: () -> Unit,
    navigateToCreateCategory: () -> Unit,
) {

    val sumTransactions by homeViewModel.sumTransactions.collectAsState()
    val difference by homeViewModel.difference.collectAsState()
    val accounts by homeViewModel.accounts.collectAsState()

    Home(
        sumTransaction = sumTransactions,
        difference = difference,
        navigateToCreateAccount = navigateToCreateAccount,
        navigateToCreateCategory = navigateToCreateCategory,
        accounts = accounts,
        onAccountChange = homeViewModel::updateAccountSelected,
        accountLabel = homeViewModel.accountLabel,
        onAccountLabelChange = homeViewModel::updateAccountLabel,
        account = homeViewModel.account
    )
}

@Composable
fun Home(
    sumTransaction: Pair<String, String> = Pair("", ""),
    difference: String = "",
    navigateToCreateAccount: () -> Unit = {},
    navigateToCreateCategory: () -> Unit = {},
    accounts: List<Account> = emptyList(),
    onAccountChange: (Account) -> Unit = {},
    accountLabel: String = "",
    onAccountLabelChange: (String) -> Unit = {},
    account: Account? = null,
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(vertical = 40.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        Row(
            horizontalArrangement = Arrangement.Center,
        ) {
            TextButton(
                onClick = dropUnlessResumed { navigateToCreateAccount() }
            ) {
                Text(
                    text = "Agregar cuenta",
                    fontSize = 16.sp,
                    fontFamily = LatoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryButtonColor
                )
            }
            TextButton(
                onClick = dropUnlessResumed { navigateToCreateCategory() }
            ) {
                Text(
                    text = "Agregar categoría",
                    fontSize = 16.sp,
                    fontFamily = LatoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryButtonColor
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        DropDownContainer(
            account = accounts,
            onAccountChange = onAccountChange,
            text = accountLabel,
            setText = onAccountLabelChange
        )

        Spacer(Modifier.height(50.dp))

        account?.let {
            val balance = remember(it) {
                fromCentsToSolesWith(it.balance)
            }
            Text(
                text = "Balance",
                fontFamily = LatoFontFamily,
                fontWeight = FontWeight.Bold,
                color = if (balance.contains("-")) {
                    DeleteButtonColor
                } else {
                    TextColor
                }
            )
            Text(
                text = "S/ $balance",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = if (balance.contains("-")) {
                    DeleteButtonColor
                } else {
                    TextColor
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Ingreso",
            fontFamily = LatoFontFamily,
            fontWeight = FontWeight.Bold,
            color = TextColor
        )
        Text(
            text = "S/ ${sumTransaction.first}",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextColor
        )

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "Gasto",
            fontFamily = LatoFontFamily,
            fontWeight = FontWeight.Bold,
            color = DeleteButtonColor,
        )
        Text(
            text = "S/ ${sumTransaction.second}",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = DeleteButtonColor,
        )
        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Diferencia",
            fontFamily = LatoFontFamily,
            fontWeight = FontWeight.Bold,
            color = if (difference.contains("-")) {
                DeleteButtonColor
            } else {
                TextColor
            },
        )
        Text(
            text = "S/ $difference",
            fontSize = 25.sp,
            color = if (difference.contains("-")) {
                DeleteButtonColor
            } else {
                TextColor
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomePreview(modifier: Modifier = Modifier) {
    EmmTheme {
        Home(
            sumTransaction = Pair("200.50", "502.5"),
            difference = "200.00"
        )
    }
}