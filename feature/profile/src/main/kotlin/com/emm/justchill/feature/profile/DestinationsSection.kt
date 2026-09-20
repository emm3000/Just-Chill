package com.emm.justchill.feature.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.emm.justchill.core.domain.shared.Money

internal data class ProfileDestinationActions(
    val onTransactionsClick: () -> Unit,
    val onReportClick: () -> Unit,
    val onAccountsClick: () -> Unit,
    val onCategoriesClick: () -> Unit,
    val onRecurringClick: () -> Unit,
    val onLoansClick: () -> Unit,
)

@Composable
internal fun DestinationsSection(
    categoryCount: Int,
    incomeCategoryCount: Int,
    recurringCount: Int,
    recurringMonthlyOutflow: Money,
    destinations: ProfileDestinationActions,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        ProfileGroup {
            ProfileRow(
                icon = Icons.AutoMirrored.Outlined.List,
                label = "Movimientos",
                meta = "",
                metaIsPrimary = false,
                onClick = destinations.onTransactionsClick,
            )
            ProfileRow(
                icon = Icons.Outlined.Insights,
                label = "Reporte del mes",
                meta = "",
                metaIsPrimary = false,
                onClick = destinations.onReportClick,
            )
            ProfileRow(
                icon = Icons.Outlined.AccountBalanceWallet,
                label = "Cuentas",
                meta = "",
                metaIsPrimary = false,
                onClick = destinations.onAccountsClick,
            )
            ProfileRow(
                icon = Icons.Outlined.Category,
                label = "Categorías",
                meta = categoriesMetaText(categoryCount, incomeCategoryCount),
                metaIsPrimary = false,
                onClick = destinations.onCategoriesClick,
            )
            ProfileRow(
                icon = Icons.Outlined.Repeat,
                label = "Recurrentes",
                meta = recurringMetaText(recurringCount, recurringMonthlyOutflow),
                metaIsPrimary = false,
                onClick = destinations.onRecurringClick,
            )
            ProfileRow(
                icon = Icons.Outlined.People,
                label = "Préstamos",
                meta = "",
                metaIsPrimary = false,
                onClick = destinations.onLoansClick,
            )
        }
    }
}
