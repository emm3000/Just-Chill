package com.emm.justchill.core.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.emm.justchill.core.domain.account.Account
import com.emm.justchill.core.domain.account.AccountType
import com.emm.justchill.core.ui.atoms.IconTile
import com.emm.justchill.core.ui.atoms.IconTileSize
import com.emm.justchill.core.ui.atoms.SheetDragHandle
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.EmmRadii
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.EmmType
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmRadii
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmType
import kotlinx.coroutines.launch

@Composable
fun AccountPickerSheet(
    accounts: List<Account>,
    selectedAccountId: String?,
    onSelect: (Account) -> Unit,
    onDismiss: () -> Unit,
    onAddNew: (() -> Unit)? = null,
) {
    val colors: EmmColors = LocalEmmColors.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val radii: EmmRadii = LocalEmmRadii.current
    val type: EmmType = LocalEmmType.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.bg,
        contentWindowInsets = { WindowInsets.navigationBars },
        dragHandle = { SheetDragHandle() },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = spacing.s6, end = spacing.s2, bottom = spacing.s4),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = "Selecciona cuenta", style = type.titleM, color = colors.textPrimary)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(spacing.s12)
                    .clip(CircleShape)
                    .clickable(onClick = onDismiss),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(spacing.s8)
                        .clip(CircleShape)
                        .background(colors.surface1)
                        .border(spacing.hairline, colors.border, CircleShape),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Cerrar",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(spacing.s3),
                    )
                }
            }
        }

        if (accounts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.s6),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Sin cuentas. Cierra y crea una primero.",
                    style = type.bodyM,
                    color = colors.textTertiary,
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(accounts, key = { it.accountId.value }) { account ->
                    val isActive = account.accountId.value == selectedAccountId
                    AccountRow(
                        account = account,
                        isActive = isActive,
                        onClick = {
                            onSelect(account)
                            onDismiss()
                        },
                    )
                }
            }
        }

        if (onAddNew != null) {
            val onAdd: () -> Unit = onAddNew
            val addButtonShape: RoundedCornerShape = radii.rM
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = spacing.s4, end = spacing.s4, top = spacing.s3, bottom = spacing.s4)
                    .height(spacing.s12)
                    .clip(addButtonShape)
                    .border(spacing.hairline, colors.borderFocus, addButtonShape)
                    // hide() does not fire onDismissRequest, so the caller's "sheet is open" flag
                    // stays set and the picker is back on screen when the user returns.
                    .clickable {
                        scope.launch {
                            sheetState.hide()
                            onAdd()
                        }
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    tint = colors.textPrimary,
                    modifier = Modifier.size(spacing.s3),
                )
                Spacer(Modifier.size(spacing.s2))
                Text(
                    text = "Nueva cuenta",
                    style = type.labelL.copy(fontWeight = FontWeight.W600),
                    color = colors.textPrimary,
                )
            }
        } else {
            Spacer(Modifier.height(spacing.s4))
        }
    }
}

@Composable
private fun AccountRow(account: Account, isActive: Boolean, onClick: () -> Unit) {
    val colors: EmmColors = LocalEmmColors.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val type: EmmType = LocalEmmType.current
    val icon: ImageVector = when (account.type) {
        AccountType.Bank -> Icons.Outlined.AccountBalance
        AccountType.Cash -> Icons.Outlined.AttachMoney
        AccountType.CreditCard -> Icons.Outlined.CreditCard
        AccountType.Investment -> Icons.Outlined.AccountBalance
        AccountType.Wallet -> Icons.Outlined.AccountBalanceWallet
    }

    val typeLabel = when (account.type) {
        AccountType.Bank -> "Banco"
        AccountType.Cash -> "Efectivo"
        AccountType.CreditCard -> "Crédito"
        AccountType.Investment -> "Inversión"
        AccountType.Wallet -> "Billetera"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isActive) colors.surface1 else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.s6, vertical = spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.s4),
    ) {
        IconTile(icon = icon, size = IconTileSize.Md)

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = account.name,
                style = type.titleM.copy(fontWeight = FontWeight.W500),
                color = colors.textPrimary,
            )
            Text(
                text = typeLabel,
                style = type.caption.copy(fontWeight = FontWeight.W500),
                color = colors.textTertiary,
            )
        }

        if (isActive) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(spacing.s6)
                    .clip(CircleShape)
                    .background(colors.surface3),
            ) {
                Icon(
                    imageVector = Icons.Outlined.AccountBalanceWallet,
                    contentDescription = null,
                    tint = colors.textPrimary,
                    modifier = Modifier.size(spacing.s3),
                )
            }
        }
    }
}
