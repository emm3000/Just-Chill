package com.emm.justchill.hh.fasttransaction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.domain.account.Account
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LatoFontFamily
import com.emm.justchill.hh.shared.fromCentsToSolesWith
import com.emm.justchill.hh.transaction.EmmCenteredToolbar

@Composable
fun AccountsScreen(
    accounts: List<Account>,
    onCardClick: (Account) -> Unit,
    addAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {

    Column(
        modifier = modifier.background(MaterialTheme.colorScheme.background)
    ) {

        Box(
            modifier = Modifier.padding(horizontal = 10.dp),
        ) {
            EmmCenteredToolbar(title = "Cuentas")

            IconButton(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .statusBarsPadding(),
                onClick = addAccount
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(17.dp)
        ) {

            items(accounts, key = Account::accountId) {
                AccountItem(it, onCardClick)
            }
        }
    }
}

@Composable
fun AccountItem(
    account: Account,
    onCardClick: (Account) -> Unit,
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                shape = RoundedCornerShape(20)
            )
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .clickable {
                onCardClick(account)
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {

        Text(
            text = account.name,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = LatoFontFamily,
            fontStyle = FontStyle.Normal,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "S/ ${fromCentsToSolesWith(account.balance)}",
            fontSize = 16.sp,
            fontFamily = LatoFontFamily,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Normal,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AccountsScreenPreview() {
    EmmTheme {
        AccountsScreen(
            accounts = listOf(
                Account(
                    accountId = "1",
                    name = "random nameww",
                    balance = 12223.22,
                ),
                Account(
                    accountId = "2",
                    name = "lorem itsum",
                    balance = 123.22,

                    ),
                Account(
                    accountId = "3",
                    name = "random name",
                    balance = 123.22,
                )
            ),
            onCardClick = { },
            addAccount = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}