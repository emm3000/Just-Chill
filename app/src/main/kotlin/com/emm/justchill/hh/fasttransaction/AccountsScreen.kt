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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.domain.account.Account
import com.emm.justchill.components.EmmAmountChill
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LatoFontFamily
import com.emm.justchill.hh.account.AddAccountAction
import com.emm.justchill.hh.account.AddAccountUiState
import com.emm.justchill.hh.shared.fromCentsToSolesWith
import com.emm.justchill.hh.shared.shared.EmmTextInput
import com.emm.justchill.hh.transaction.EmmCenteredToolbar
import com.emm.justchill.hh.transaction.NewButton
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    accounts: List<Account>,
    onAction: (AddAccountAction) -> Unit,
    state: AddAccountUiState,
    addCategory: () -> Unit,
    modifier: Modifier = Modifier,
) {

    val (showBottomSheet, setShowBottomSheet) = remember { mutableStateOf(false) }

    Column(
        modifier = modifier.background(MaterialTheme.colorScheme.background)
    ) {

        Box(
            modifier = Modifier.padding(horizontal = 10.dp),
        ) {
            EmmCenteredToolbar(title = "Cuentas")

            MinimalDropdownMenu(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .statusBarsPadding(),
                goToCreateAccount = {
                    setShowBottomSheet(true)
                },
                goToCreateCategory = addCategory
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            items(accounts, key = Account::accountId) {
                AccountItem(it) {}
            }
        }
    }

    if (showBottomSheet) {
        val scope = rememberCoroutineScope()
        val focusRequester = remember { FocusRequester() }
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = {
                setShowBottomSheet(false)
            },
            sheetState = sheetState,
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {

                Text(
                    text = "Monto inicial",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontFamily = LatoFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                EmmAmountChill(
                    value = state.balance,
                    onValueChange = {
                        onAction(AddAccountAction.OnAmountChange(it))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )

                EmmTextInput(
                    value = state.name,
                    onChange = {
                        onAction(AddAccountAction.OnNameChange(it))
                    },
                    label = "Nombre *",
                    placeholder = "Ingresa el nombre",
                    modifier = Modifier,
                )

                NewButton(
                    title = "Crear cuenta",
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                onAction(AddAccountAction.OnSave)
                                setShowBottomSheet(false)
                            }
                        }
                    },
                    enabled = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            LaunchedEffect(sheetState) {
                snapshotFlow { sheetState.currentValue }
                    .collect { value ->
                        if (value == SheetValue.Expanded) {
                            focusRequester.requestFocus()
                        }
                    }
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
fun AccountItem(
    account: Account,
    onCardClick: (Account) -> Unit,
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                shape = RoundedCornerShape(20)
            )
            .clickable {
                onCardClick(account)
            }
            .padding(horizontal = 14.dp, vertical = 6.dp),
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
            addCategory = {},
            modifier = Modifier.fillMaxSize(),
            state = AddAccountUiState(),
            onAction = {}
        )
    }
}