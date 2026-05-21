package com.emm.justchill.hh.report

import android.content.Intent
import android.content.Intent.EXTRA_TEXT
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emm.domain.shared.YearMonth
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType
import com.emm.justchill.core.ui.atoms.Eyebrow
import com.emm.justchill.core.ui.atoms.SegmentOption
import com.emm.justchill.core.ui.atoms.Segmented
import com.emm.justchill.hh.report.components.CategoryBarsCard
import com.emm.justchill.hh.report.components.ComparisonPill
import com.emm.justchill.hh.report.components.MonthSelector
import com.emm.justchill.hh.report.components.ShareReportButton
import com.emm.justchill.hh.report.components.TodayPill
import com.emm.justchill.hh.report.components.ToggleIncomeExpense
import com.emm.justchill.hh.report.components.TotalAmountHero
import com.emm.justchill.hh.report.components.TrendsContent
import com.emm.justchill.hh.shared.fullLabel
import com.emm.justchill.hh.shared.shortLabel
import kotlinx.datetime.Month
import org.koin.androidx.compose.koinViewModel

@Composable
fun ReportScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onAddTransaction: () -> Unit = {},
    vm: ReportViewModel = koinViewModel(),
) {
    val state: ReportUiState by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(vm) {
        vm.effect.collect { effect ->
            when (effect) {
                is ReportEffect.ShowError -> { /* snackbar handled by Hh.kt root */ }

                is ReportEffect.ShareReport -> {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(EXTRA_TEXT, effect.text)
                    }
                    context.startActivity(Intent.createChooser(intent, "Compartir reporte"))
                }
            }
        }
    }

    ReportScreen(
        state = state,
        onBack = onBack,
        onAddTransaction = onAddTransaction,
        onPreviousMonth = { vm.onIntent(ReportIntent.PreviousMonth) },
        onNextMonth = { vm.onIntent(ReportIntent.NextMonth) },
        onJumpToCurrent = { vm.onIntent(ReportIntent.JumpToCurrent) },
        onTypeSelect = { vm.onIntent(ReportIntent.SelectType(it)) },
        onTabSelect = { vm.onIntent(ReportIntent.SelectTab(it)) },
        onShare = { vm.onIntent(ReportIntent.ShareReport) },
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
    onTypeSelect: (TransactionType) -> Unit,
    onTabSelect: (ReportTab) -> Unit,
    onShare: () -> Unit,
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
        ReportTopBar(onBack = onBack, onShare = onShare)

        val tabOptions = listOf(
            SegmentOption(ReportTab.Mes, "Mes"),
            SegmentOption(ReportTab.Tendencias, "Tendencias 6m"),
        )
        Segmented(
            options = tabOptions,
            selected = state.selectedTab,
            onSelect = onTabSelect,
            modifier = Modifier.padding(horizontal = spacing.s4),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.s4),
            verticalArrangement = Arrangement.spacedBy(spacing.s5),
        ) {
            Spacer(Modifier.height(spacing.s4))

            when (state.selectedTab) {
                ReportTab.Mes -> MesContent(
                    state = state,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                    onJumpToCurrent = onJumpToCurrent,
                    onTypeSelect = onTypeSelect,
                    onAddTransaction = onAddTransaction,
                    onShare = onShare,
                )

                ReportTab.Tendencias -> TrendsContent(trends = state.trends)
            }

            Spacer(Modifier.height(spacing.s8))
        }
    }
}

@Composable
private fun MesContent(
    state: ReportUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onJumpToCurrent: () -> Unit,
    onTypeSelect: (TransactionType) -> Unit,
    onAddTransaction: () -> Unit,
    onShare: () -> Unit,
) {
    val spacing = LocalEmmSpacing.current
    val isCurrentMonth = state.month == YearMonth.current()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MonthSelector(
            label = state.month.fullLabel(),
            onPrevious = onPreviousMonth,
            onNext = onNextMonth,
        )
        if (!isCurrentMonth) {
            Spacer(Modifier.size(spacing.s2))
            TodayPill(onClick = onJumpToCurrent)
        }
    }

    ToggleIncomeExpense(
        selected = state.selectedType,
        onSelect = onTypeSelect,
    )

    if (state.isEmpty) {
        EmptyState(
            type = state.selectedType,
            onAddTransaction = onAddTransaction,
        )
    } else {
        TotalHeroBlock(state = state)

        CategoryBarsCard(
            shares = state.shares,
            movementCount = state.movementCount,
            averageFormatted = state.averageFormatted,
        )

        ShareReportButton(onClick = onShare)
    }
}

@Composable
private fun TotalHeroBlock(state: ReportUiState) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current

    val eyebrowText = when (state.selectedType) {
        TransactionType.Income -> "TOTAL INGRESOS · ${state.month.shortLabel().uppercase()}"
        TransactionType.Spend -> "TOTAL GASTOS · ${state.month.shortLabel().uppercase()}"
    }

    Column(verticalArrangement = Arrangement.spacedBy(spacing.s2)) {
        Eyebrow(text = eyebrowText)
        TotalAmountHero(
            totalFormatted = state.totalFormatted,
            type = state.selectedType,
        )

        val comparisonAmt = state.comparisonAmountFormatted
        val comparisonTxt = state.comparisonText
        if (comparisonAmt != null && comparisonTxt != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.s2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ComparisonPill(
                    absoluteDeltaFormatted = comparisonAmt,
                    percent = state.comparisonPercent,
                    isPositive = state.comparisonIsPositive ?: true,
                )
                Text(
                    text = comparisonTxt,
                    style = type.bodyM,
                    color = colors.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun ReportTopBar(onBack: () -> Unit, onShare: () -> Unit) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = spacing.s4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TopBarTile(
            icon = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = "Volver",
            onClick = onBack,
        )
        Text(
            text = "Reporte",
            style = type.titleL,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        TopBarTile(
            icon = Icons.Outlined.IosShare,
            contentDescription = "Compartir reporte",
            onClick = onShare,
        )
    }
}

@Composable
private fun TopBarTile(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    val colors = LocalEmmColors.current
    val shape = RoundedCornerShape(12.dp)
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(shape)
            .background(colors.surface1)
            .border(width = 1.dp, color = colors.border, shape = shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = colors.textPrimary,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun EmptyState(type: TransactionType, onAddTransaction: () -> Unit) {
    val colors = LocalEmmColors.current
    val typeTokens = LocalEmmType.current
    val spacing = LocalEmmSpacing.current

    val title = when (type) {
        TransactionType.Income -> "Aún no registraste ingresos este mes"
        TransactionType.Spend -> "Aún no registraste gastos este mes"
    }
    val subtitle = "Anota el primero y vuelve al final del mes"
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
                .clip(RoundedCornerShape(6.dp))
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
private fun ReportScreenMesPreview() {
    EmmTheme {
        val colors = LocalEmmColors.current
        ReportScreen(
            state = ReportUiState(
                month = YearMonth(2026, Month.MAY),
                selectedType = TransactionType.Income,
                selectedTab = ReportTab.Mes,
                totalFormatted = "S/ 6,200.00",
                comparisonText = "vs. abril",
                comparisonIsPositive = true,
                comparisonAmountFormatted = "S/ 660",
                comparisonPercent = 12,
                shares = listOf(
                    CategoryShare("1", "Sueldo", "S/ 4,500.00", 73, colors.catTerracotta),
                    CategoryShare("2", "Freelance", "S/ 1,200.00", 19, colors.catSlate),
                    CategoryShare("3", "Ventas IG", "S/ 380.00", 6, colors.catSage),
                    CategoryShare("4", "Yapes", "S/ 120.00", 2, colors.catOchre),
                ),
                isEmpty = false,
                movementCount = 12,
                averageFormatted = "S/ 517",
            ),
            onBack = {},
            onAddTransaction = {},
            onPreviousMonth = {},
            onNextMonth = {},
            onJumpToCurrent = {},
            onTypeSelect = {},
            onTabSelect = {},
            onShare = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun ReportScreenMesGastosPreview() {
    EmmTheme {
        val colors = LocalEmmColors.current
        ReportScreen(
            state = ReportUiState(
                month = YearMonth(2026, Month.MARCH),
                selectedType = TransactionType.Spend,
                selectedTab = ReportTab.Mes,
                totalFormatted = "S/ 4,580.00",
                comparisonText = "vs. marzo",
                comparisonIsPositive = false,
                comparisonAmountFormatted = "S/ 220",
                comparisonPercent = 5,
                shares = listOf(
                    CategoryShare("1", "Comida", "S/ 1,840.00", 40, colors.catTerracotta),
                    CategoryShare("2", "Transporte", "S/ 920.00", 20, colors.catSlate),
                    CategoryShare("3", "Ocio", "S/ 680.00", 15, colors.catMauve),
                    CategoryShare("4", "Servicios", "S/ 540.00", 12, colors.catOchre),
                    CategoryShare("5", "Salud", "S/ 340.00", 7, colors.catSage),
                    CategoryShare("6", "Sin categoría", "S/ 260.00", 6, colors.catGraphite),
                ),
                isEmpty = false,
                movementCount = 26,
                averageFormatted = "S/ 176",
            ),
            onBack = {},
            onAddTransaction = {},
            onPreviousMonth = {},
            onNextMonth = {},
            onJumpToCurrent = {},
            onTypeSelect = {},
            onTabSelect = {},
            onShare = {},
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
            onTypeSelect = {},
            onTabSelect = {},
            onShare = {},
        )
    }
}
