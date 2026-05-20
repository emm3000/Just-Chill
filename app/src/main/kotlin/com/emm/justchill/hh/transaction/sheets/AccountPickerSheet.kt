package com.emm.justchill.hh.transaction.sheets

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.domain.account.Account
import com.emm.domain.account.AccountType
import com.emm.justchill.core.theme.InterFontFamily
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.ui.atoms.IconTile
import com.emm.justchill.core.ui.atoms.IconTileSize
import com.emm.justchill.core.ui.atoms.IconTileTone
import com.emm.justchill.core.ui.atoms.SheetDragHandle
import kotlinx.coroutines.launch

/**
 * Bottom sheet for selecting an account.
 *
 * @param accounts          List of accounts to display.
 * @param selectedAccountId Currently selected account id (or null).
 * @param onSelect       Called when user taps an account row.
 * @param onAddNew          When non-null, shows a dashed "+ Nueva cuenta" button.
 * @param onDismiss         Called to dismiss the sheet.
 */
@Composable
fun AccountPickerSheet(
    accounts: List<Account>,
    selectedAccountId: String?,
    onSelect: (Account) -> Unit,
    onDismiss: () -> Unit,
    onAddNew: (() -> Unit)? = null,
) {
    val colors = LocalEmmColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.bg,
        contentWindowInsets = { WindowInsets.navigationBars },
        dragHandle = { SheetDragHandle() },
    ) {
        // Title row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 16.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Selecciona cuenta",
                fontSize = 15.sp,
                fontWeight = FontWeight.W600,
                fontFamily = InterFontFamily,
                color = colors.textPrimary,
                letterSpacing = (-0.15).sp,
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(colors.surface1)
                    .border(1.dp, colors.border, CircleShape)
                    .clickable(onClick = onDismiss),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Cerrar",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(13.dp),
                )
            }
        }

        if (accounts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Sin cuentas. Cierra y crea una primero.",
                    fontSize = 13.sp,
                    color = colors.textTertiary,
                    fontFamily = InterFontFamily,
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(accounts, key = { it.accountId.value }) { account ->
                    val isActive = account.accountId.value == selectedAccountId
                    val swatchColor = accountSwatchColor(
                        name = account.name,
                        type = account.type,
                        colors = colors,
                    )
                    AccountRow(
                        account = account,
                        isActive = isActive,
                        swatchColor = swatchColor,
                        accentColor = colors.accent,
                        textPrimary = colors.textPrimary,
                        textTertiary = colors.textTertiary,
                        activeBg = colors.surface1,
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
            val dashedShape = RoundedCornerShape(12.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp)
                    .height(46.dp)
                    .clip(dashedShape)
                    // Dashed border approximated with a solid borderFocus — true dashed not
                    // natively supported in Compose without Canvas; close enough for SR-3.
                    .border(1.dp, colors.borderFocus, dashedShape)
                    // Animate the sheet hide first so it slides out cleanly; THEN navigate.
                    // hide() does not trigger onDismissRequest, so the parent's showAccountSheet
                    // stays true and the sheet auto-re-opens when the user returns.
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
                    modifier = Modifier.size(13.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = "Nueva cuenta",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.W600,
                    fontFamily = InterFontFamily,
                    color = colors.textPrimary,
                )
            }
        } else {
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AccountRow(
    account: Account,
    isActive: Boolean,
    swatchColor: Color,
    accentColor: Color,
    textPrimary: Color,
    textTertiary: Color,
    activeBg: Color,
    onClick: () -> Unit,
) {
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
            .background(if (isActive) activeBg else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        IconTile(
            icon = icon,
            size = IconTileSize.Md,
            tone = IconTileTone.Swatch,
            swatch = swatchColor,
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = account.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.W500,
                fontFamily = InterFontFamily,
                color = textPrimary,
                letterSpacing = (-0.15).sp,
            )
            Text(
                text = typeLabel,
                fontSize = 11.sp,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.W500,
                color = textTertiary,
            )
        }

        if (isActive) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(accentColor),
            ) {
                Icon(
                    imageVector = Icons.Outlined.AccountBalanceWallet,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
    }
}

/**
 * Maps a well-known account name (case-insensitive) to a design-system category swatch color.
 * Falls back to [EmmColors.catGraphite] for unknown names.
 */
@Composable
internal fun accountSwatchColor(
    name: String,
    type: AccountType,
    colors: com.emm.justchill.core.theme.EmmColors,
): Color {
    val lower = name.lowercase()
    return when {
        "yape" in lower -> colors.catMauve
        "plin" in lower -> colors.catSage
        "bcp" in lower -> colors.catSlate
        "bbva" in lower -> colors.catTerracotta
        "interbank" in lower -> colors.catOchre
        "scotiabank" in lower -> colors.catMauve
        "efectivo" in lower || type == AccountType.Cash -> colors.catOchre
        type == AccountType.CreditCard -> colors.catTerracotta
        type == AccountType.Investment -> colors.catSage
        else -> colors.catGraphite
    }
}
