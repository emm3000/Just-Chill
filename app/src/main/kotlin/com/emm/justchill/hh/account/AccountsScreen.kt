package com.emm.justchill.hh.account

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.domain.account.Account
import com.emm.justchill.components.EmmButton
import com.emm.justchill.components.EmmButtonVariant
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType

@Composable
fun AccountsScreen(
    accounts: List<Account>,
    addCategory: () -> Unit,
    addAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current

    Column(modifier = modifier.background(colors.bg).statusBarsPadding()) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = spacing.s4,
                    end = spacing.s4,
                    top = spacing.s6,
                    bottom = spacing.s4,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Cuentas",
                style = type.headlineL,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            AddMenu(
                onAddAccount = addAccount,
                onAddCategory = addCategory,
            )
        }

        if (accounts.isEmpty()) {
            EmptyState(onCreate = addAccount, modifier = Modifier.fillMaxSize())
            return@Column
        }

        LazyColumn(contentPadding = PaddingValues(bottom = spacing.s8)) {
            items(accounts, key = Account::accountId) { account ->
                AccountRow(account = account)
            }
        }
    }
}

@Composable
private fun AccountRow(account: Account) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.s4, vertical = spacing.s4)
            .drawBehind {
                drawLine(
                    color = colors.border,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1f,
                )
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.s4),
    ) {
        Icon(
            imageVector = Icons.Outlined.AccountBalanceWallet,
            contentDescription = null,
            tint = colors.textPrimary,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = account.name,
            style = type.bodyL,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun AddMenu(onAddAccount: () -> Unit, onAddCategory: () -> Unit) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    var expanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    Box {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = { expanded = !expanded },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = "Más opciones",
                tint = colors.textPrimary,
                modifier = Modifier.size(22.dp),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = colors.surface2,
        ) {
            DropdownMenuItem(
                text = { Text("Nueva cuenta", style = type.bodyL, color = colors.textPrimary) },
                leadingIcon = { MenuIcon(Icons.Outlined.Add) },
                onClick = {
                    expanded = false
                    onAddAccount()
                },
            )
            DropdownMenuItem(
                text = { Text("Nueva categoría", style = type.bodyL, color = colors.textPrimary) },
                leadingIcon = { MenuIcon(Icons.Outlined.Category) },
                onClick = {
                    expanded = false
                    onAddCategory()
                },
            )
        }
    }
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
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current

    Column(
        modifier = modifier.padding(horizontal = spacing.s4),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.AccountBalanceWallet,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(spacing.s4))
        Text(
            text = "Aún sin cuentas",
            style = type.headlineM,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(spacing.s2))
        Text(
            text = "Crea una para empezar a registrar movimientos",
            style = type.bodyM,
            color = colors.textSecondary,
        )
        Spacer(Modifier.height(spacing.s6))
        EmmButton(
            text = "Crear cuenta",
            onClick = onCreate,
            variant = EmmButtonVariant.Secondary,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 800)
@Composable
private fun AccountsScreenPreview() {
    EmmTheme {
        AccountsScreen(
            accounts = listOf(
                Account(accountId = "1", name = "Cuenta principal"),
                Account(accountId = "2", name = "Ahorros"),
                Account(accountId = "3", name = "Tarjeta de crédito"),
            ),
            addCategory = {},
            addAccount = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 800)
@Composable
private fun AccountsScreenEmptyPreview() {
    EmmTheme {
        AccountsScreen(
            accounts = emptyList(),
            addCategory = {},
            addAccount = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}
