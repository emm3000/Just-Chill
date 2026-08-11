package com.emm.justchill.hh.report

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
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emm.domain.shared.YearMonth
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType
import com.emm.justchill.core.ui.atoms.Eyebrow
import com.emm.justchill.core.ui.atoms.MonthSelector
import com.emm.justchill.core.ui.atoms.SegmentOption
import com.emm.justchill.core.ui.atoms.Segmented
import com.emm.justchill.hh.report.components.CategoryBarsCard
import com.emm.justchill.hh.report.components.ComparisonPill
import com.emm.justchill.hh.report.components.MonthPickerSheet
import com.emm.justchill.hh.report.components.ShareReportButton
import com.emm.justchill.hh.report.components.TodayPill
import com.emm.justchill.hh.report.components.ToggleIncomeExpense
import com.emm.justchill.hh.report.components.TotalAmountHero
import com.emm.justchill.hh.report.components.TrendsContent
import com.emm.justchill.hh.shared.monthLabel
import com.emm.justchill.hh.shared.monthYearLabel
import kotlinx.datetime.Month
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ReportScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onAddTransaction: () -> Unit = {},
    onShareText: (String) -> Unit = {},
    vm: ReportViewModel = koinViewModel(),
) {
    val state: ReportUiState by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(vm) {
        vm.effect.collect { effect ->
            when (effect) {
                is ReportEffect.ShowError -> { /* snackbar handled by Hh.kt root */ }

                is ReportEffect.ShareReport -> onShareText(effect.text)
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
        onSelectMonth = { vm.onIntent(ReportIntent.SelectMonth(it)) },
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
    onSelectMonth: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEmmColors.current
    val spacing = LocalEmmSpacing.current

    var showMonthSheet by rememberSaveable { mutableStateOf(false) }

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
                    onLabelClick = { showMonthSheet = true },
                )

                ReportTab.Tendencias -> TrendsContent(trends = state.trends)
            }

            Spacer(Modifier.height(spacing.s8))
        }
    }

    if (showMonthSheet) {
        MonthPickerSheet(
            current = state.month,
            onSelect = { selected ->
                onSelectMonth(selected)
                showMonthSheet = false
            },
            onDismiss = { showMonthSheet = false },
        )
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
    onLabelClick: () -> Unit,
) {
    val spacing = LocalEmmSpacing.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MonthSelector(
            label = state.month.monthYearLabel(),
            onPrevious = onPreviousMonth,
            onNext = onNextMonth,
            onLabelClick = onLabelClick,
        )
        // Answered by the ViewModel, which holds the injected clock and zone. Resolving "what month
        // is it" here read the device instead, so the pill could disagree with the month beside it.
        if (!state.isCurrentMonth) {
            Spacer(Modifier.size(spacing.s2))
            TodayPill(onClick = onJumpToCurrent)
        }
    }

    if (state.isMonthEmpty) {
        MonthEmptyState(month = state.month.monthYearLabel())
    } else {
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
}

@Composable
private fun TotalHeroBlock(state: ReportUiState) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current

    val eyebrowText = when (state.selectedType) {
        TransactionType.Income -> "TOTAL INGRESOS · ${state.month.monthLabel().uppercase()}"
        TransactionType.Spend -> "TOTAL GASTOS · ${state.month.monthLabel().uppercase()}"
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
                    directionUp = state.comparisonDirectionUp ?: true,
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
            textAlign = TextAlign.Center,
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
private fun MonthEmptyState(month: String) {
    val colors = LocalEmmColors.current
    val typeTokens = LocalEmmType.current
    val spacing = LocalEmmSpacing.current
    val tileShape = RoundedCornerShape(12.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = spacing.s12),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.s3),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(tileShape)
                .background(colors.surface1)
                .border(width = 1.dp, color = colors.border, shape = tileShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = "Sin movimientos en $month",
            style = typeTokens.headlineM,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Anota un gasto o ingreso para empezar a ver tu reporte de este mes.",
            style = typeTokens.bodyM,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
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

@Preview
@Composable
private fun ReportScreenMesPreview() {
    EmmTheme {
        ReportScreen(
            state = ReportUiState(
                month = YearMonth(2026, Month.MAY),
                // Stated, not inherited: the default is false, which would draw TodayPill on every
                // canvas regardless of the month above. May is this preview set's "now".
                isCurrentMonth = true,
                selectedType = TransactionType.Income,
                selectedTab = ReportTab.Mes,
                totalFormatted = "S/ 6,200.00",
                comparisonText = "vs. abril",
                comparisonDirectionUp = true,
                comparisonIsPositive = true,
                comparisonAmountFormatted = "S/ 660",
                comparisonPercent = 12,
                shares = listOf(
                    CategoryShare("1", "Sueldo", "S/ 4,500.00", 73, "red"),
                    CategoryShare("2", "Freelance", "S/ 1,200.00", 19, "blue"),
                    CategoryShare("3", "Ventas IG", "S/ 380.00", 6, "green"),
                    CategoryShare("4", "Yapes", "S/ 120.00", 2, "orange"),
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
            onSelectMonth = {},
        )
    }
}

@Preview
@Composable
private fun ReportScreenMesGastosPreview() {
    EmmTheme {
        ReportScreen(
            state = ReportUiState(
                month = YearMonth(2026, Month.MARCH),
                // March against a "now" of May: a browsed past month, so TodayPill belongs on this
                // canvas. The one preview that renders it.
                isCurrentMonth = false,
                selectedType = TransactionType.Spend,
                selectedTab = ReportTab.Mes,
                totalFormatted = "S/ 4,580.00",
                comparisonText = "vs. marzo",
                comparisonDirectionUp = false,
                comparisonIsPositive = true,
                comparisonAmountFormatted = "S/ 220",
                comparisonPercent = 5,
                shares = listOf(
                    CategoryShare("1", "Comida", "S/ 1,840.00", 40, "red"),
                    CategoryShare("2", "Transporte", "S/ 920.00", 20, "blue"),
                    CategoryShare("3", "Ocio", "S/ 680.00", 15, "purple"),
                    CategoryShare("4", "Servicios", "S/ 540.00", 12, "orange"),
                    CategoryShare("5", "Salud", "S/ 340.00", 7, "green"),
                    CategoryShare("6", "Sin categoría", "S/ 260.00", 6, "gray"),
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
            onSelectMonth = {},
        )
    }
}

@Preview
@Composable
private fun ReportScreenEmptyPreview() {
    EmmTheme {
        ReportScreen(
            state = ReportUiState(
                month = YearMonth(2026, Month.MAY),
                // The current month, and empty — landing on a month with nothing in it yet.
                isCurrentMonth = true,
                selectedType = TransactionType.Income,
                shares = emptyList(),
                isEmpty = true,
                isMonthEmpty = true,
            ),
            onBack = {},
            onAddTransaction = {},
            onPreviousMonth = {},
            onNextMonth = {},
            onJumpToCurrent = {},
            onTypeSelect = {},
            onTabSelect = {},
            onShare = {},
            onSelectMonth = {},
        )
    }
}
