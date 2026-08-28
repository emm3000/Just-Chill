package com.emm.justchill.hh.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.emm.domain.account.Account
import com.emm.domain.account.AccountType
import com.emm.domain.shared.AccountId
import com.emm.domain.shared.YearMonth
import com.emm.justchill.components.EmmButton
import com.emm.justchill.components.EmmButtonVariant
import com.emm.justchill.components.EmmTextInput
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType
import kotlinx.datetime.Month

@Composable
fun AccountsScreen(
    state: AccountsUiState,
    onIntent: (AccountsIntent) -> Unit,
    addAccount: () -> Unit,
    navigateToLoans: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEmmColors.current
    val spacing = LocalEmmSpacing.current

    Column(modifier = modifier.background(colors.bg)) {
        AccountsHeader(state = state, addAccount = addAccount)

        LazyColumn(contentPadding = PaddingValues(bottom = spacing.s3)) {
            if (state.accounts.isEmpty()) {
                item { EmptyState(onCreate = addAccount) }
            } else {
                items(state.accounts, key = { it.account.accountId.value }) { row ->
                    AccountRow(
                        row = row,
                        onEdit = { onIntent(AccountsIntent.OnEditClick(row.account)) },
                        onDelete = { onIntent(AccountsIntent.OnDeleteClick(row.account)) },
                    )
                }
            }

            item {
                LoansSection(
                    totalOwed = state.loansTotalOwed,
                    people = state.loansPeople,
                    onClick = navigateToLoans,
                )
            }
        }
    }

    if (state.pendingEdit != null) {
        EditAccountDialog(
            name = state.editName,
            onNameChange = { onIntent(AccountsIntent.OnEditNameChange(it)) },
            onConfirm = { onIntent(AccountsIntent.OnEditConfirm) },
            onDismiss = { onIntent(AccountsIntent.OnEditDismiss) },
        )
    }

    state.pendingDelete?.let { account ->
        DeleteAccountDialog(
            accountName = account.name,
            onConfirm = { onIntent(AccountsIntent.OnDeleteConfirm) },
            onDismiss = { onIntent(AccountsIntent.OnDeleteDismiss) },
        )
    }
}

@Composable
private fun EditAccountDialog(
    name: String,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar cuenta") },
        text = {
            EmmTextInput(
                value = name,
                onValueChange = onNameChange,
                label = "NOMBRE",
                placeholder = "ejm. Gasto diario",
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

@Composable
private fun DeleteAccountDialog(accountName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val colors = LocalEmmColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¿Borrar «$accountName»?") },
        text = { Text("Si tiene movimientos asociados, no se puede borrar.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = "Borrar", color = colors.danger)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

@Composable
private fun EmptyState(onCreate: () -> Unit) {
    val colors = LocalEmmColors.current
    val spacing = LocalEmmSpacing.current
    val type = LocalEmmType.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.s6, vertical = spacing.s8),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.AccountBalanceWallet,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(spacing.s8),
        )
        Spacer(Modifier.height(spacing.s3))
        Text(text = "Aún sin cuentas", style = type.titleL, color = colors.textPrimary)
        Spacer(Modifier.height(spacing.s1))
        Text(
            text = "Crea una para empezar a registrar movimientos",
            style = type.bodyM,
            color = colors.textSecondary,
        )
        Spacer(Modifier.height(spacing.s5))
        EmmButton(text = "Crear cuenta", onClick = onCreate, variant = EmmButtonVariant.Secondary)
    }
}

private val previewMonth = YearMonth(2026, Month.AUGUST)

private val previewAccounts = listOf(
    AccountMonthUi(
        account = Account(accountId = AccountId("1"), name = "BCP", type = AccountType.Bank),
        movementCount = 5,
        net = "−S/ 193.45",
    ),
    AccountMonthUi(
        account = Account(accountId = AccountId("2"), name = "Efectivo", type = AccountType.Cash),
        movementCount = 0,
        net = "S/ 0.00",
    ),
    AccountMonthUi(
        account = Account(accountId = AccountId("3"), name = "Yape", type = AccountType.Wallet),
        movementCount = 12,
        net = "S/ 1,240.00",
    ),
)

@Preview
@Composable
private fun AccountsScreenPreview() {
    EmmTheme {
        AccountsScreen(
            state = AccountsUiState(
                month = previewMonth,
                accounts = previewAccounts,
                monthSpent = "S/ 193.45",
                monthIncome = "S/ 3,500.00",
                loansTotalOwed = "+S/ 500.00",
                loansPeople = listOf("Carlos"),
            ),
            onIntent = {},
            addAccount = {},
            navigateToLoans = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview
@Composable
private fun AccountsScreenZeroTotalsPreview() {
    EmmTheme {
        AccountsScreen(
            state = AccountsUiState(month = previewMonth, accounts = previewAccounts),
            onIntent = {},
            addAccount = {},
            navigateToLoans = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview
@Composable
private fun AccountsScreenEmptyPreview() {
    EmmTheme {
        AccountsScreen(
            state = AccountsUiState(month = previewMonth),
            onIntent = {},
            addAccount = {},
            navigateToLoans = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}
