package com.emm.justchill.hh.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import com.emm.domain.shared.Money
import com.emm.domain.shared.YearMonth
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.components.EmmListItem
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType
import com.emm.justchill.hh.category.findById
import com.emm.justchill.hh.report.components.MonthSelector
import com.emm.justchill.hh.shared.formatExpense
import com.emm.justchill.hh.shared.formatIncome
import com.emm.justchill.hh.shared.formatNeutral
import com.emm.justchill.hh.shared.fromCentsToSolesWith
import com.emm.justchill.hh.shared.fullLabel
import com.emm.justchill.hh.transaction.CategoryUi
import com.emm.justchill.hh.transaction.TransactionUi
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel = koinViewModel(),
    navigateToAll: () -> Unit = {},
    navigateToReport: () -> Unit = {},
) {
    val state: HomeUiState by homeViewModel.state.collectAsStateWithLifecycle()
    HomeScreen(
        homeData = state,
        navigateToAll = navigateToAll,
        navigateToReport = navigateToReport,
        onPreviousMonth = { homeViewModel.onIntent(HomeIntent.PreviousMonth) },
        onNextMonth = { homeViewModel.onIntent(HomeIntent.NextMonth) },
        onJumpToCurrent = { homeViewModel.onIntent(HomeIntent.JumpToCurrent) },
    )
}

@Composable
fun HomeScreen(
    homeData: HomeUiState,
    navigateToAll: () -> Unit = {},
    navigateToReport: () -> Unit = {},
    onPreviousMonth: () -> Unit = {},
    onNextMonth: () -> Unit = {},
    onJumpToCurrent: () -> Unit = {},
) {
    val colors = LocalEmmColors.current
    val spacing = LocalEmmSpacing.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding(),
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = spacing.s4,
                        vertical = spacing.s6,
                    ),
                verticalArrangement = Arrangement.spacedBy(spacing.s10),
            ) {
                MonthSelector(
                    label = homeData.month.fullLabel(),
                    onPrevious = onPreviousMonth,
                    onNext = onNextMonth,
                    onJumpToCurrent = if (homeData.month != YearMonth.current()) onJumpToCurrent else null,
                )
                BalanceHero(homeData.balance)
                MonthSummary(income = homeData.income, expense = homeData.spend)
                ViewReportButton(onClick = navigateToReport)
            }
        }

        item {
            RecentHeader(
                onViewAll = navigateToAll,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = spacing.s4,
                        end = spacing.s4,
                        top = spacing.s2,
                        bottom = spacing.s2,
                    ),
            )
        }

        if (homeData.lastTransactions.isEmpty()) {
            item { RecentEmpty(modifier = Modifier.fillMaxWidth().padding(vertical = spacing.s8)) }
        } else {
            items(homeData.lastTransactions, TransactionUi::transactionId) { tx ->
                EmmListItem(
                    icon = tx.category.categoryIcon,
                    title = tx.description.ifBlank { "Sin descripción" },
                    metadata = "${tx.readableDate} · ${tx.readableTime}",
                    amount = tx.amount,
                    categoryColor = tx.category.categoryColor.primary,
                )
            }
        }

        item { Spacer(Modifier.height(spacing.s8)) }
    }
}

@Composable
private fun BalanceHero(balance: Money) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current

    Column(verticalArrangement = Arrangement.spacedBy(spacing.s2)) {
        Text(
            text = "BALANCE",
            style = type.labelM,
            color = colors.textTertiary,
        )
        Text(
            text = formatNeutral(fromCentsToSolesWith(balance)),
            style = type.amountHero,
            color = colors.textPrimary,
        )
    }
}

@Composable
private fun MonthSummary(income: Money, expense: Money) {
    val spacing = LocalEmmSpacing.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.s6),
    ) {
        SummaryColumn(
            label = "INGRESOS",
            amount = formatNeutral(fromCentsToSolesWith(income)),
            modifier = Modifier.weight(1f),
        )
        SummaryColumn(
            label = "GASTOS",
            amount = formatNeutral(fromCentsToSolesWith(expense)),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SummaryColumn(
    label: String,
    amount: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing.s2),
    ) {
        Text(
            text = label,
            style = type.labelM,
            color = colors.textTertiary,
        )
        Text(
            text = amount,
            style = type.amountL,
            color = colors.textPrimary,
        )
    }
}

@Composable
private fun ViewReportButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current

    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .border(
                width = 1.dp,
                color = colors.border,
                shape = RoundedCornerShape(6.dp),
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = spacing.s5),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.s3),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ShowChart,
            contentDescription = null,
            tint = colors.textPrimary,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = "Ver reporte completo",
            style = type.labelL,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun RecentHeader(
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "RECIENTES",
            style = type.labelM,
            color = colors.textTertiary,
        )
        val interactionSource = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier.clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = dropUnlessResumed(block = onViewAll),
            ),
        ) {
            Text(
                text = "Ver todas",
                style = type.labelL,
                color = colors.textPrimary,
            )
        }
    }
}

@Composable
private fun RecentEmpty(modifier: Modifier = Modifier) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.s3),
    ) {
        Icon(
            imageVector = Icons.Outlined.Receipt,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(40.dp),
        )
        Text(
            text = "Sin transacciones recientes",
            style = type.bodyM,
            color = colors.textSecondary,
        )
    }
}

@PreviewLightDark
@Composable
private fun HomeScreenPreview() {
    EmmTheme {
        HomeScreen(
            HomeUiState(
                balance = Money(48200000L),
                income = Money(32000000L),
                spend = Money(8400000L),
                lastTransactions = listOf(
                    TransactionUi(
                        transactionId = "1",
                        type = TransactionType.Spend,
                        amount = formatExpense("84.20"),
                        description = "Mercado",
                        date = 0,
                        readableDate = "HOY",
                        readableTime = "14:30",
                        category = CategoryUi(Icons.Rounded.Category, findById("green")),
                    ),
                    TransactionUi(
                        transactionId = "2",
                        type = TransactionType.Income,
                        amount = formatIncome("3,200.00"),
                        description = "Sueldo",
                        date = 0,
                        readableDate = "HOY",
                        readableTime = "09:00",
                        category = CategoryUi(Icons.Rounded.Category, findById("gray")),
                    ),
                    TransactionUi(
                        transactionId = "3",
                        type = TransactionType.Spend,
                        amount = formatExpense("12.00"),
                        description = "Café con Sofía",
                        date = 0,
                        readableDate = "AYER",
                        readableTime = "16:48",
                        category = CategoryUi(Icons.Rounded.Category, findById("pink")),
                    ),
                ),
            )
        )
    }
}

@PreviewLightDark
@Composable
private fun HomeScreenEmptyPreview() {
    EmmTheme {
        HomeScreen(HomeUiState())
    }
}
