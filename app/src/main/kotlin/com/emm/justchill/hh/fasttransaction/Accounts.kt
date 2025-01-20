package com.emm.justchill.hh.fasttransaction

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.components.HhCard
import com.emm.justchill.core.theme.DeleteButtonColor
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.HhBackgroundColor
import com.emm.justchill.core.theme.HhCardTextColor
import com.emm.justchill.core.theme.LatoFontFamily
import com.emm.justchill.core.theme.PrimaryButtonColor
import com.emm.justchill.hh.account.domain.Account
import com.emm.justchill.hh.transaction.presentation.EmmCenteredToolbar
import com.emm.justchill.hh.transaction.presentation.TransactionType

@Composable
fun Accounts(
    accounts: List<Account>,
    onCardClick: (Account, TransactionType) -> Unit,
    modifier: Modifier = Modifier,
) {

    Column(
        modifier = modifier
            .background(HhBackgroundColor)
    ) {

        EmmCenteredToolbar(
            title = "Cuentas",
        )

        LazyColumn(
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            items(accounts, key = Account::accountId) {
                AccountItem(it, onCardClick)
            }
        }
    }
}

@Composable
private fun AccountItem(
    account: Account,
    onCardClick: (Account, TransactionType) -> Unit,
) {
    HhCard(
        modifier = Modifier
            .fillMaxWidth(),
    ) {
        TitleAndDescription(account)
        AddTransactionButtons(onCardClick, account)
    }
}

@Composable
private fun AddTransactionButtons(
    onCardClick: (Account, TransactionType) -> Unit,
    account: Account,
) {
    Row {
        TextButton(
            onClick = { onCardClick(account, TransactionType.INCOME) },
            modifier = Modifier.height(40.dp),
        ) {
            Text(
                text = "Ingreso",
                fontFamily = FontFamily.SansSerif,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PrimaryButtonColor
            )
        }
        TextButton(
            onClick = { onCardClick(account, TransactionType.SPENT) },
            modifier = Modifier.height(40.dp),
        ) {
            Text(
                text = "Gasto",
                fontFamily = FontFamily.SansSerif,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DeleteButtonColor,
            )
        }
    }
}

@Composable
private fun TitleAndDescription(it: Account) {
    Row {
        Text(
            text = it.name,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.SansSerif,
            fontStyle = FontStyle.Normal,
            color = HhCardTextColor,
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = it.description.ifBlank { "No description" },
            fontSize = 14.sp,
            fontFamily = LatoFontFamily,
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Normal,
            color = HhCardTextColor,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AccountsPreview() {
    EmmTheme {
        Accounts(
            accounts = listOf(
                Account(
                    accountId = "1",
                    name = "random name",
                    balance = 123.22,
                    initialBalance = 322.322,
                    description = "random descripction"

                ),
                Account(
                    accountId = "2",
                    name = "lorem itsum",
                    balance = 123.22,
                    initialBalance = 322.322,
                    description = "random descripction"

                ),
                Account(
                    accountId = "3",
                    name = "random name",
                    balance = 123.22,
                    initialBalance = 322.322,
                    description = "random descripction"

                )
            ),
            onCardClick = { _, _ -> },
            modifier = Modifier.fillMaxSize()
        )
    }
}