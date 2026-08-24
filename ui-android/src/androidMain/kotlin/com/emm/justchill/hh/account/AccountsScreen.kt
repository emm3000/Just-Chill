package com.emm.justchill.hh.account

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.domain.account.Account
import com.emm.domain.account.AccountType
import com.emm.domain.shared.AccountId
import com.emm.domain.shared.Money
import com.emm.justchill.components.EmmButton
import com.emm.justchill.components.EmmButtonVariant
import com.emm.justchill.components.EmmListItem
import com.emm.justchill.components.EmmTextInput
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.InterFontFamily
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmRadii
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType
import com.emm.justchill.core.ui.atoms.Eyebrow
import com.emm.justchill.core.ui.atoms.Hairline

// state/onIntent are the screen's data and event sink; addCategory/addAccount/navigateToLoans are
// its three doors, each a distinct destination. Splitting these into a config object would relocate
// the count, not reduce it.
@Suppress("LongParameterList")
@Composable
fun AccountsScreen(
    state: AccountsUiState,
    onIntent: (AccountsIntent) -> Unit,
    addCategory: () -> Unit,
    addAccount: () -> Unit,
    navigateToLoans: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEmmColors.current

    Column(
        modifier = modifier
            .background(colors.bg),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Cuentas",
                fontSize = 28.sp,
                fontWeight = FontWeight.W700,
                fontFamily = InterFontFamily,
                color = colors.textPrimary,
                letterSpacing = (-0.4).sp,
                modifier = Modifier.weight(1f),
            )
            NewAccountButton(onClick = addAccount)
        }

        Hairline()

        // Always mounted so the Préstamos row survives the accounts-empty branch — a row that
        // depended on `state.accounts` being non-empty would be a second unreachable-door landmine
        // (ui-android/CLAUDE.md already records the first, LoansCard on Home).
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(top = 4.dp, bottom = 12.dp),
        ) {
            item {
                TotalBalanceRow(totalBalance = state.totalBalance, totalBalanceMoney = state.totalBalanceMoney)
                Hairline()
            }

            item {
                LoansEntryRow(totalOwed = state.loansTotalOwed, onClick = navigateToLoans)
                Hairline()
            }

            if (state.accounts.isEmpty()) {
                item {
                    // fillParentMaxHeight() sets height only, not width — CenterHorizontally
                    // needs fillMaxWidth() too, or this wrap-content Column hugs the left edge.
                    EmptyState(onCreate = addAccount, modifier = Modifier.fillMaxWidth().fillParentMaxHeight())
                }
            } else {
                items(state.accounts, key = { it.accountId.value }) { account ->
                    AccountRow(
                        account = account,
                        movementCount = state.movementCounts[account.accountId] ?: 0,
                        onEdit = { onIntent(AccountsIntent.OnEditClick(account)) },
                        onDelete = { onIntent(AccountsIntent.OnDeleteClick(account)) },
                    )
                }
            }
        }

        Hairline()
        ManageCategoriesRow(onClick = addCategory)
    }

    state.pendingEdit?.let {
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
private fun NewAccountButton(onClick: () -> Unit) {
    val colors = LocalEmmColors.current
    val shape = RoundedCornerShape(999.dp)

    Row(
        modifier = Modifier
            .clip(shape)
            .background(colors.surface3)
            .border(1.dp, colors.borderFocus, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Add,
            contentDescription = null,
            tint = colors.textPrimary,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = "Nueva",
            fontSize = 13.sp,
            fontWeight = FontWeight.W600,
            fontFamily = InterFontFamily,
            color = colors.textPrimary,
        )
    }
}

// Home's HeroBalance (deleted with the screen, E06-04/ADR 010) tinted a negative balance `danger`;
// this screen deliberately drops that per DESIGN_SYSTEM.md §4 — "negative stays monochrome, an
// expense is never red" — so a negative totalBalanceMoney maps to `textPrimary`, not `danger`.
@Composable
private fun TotalBalanceRow(totalBalance: String, totalBalanceMoney: Money) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current

    val amountColor = when {
        totalBalanceMoney.cents > 0L -> colors.success
        totalBalanceMoney.cents < 0L -> colors.textPrimary
        else -> colors.textTertiary
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.s4, vertical = spacing.s4),
    ) {
        Eyebrow(text = "Saldo total")
        Spacer(Modifier.height(spacing.s2))
        Text(text = totalBalance, style = type.amountLead, color = amountColor)
    }
}

@Composable
private fun LoansEntryRow(totalOwed: String, onClick: () -> Unit) {
    // ADR 010: totalOwed is a parallel-ledger number, formatted via the shared totalOwedFormatted()
    // helper — never derived from `state.accounts` and never folded into any balance on this screen.
    EmmListItem(
        icon = Icons.Outlined.People,
        title = "Préstamos",
        metadata = "Te deben",
        amount = totalOwed,
        onClick = onClick,
    )
}

@Composable
private fun AccountRow(account: Account, movementCount: Int, onEdit: () -> Unit, onDelete: () -> Unit) {
    val colors = LocalEmmColors.current

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AccountIconTile(
                icon = account.type.toIcon(),
                tintColor = accountDotColor(account.name, colors),
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.W600,
                    fontFamily = InterFontFamily,
                    color = colors.textPrimary,
                    letterSpacing = (-0.15).sp,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${account.type.toLabel()} · ${formatMovements(movementCount)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.W400,
                    fontFamily = InterFontFamily,
                    color = colors.textTertiary,
                )
            }

            AccountRowMenu(onEdit = onEdit, onDelete = onDelete)
        }
        Hairline()
    }
}

private fun formatMovements(count: Int): String = when (count) {
    0 -> "Sin movimientos"
    1 -> "1 movimiento"
    else -> "$count movimientos"
}

@Composable
private fun AccountIconTile(icon: ImageVector, tintColor: Color) {
    val colors = LocalEmmColors.current
    val shape = RoundedCornerShape(8.dp)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(34.dp)
            .clip(shape)
            .background(tintColor.copy(alpha = 0.18f))
            .border(1.dp, tintColor.copy(alpha = 0.32f), shape),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tintColor,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun AccountRowMenu(onEdit: () -> Unit, onDelete: () -> Unit) {
    val colors = LocalEmmColors.current
    var expanded by remember { mutableStateOf(false) }

    Box {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { expanded = !expanded },
                ),
        ) {
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = "Opciones de cuenta",
                tint = colors.textTertiary,
                modifier = Modifier.size(18.dp),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = colors.surface2,
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = "Editar",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.W500,
                        fontFamily = InterFontFamily,
                        color = colors.textPrimary,
                        letterSpacing = (-0.15).sp,
                    )
                },
                leadingIcon = { MenuIcon(Icons.Outlined.Edit) },
                onClick = {
                    expanded = false
                    onEdit()
                },
            )
            DropdownMenuItem(
                text = {
                    Text(
                        text = "Borrar",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.W500,
                        fontFamily = InterFontFamily,
                        color = colors.danger,
                        letterSpacing = (-0.15).sp,
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = colors.danger,
                        modifier = Modifier.size(20.dp),
                    )
                },
                onClick = {
                    expanded = false
                    onDelete()
                },
            )
        }
    }
}

@Composable
private fun ManageCategoriesRow(onClick: () -> Unit) {
    val colors = LocalEmmColors.current
    val radii = LocalEmmRadii.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(28.dp)
                .clip(radii.rS)
                .background(colors.surface2)
                .border(1.dp, colors.border, radii.rS),
        ) {
            Icon(
                imageVector = Icons.Outlined.Category,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(14.dp),
            )
        }

        Text(
            text = "Gestionar categorías",
            fontSize = 15.sp,
            fontWeight = FontWeight.W500,
            fontFamily = InterFontFamily,
            color = colors.textPrimary,
            letterSpacing = (-0.15).sp,
            modifier = Modifier.weight(1f),
        )

        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(16.dp),
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
private fun MenuIcon(icon: ImageVector) {
    val colors = LocalEmmColors.current
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = colors.textSecondary,
        modifier = Modifier.size(20.dp),
    )
}

@Composable
private fun EmptyState(onCreate: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalEmmColors.current

    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.AccountBalanceWallet,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = "Aún sin cuentas",
            fontSize = 18.sp,
            fontWeight = FontWeight.W600,
            fontFamily = InterFontFamily,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Crea una para empezar a registrar movimientos",
            fontSize = 13.sp,
            fontFamily = InterFontFamily,
            color = colors.textSecondary,
        )
        Spacer(Modifier.height(20.dp))
        EmmButton(
            text = "Crear cuenta",
            onClick = onCreate,
            variant = EmmButtonVariant.Secondary,
        )
    }
}

private val previewAccounts = listOf(
    Account(accountId = AccountId("1"), name = "Yape", type = AccountType.Wallet),
    Account(accountId = AccountId("2"), name = "Plin", type = AccountType.Wallet),
    Account(accountId = AccountId("3"), name = "BCP", type = AccountType.Bank),
    Account(accountId = AccountId("4"), name = "BBVA", type = AccountType.Bank),
    Account(accountId = AccountId("5"), name = "Cash", type = AccountType.Cash),
)

private val previewMovementCounts = mapOf(
    AccountId("1") to 32,
    AccountId("2") to 8,
    AccountId("3") to 14,
    AccountId("4") to 4,
    AccountId("5") to 11,
)

@Preview
@Composable
private fun AccountsScreenPreview() {
    EmmTheme {
        AccountsScreen(
            state = AccountsUiState(
                accounts = previewAccounts,
                movementCounts = previewMovementCounts,
                loansTotalOwed = "S/ 350.00",
                totalBalance = "S/ 4,820.00",
                totalBalanceMoney = Money(482_000L),
            ),
            onIntent = {},
            addCategory = {},
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
            state = AccountsUiState(
                accounts = previewAccounts,
                movementCounts = previewMovementCounts,
                loansTotalOwed = "S/ 0.00",
                totalBalance = "S/ 0.00",
                totalBalanceMoney = Money.Zero,
            ),
            onIntent = {},
            addCategory = {},
            addAccount = {},
            navigateToLoans = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview
@Composable
private fun AccountsScreenNegativeBalancePreview() {
    EmmTheme {
        AccountsScreen(
            state = AccountsUiState(
                accounts = previewAccounts,
                movementCounts = previewMovementCounts,
                loansTotalOwed = "S/ 120.00",
                totalBalance = "−S/ 350.00",
                totalBalanceMoney = Money(-35_000L),
            ),
            onIntent = {},
            addCategory = {},
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
            state = AccountsUiState(),
            onIntent = {},
            addCategory = {},
            addAccount = {},
            navigateToLoans = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}
