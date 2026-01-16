package com.emm.justchill.hh.fasttransaction

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.domain.account.Account
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LatoFontFamily
import com.emm.justchill.hh.transaction.components.EmmCenteredToolbar
import com.emm.justchill.hh.transaction.components.NewAccountItem
import com.emm.justchill.hh.transaction.components.NewButton

@Composable
fun AccountsScreen(
    accounts: List<Account>,
    addCategory: () -> Unit,
    addAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {

    Column(
        modifier = modifier.background(MaterialTheme.colorScheme.background)
    ) {

        Box(
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            EmmCenteredToolbar(title = "Cuentas")

            MinimalDropdownMenu(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .statusBarsPadding(),
                goToCreateAccount = addAccount,
                goToCreateCategory = addCategory
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            if (accounts.isEmpty()) {
                item {
                    EmptyAccountsPlaceholder(
                        onCreateAccount = {
                            addAccount()
                        }
                    )
                }
            }

            items(accounts, key = Account::accountId) {
                NewAccountItem(
                    modifier = Modifier.fillMaxWidth(),
                    accountName = it.name,
                    balance = it.balance,
                    accountType = it.name
                )
            }
        }
    }
}

@Composable
fun MinimalDropdownMenu(
    modifier: Modifier,
    goToCreateAccount: () -> Unit = {},
    goToCreateCategory: () -> Unit = {},
) {

    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
    ) {
        IconButton(onClick = { expanded = !expanded }) {
            Icon(Icons.Default.MoreVert, contentDescription = "More options")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Crear categorias") },
                onClick = {
                    expanded = false
                    goToCreateCategory()
                }
            )
            DropdownMenuItem(
                text = { Text("Crear cuenta") },
                onClick = {
                    expanded = false
                    goToCreateAccount()
                }
            )
        }
    }
}

@Composable
private fun EmptyAccountsPlaceholder(
    onCreateAccount: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Sin cuentas todavía",
            fontFamily = LatoFontFamily,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            fontSize = 16.sp,
        )
        NewButton(
            title = "Crear cuenta",
            onClick = onCreateAccount,
            enabled = true,
            modifier = Modifier.fillMaxWidth()
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
            addCategory = {},
            modifier = Modifier.fillMaxSize(),
            addAccount = {},
        )
    }
}