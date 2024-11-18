package com.emm.justchill.hh.fasttransaction

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LatoFontFamily
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
            .background(MaterialTheme.colorScheme.background)
    ) {

        EmmCenteredToolbar(
            title = "Cuentas",
        )

        LazyColumn(
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            items(accounts, key = Account::accountId) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = it.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = LatoFontFamily,
                                fontStyle = FontStyle.Normal,
                            )
                            Text(
                                text = "${it.description.ifBlank { "No description" }} - S/ ${it.balance}",
                                fontSize = 14.sp,
                                fontFamily = LatoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontStyle = FontStyle.Normal,
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            TextButton(
                                onClick = { onCardClick(it, TransactionType.INCOME) },
                                modifier = Modifier.height(40.dp),
                            ) {
                                Text("Ingreso")
                            }
                            TextButton(
                                onClick = { onCardClick(it, TransactionType.SPENT) },
                                modifier = Modifier.height(40.dp),
                            ) {
                                Text("Gasto")
                            }
                        }
                    }
                }
            }
        }

    }

}

@PreviewLightDark
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
                    name = "random name",
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