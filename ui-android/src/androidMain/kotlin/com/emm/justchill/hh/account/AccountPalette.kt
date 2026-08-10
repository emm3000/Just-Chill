package com.emm.justchill.hh.account

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.emm.domain.account.AccountType
import com.emm.justchill.core.theme.EmmColors

/**
 * Per-account dot color, derived from the account name (so "Yape" always maps
 * to the same swatch regardless of where it appears). Used by the picker chip
 * dot, the Cuentas tab icon tile, and any preview surface.
 */
fun accountDotColor(name: String, colors: EmmColors): Color {
    val lower = name.lowercase()
    return when {
        "yape" in lower -> colors.catMauve
        "plin" in lower -> colors.catSage
        "bcp" in lower -> colors.catSlate
        "bbva" in lower -> colors.catTerracotta
        "interbank" in lower -> colors.catOchre
        "scotiabank" in lower -> colors.catMauve
        "efectivo" in lower || "cash" in lower -> colors.catOchre
        else -> colors.catGraphite
    }
}

fun AccountType.toIcon(): ImageVector = when (this) {
    AccountType.Bank -> Icons.Outlined.AccountBalance
    AccountType.Cash -> Icons.Outlined.Payments
    AccountType.CreditCard -> Icons.Outlined.CreditCard
    AccountType.Investment -> Icons.Outlined.TrendingUp
    AccountType.Wallet -> Icons.Outlined.AccountBalanceWallet
}

fun AccountType.toLabel(): String = when (this) {
    AccountType.Bank -> "Banco"
    AccountType.Cash -> "Efectivo"
    AccountType.CreditCard -> "Tarjeta de crédito"
    AccountType.Investment -> "Inversión"
    AccountType.Wallet -> "Billetera digital"
}
