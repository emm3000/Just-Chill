package com.emm.justchill.hh.fasttransaction

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.dropUnlessResumed
import com.emm.domain.account.Account
import com.emm.domain.account.AccountSelect
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LatoFontFamily
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
                onClick = dropUnlessResumed { addAccount() }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }

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
    onCardClick: (Account) -> Unit,
) {

    val isSelected: BorderStroke? = when (account.isSelected){
        AccountSelect.IsSelected -> BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        AccountSelect.NonSelected -> null
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = true,
                role = Role.RadioButton,
                onClick = { onCardClick(account) }
            ),
        border = isSelected
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 7.dp),
        ) {
            Text(
                text = account.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = LatoFontFamily,
                fontStyle = FontStyle.Normal,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = account.description.ifBlank { "No description" },
                fontSize = 14.sp,
                fontFamily = LatoFontFamily,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Normal,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun AccountsScreenPreview() {
    EmmTheme {
        AccountsScreen(
            accounts = listOf(
                Account(
                    accountId = "1",
                    name = "random name",
                    balance = 123.22,
                    description = "random descripction",
                    isSelected = AccountSelect.NonSelected

                ),
                Account(
                    accountId = "2",
                    name = "lorem itsum",
                    balance = 123.22,
                    description = "random descripction",
                    isSelected = AccountSelect.NonSelected

                ),
                Account(
                    accountId = "3",
                    name = "random name",
                    balance = 123.22,
                    description = "random descripction",
                    isSelected = AccountSelect.NonSelected

                )
            ),
            onCardClick = { },
            addAccount = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}