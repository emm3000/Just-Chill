package com.emm.justchill.hh.account

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.ui.graphics.vector.ImageVector
import com.emm.justchill.core.domain.account.AccountType

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
