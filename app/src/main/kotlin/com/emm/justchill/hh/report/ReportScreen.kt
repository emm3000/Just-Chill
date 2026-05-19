package com.emm.justchill.hh.report

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Receipt
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
import com.emm.domain.shared.YearMonth
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType
import com.emm.justchill.hh.report.components.IncomeByCategoryBars
import com.emm.justchill.hh.report.components.MonthSelector
import com.emm.justchill.hh.report.components.ToggleIncomeExpense
import com.emm.justchill.hh.shared.fullLabel
import kotlinx.datetime.Month
import org.koin.androidx.compose.koinViewModel

@Composable
fun ReportScreen(
    onBack: () -> Unit = {},
    onAddTransaction: () -> Unit = {},
    vm: ReportViewModel = koinViewModel(),
    modifier: Modifier = Modifier,
) {
    val state: ReportUiState by vm.state.collectAsStateWithLifecycle()
    ReportScreen(
        state = state,
        onBack = onBack,
        onAddTransaction = onAddTransaction,
        onPreviousMonth = { vm.onIntent(ReportIntent.PreviousMonth) },
        onNextMonth = { vm.onIntent(ReportIntent.NextMonth) },
        onJumpToCurrent = { vm.onIntent(ReportIntent.JumpToCurrent) },
        onTypeSelected = { vm.onIntent(ReportIntent.SelectType(it)) },
        modifier = modifier,
    )
}

@Composable
private fun ReportScreen(
    state: ReportUiState,
    onBack: () -> Unit,
    onAddTransaction: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onJumpToCurrent: () -> Unit,
    onTypeSelected: (TransactionType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEmmColors.current
    val spacing = LocalEmmSpacing.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding(),
    ) {
        ReportTopBar(onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.s4),
            verticalArrangement = Arrangement.spacedBy(spacing.s6),
        ) {
            Spacer(Modifier.height(spacing.s2))

            MonthSelector(
                label = state.month.fullLabel(),
                onPrevious = onPreviousMonth,
                onNext = onNextMonth,
                onJumpToCurrent = if (state.month != YearMonth.current()) onJumpToCurrent else null,
            )

            ToggleIncomeExpense(
                selected = state.selectedType,
                onSelected = onTypeSelected,
            )

            if (state.isEmpty) {
                EmptyState(
                    type = state.selectedType,
                    onAddTransaction = onAddTransaction,
                )
            } else {
                TotalHeader(
                    totalFormatted = state.totalFormatted,
                    type = state.selectedType,
                    comparisonText = state.comparisonText,
                    comparisonIsPositive = state.comparisonIsPositive,
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(colors.border),
                )

                IncomeByCategoryBars(shares = state.shares)
            }

            Spacer(Modifier.height(spacing.s8))
        }
    }
}

@Composable
private fun ReportTopBar(onBack: () -> Unit) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .size(48.dp)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onBack,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Volver",
                tint = colors.textPrimary,
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            text = "Reporte",
            style = type.titleL,
            color = colors.textPrimary,
            modifier = Modifier
                .weight(1f)
                .padding(start = spacing.s2),
        )
    }
}

@Composable
private fun TotalHeader(
    totalFormatted: String,
    type: TransactionType,
    comparisonText: String?,
    comparisonIsPositive: Boolean?,
) {
    val colors = LocalEmmColors.current
    val typeTokens = LocalEmmType.current
    val spacing = LocalEmmSpacing.current

    val subtitle = when (type) {
        TransactionType.Income -> "ingresos en el mes"
        TransactionType.Spend -> "gastos en el mes"
    }

    Column(verticalArrangement = Arrangement.spacedBy(spacing.s2)) {
        Text(
            text = totalFormatted,
            style = typeTokens.amountL,
            color = colors.textPrimary,
        )
        Text(
            text = subtitle,
            style = typeTokens.bodyM,
            color = colors.textSecondary,
        )
        if (comparisonText != null) {
            val color = when (comparisonIsPositive) {
                true -> colors.success
                false -> colors.danger
                null -> colors.textSecondary
            }
            Text(
                text = comparisonText,
                style = typeTokens.labelL,
                color = color,
            )
        }
    }
}

@Composable
private fun EmptyState(
    type: TransactionType,
    onAddTransaction: () -> Unit,
) {
    val colors = LocalEmmColors.current
    val typeTokens = LocalEmmType.current
    val spacing = LocalEmmSpacing.current

    val title = when (type) {
        TransactionType.Income -> "Aún no registraste ingresos este mes"
        TransactionType.Spend -> "Aún no registraste gastos este mes"
    }
    val subtitle = when (type) {
        TransactionType.Income -> "Anota el primero y vuelve al final del mes"
        TransactionType.Spend -> "Anota el primero y vuelve al final del mes"
    }
    val cta = when (type) {
        TransactionType.Income -> "Anotar ingreso"
        TransactionType.Spend -> "Anotar gasto"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = spacing.s12),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.s3),
    ) {
        Icon(
            imageVector = Icons.Outlined.Receipt,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(48.dp),
        )
        Text(
            text = title,
            style = typeTokens.headlineM,
            color = colors.textPrimary,
        )
        Text(
            text = subtitle,
            style = typeTokens.bodyM,
            color = colors.textSecondary,
        )
        Spacer(Modifier.height(spacing.s4))
        val interactionSource = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onAddTransaction,
                )
                .background(colors.accent)
                .padding(horizontal = spacing.s5, vertical = spacing.s3),
        ) {
            Text(
                text = cta,
                style = typeTokens.labelL,
                color = colors.textOnAccent,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ReportScreenPreview() {
    EmmTheme {
        val colors = LocalEmmColors.current
        ReportScreen(
            state = ReportUiState(
                month = YearMonth(2026, Month.MAY),
                selectedType = TransactionType.Income,
                totalFormatted = "S/ 6,200",
                comparisonText = "+12% vs Abril",
                comparisonIsPositive = true,
                shares = listOf(
                    CategoryShare("1", "Sueldo", "S/ 4,500", 60, colors.catSlate),
                    CategoryShare("2", "Freelance", "S/ 1,200", 19, colors.catSage),
                    CategoryShare("3", "Ventas", "S/ 400", 6, colors.catTerracotta),
                    CategoryShare("4", "Propinas", "S/ 80", 1, colors.catOchre),
                    CategoryShare("5", "Otros", "S/ 20", 0, colors.catGraphite),
                ),
                isEmpty = false,
            ),
            onBack = {},
            onAddTransaction = {},
            onPreviousMonth = {},
            onNextMonth = {},
            onJumpToCurrent = {},
            onTypeSelected = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun ReportScreenEmptyPreview() {
    EmmTheme {
        ReportScreen(
            state = ReportUiState(
                month = YearMonth(2026, Month.MAY),
                selectedType = TransactionType.Income,
                shares = emptyList(),
                isEmpty = true,
            ),
            onBack = {},
            onAddTransaction = {},
            onPreviousMonth = {},
            onNextMonth = {},
            onJumpToCurrent = {},
            onTypeSelected = {},
        )
    }
}
