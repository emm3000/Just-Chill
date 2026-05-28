package com.emm.justchill.hh.home

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import com.emm.domain.shared.Money
import com.emm.domain.shared.YearMonth
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.InterFontFamily
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.ui.atoms.AmountHero
import com.emm.justchill.core.ui.atoms.AmountTone
import com.emm.justchill.core.ui.atoms.Eyebrow
import com.emm.justchill.core.ui.atoms.MoneyInline
import com.emm.justchill.core.ui.atoms.MonthSelector
import com.emm.justchill.hh.category.findById
import com.emm.justchill.hh.recurring.ConfirmRecurringSheet
import com.emm.justchill.hh.recurring.PendingRecurringUi
import com.emm.justchill.hh.shared.formatExpense
import com.emm.justchill.hh.shared.formatIncome
import com.emm.justchill.hh.shared.fullLabel
import com.emm.justchill.hh.shared.shortLabel
import com.emm.justchill.hh.transaction.CategoryUi
import com.emm.justchill.hh.transaction.TransactionUi
import com.emm.justchill.hh.transaction.components.TransactionRow

/**
 * VM-owning overload used by [HomeEntry] in Hh.kt.
 * Effect collection (CloseConfirmSheet, ShowError) lives in HomeEntry; sheet-open state is
 * threaded in so the effect collector can close the sheet from outside this composable.
 */
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    confirmSheetOpen: Boolean,
    onConfirmSheetOpenChange: (Boolean) -> Unit,
    navigateToAll: () -> Unit = {},
    navigateToAdd: () -> Unit = {},
    navigateToEdit: (String) -> Unit = {},
    navigateToReport: () -> Unit = {},
) {
    val state: HomeUiState by homeViewModel.state.collectAsStateWithLifecycle()
    HomeScreen(
        homeData = state,
        confirmSheetOpen = confirmSheetOpen,
        onConfirmSheetOpenChange = onConfirmSheetOpenChange,
        navigateToAll = navigateToAll,
        navigateToAdd = navigateToAdd,
        navigateToEdit = navigateToEdit,
        navigateToReport = navigateToReport,
        onPreviousMonth = { homeViewModel.onIntent(HomeIntent.PreviousMonth) },
        onNextMonth = { homeViewModel.onIntent(HomeIntent.NextMonth) },
        onIntent = homeViewModel::onIntent,
    )
}

@Composable
fun HomeScreen(
    homeData: HomeUiState,
    confirmSheetOpen: Boolean = false,
    onConfirmSheetOpenChange: (Boolean) -> Unit = {},
    navigateToAll: () -> Unit = {},
    navigateToAdd: () -> Unit = {},
    navigateToEdit: (String) -> Unit = {},
    navigateToReport: () -> Unit = {},
    onPreviousMonth: () -> Unit = {},
    onNextMonth: () -> Unit = {},
    onIntent: (HomeIntent) -> Unit = {},
) {
    when {
        homeData.isFirstLaunch -> FirstLaunchEmpty(onAddClick = navigateToAdd)

        homeData.isMonthEmpty -> MonthEmpty(
            month = homeData.month,
            onPreviousMonth = onPreviousMonth,
            onNextMonth = onNextMonth,
            onAddClick = navigateToAdd,
        )

        else -> HomeWithData(
            homeData = homeData,
            confirmSheetOpen = confirmSheetOpen,
            onConfirmSheetOpenChange = onConfirmSheetOpenChange,
            navigateToAll = navigateToAll,
            navigateToEdit = navigateToEdit,
            navigateToReport = navigateToReport,
            onPreviousMonth = onPreviousMonth,
            onNextMonth = onNextMonth,
            onIntent = onIntent,
        )
    }
}

@Composable
private fun HomeWithData(
    homeData: HomeUiState,
    confirmSheetOpen: Boolean,
    onConfirmSheetOpenChange: (Boolean) -> Unit,
    navigateToAll: () -> Unit,
    navigateToEdit: (String) -> Unit,
    navigateToReport: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onIntent: (HomeIntent) -> Unit = {},
) {
    val colors = LocalEmmColors.current

    // The selected template ID driving the sheet — stored locally for row clicks.
    // Sheet-close is driven by the parent via confirmSheetOpen = false (from CloseConfirmSheet effect).
    var confirmSheetItem by rememberSaveable { mutableStateOf<String?>(null) }

    // When the parent signals close (effect-driven), clear the local item too.
    if (!confirmSheetOpen && confirmSheetItem != null) {
        confirmSheetItem = null
    }

    val pendingMap = remember(homeData.pendingRecurringMovements) {
        homeData.pendingRecurringMovements.associateBy { it.templateId }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding(),
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp, start = 20.dp, end = 20.dp),
            ) {
                MonthSelector(
                    label = homeData.month.fullLabel(),
                    onPrev = onPreviousMonth,
                    onNext = onNextMonth,
                    modifier = Modifier.align(Alignment.Center),
                )
                ReportIconButton(
                    onClick = navigateToReport,
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
            }
        }
        item { HeroBalance(balance = homeData.balance, month = homeData.month.shortLabel()) }
        item { InOutRow(income = homeData.income, spend = homeData.spend) }

        // ---- Pendientes section — only shown when list is non-empty (Scenario 10.1 / 10.2) ----
        if (homeData.pendingRecurringMovements.isNotEmpty()) {
            item {
                PendientesHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 8.dp),
                )
            }
            items(homeData.pendingRecurringMovements, PendingRecurringUi::templateId) { pending ->
                PendingRecurringRow(
                    item = pending,
                    onClick = {
                        confirmSheetItem = pending.templateId
                        onConfirmSheetOpenChange(true)
                    },
                )
            }
        }

        item {
            RecentsHeader(
                onViewAll = navigateToAll,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 8.dp),
            )
        }
        items(homeData.lastTransactions, TransactionUi::transactionId) { tx ->
            TransactionRow(
                tx = tx,
                showDate = true,
                onClick = dropUnlessResumed { navigateToEdit(tx.transactionId) },
            )
        }
        item { Spacer(Modifier.height(16.dp)) }
    }

    // Show ConfirmRecurringSheet when an item is selected and sheet is open.
    // Sheet closes ONLY via CloseConfirmSheet effect (success) → onConfirmSheetOpenChange(false).
    // On error the sheet stays open so the snackbar is still visible with the sheet.
    if (confirmSheetOpen) {
        confirmSheetItem?.let { templateId ->
            pendingMap[templateId]?.let { item ->
                ConfirmRecurringSheet(
                    item = item,
                    onConfirm = { id, callerAmount ->
                        onIntent(HomeIntent.ConfirmRecurring(templateId = id, callerAmount = callerAmount))
                    },
                    onDismiss = {
                        confirmSheetItem = null
                        onConfirmSheetOpenChange(false)
                    },
                )
            }
        }
    }
}

@Composable
private fun PendientesHeader(modifier: Modifier = Modifier) {
    val colors = LocalEmmColors.current
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Eyebrow(text = "Pendientes")
    }
}

@Composable
private fun PendingRecurringRow(item: PendingRecurringUi, onClick: () -> Unit) {
    val colors = LocalEmmColors.current
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = dropUnlessResumed(block = onClick),
            )
            .padding(horizontal = 24.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = TextStyle(
                    fontFamily = InterFontFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.W500,
                    letterSpacing = (-0.15).sp,
                ),
                color = colors.textPrimary,
            )
            Text(
                text = "Día ${item.dayOfMonth}",
                style = TextStyle(
                    fontFamily = InterFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.W400,
                ),
                color = colors.textTertiary,
            )
        }
        Text(
            text = item.formattedAmount,
            style = TextStyle(
                fontFamily = InterFontFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.W600,
                letterSpacing = (-0.1).sp,
            ),
            color = when (item.type) {
                TransactionType.Income -> colors.success
                TransactionType.Spend -> colors.danger
            },
        )
    }
}

@Composable
private fun HeroBalance(balance: Money, month: String) {
    val balanceDouble = balance.cents.toDouble() / 100.0
    val tone = when {
        balance.cents > 0L -> AmountTone.Pos
        balance.cents < 0L -> AmountTone.Neg
        else -> AmountTone.Mute
    }

    Column(modifier = Modifier.padding(top = 30.dp, start = 24.dp, end = 24.dp)) {
        Eyebrow(text = "Balance · ${month.lowercase()}")
        Spacer(Modifier.height(12.dp))
        AmountHero(value = balanceDouble, size = 52.sp, tone = tone)
    }
}

@Composable
private fun InOutRow(income: Money, spend: Money) {
    val colors = LocalEmmColors.current

    val incomeDouble = income.cents.toDouble() / 100.0
    val spendDouble = spend.cents.toDouble() / 100.0

    Row(
        modifier = Modifier.padding(top = 24.dp, start = 24.dp, end = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        Column {
            Eyebrow(text = "Entró", color = colors.textDisabled)
            Spacer(Modifier.height(4.dp))
            MoneyInline(value = incomeDouble, color = colors.textPrimary)
        }

        Box(
            modifier = Modifier
                .width(1.dp)
                .height(28.dp)
                .background(colors.border),
        )

        Column {
            Eyebrow(text = "Salió", color = colors.textDisabled)
            Spacer(Modifier.height(4.dp))
            MoneyInline(value = spendDouble, color = colors.textSecondary)
        }
    }
}

@Composable
private fun RecentsHeader(onViewAll: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalEmmColors.current
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Eyebrow(text = "Recientes")
        Box(
            modifier = Modifier.clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = dropUnlessResumed(block = onViewAll),
            ),
        ) {
            Text(
                text = "Ver todas",
                style = TextStyle(
                    fontFamily = InterFontFamily,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.W500,
                    letterSpacing = 0.sp,
                ),
                color = colors.textSecondary,
            )
        }
    }
}

@Composable
private fun FirstLaunchEmpty(onAddClick: () -> Unit) {
    val colors = LocalEmmColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AccentSparkleTile()
            Spacer(Modifier.height(28.dp))
            EmptyTitle(text = "Tu plata empieza acá.")
            Spacer(Modifier.height(10.dp))
            EmptyBody(text = "Anotá tu primer Yape, sueldo o gasto.\nTe toma 15 segundos.")
            Spacer(Modifier.height(28.dp))
            FilledAccentCta(
                label = "Anotar el primero",
                onClick = onAddClick,
                leadingIcon = Icons.Outlined.Add,
            )
        }
    }
}

@Composable
private fun MonthEmpty(
    month: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onAddClick: () -> Unit,
) {
    val colors = LocalEmmColors.current
    val prevName = remember(month) { month.previous().shortLabel().lowercase() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp, start = 20.dp, end = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            MonthSelector(
                label = month.fullLabel(),
                onPrev = onPreviousMonth,
                onNext = onNextMonth,
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    tint = colors.textTertiary,
                    modifier = Modifier.size(26.dp),
                )
                Spacer(Modifier.height(20.dp))
                EmptyTitle(text = "${month.shortLabel()} aún vacío.")
                Spacer(Modifier.height(10.dp))
                EmptyBody(text = "No registraste nada en este mes\ntodavía.")
                Spacer(Modifier.height(22.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilledAccentCta(
                        label = "Anotar acá",
                        onClick = onAddClick,
                        leadingIcon = Icons.Outlined.Add,
                    )
                    OutlinedCta(
                        label = "Ir a $prevName",
                        onClick = onPreviousMonth,
                        leadingIcon = Icons.AutoMirrored.Outlined.ArrowBack,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportIconButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalEmmColors.current
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(48.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = dropUnlessResumed(block = onClick),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Insights,
            contentDescription = "Ver reporte",
            tint = colors.textPrimary,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun AccentSparkleTile() {
    val colors = LocalEmmColors.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.accentMuted),
    ) {
        Icon(
            imageVector = Icons.Outlined.AutoAwesome,
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun EmptyTitle(text: String) {
    val colors = LocalEmmColors.current
    Text(
        text = text,
        style = TextStyle(
            fontFamily = InterFontFamily,
            fontSize = 22.sp,
            fontWeight = FontWeight.W700,
            letterSpacing = (-0.4).sp,
        ),
        color = colors.textPrimary,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}

@Composable
private fun EmptyBody(text: String) {
    val colors = LocalEmmColors.current
    Text(
        text = text,
        style = TextStyle(
            fontFamily = InterFontFamily,
            fontSize = 15.sp,
            fontWeight = FontWeight.W400,
            lineHeight = 22.sp,
            letterSpacing = (-0.15).sp,
        ),
        color = colors.textSecondary,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        modifier = Modifier.widthIn(max = 280.dp),
    )
}

@Composable
private fun FilledAccentCta(
    label: String,
    onClick: () -> Unit,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    val colors = LocalEmmColors.current
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(colors.accent)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 20.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = leadingIcon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = label,
            style = TextStyle(
                fontFamily = InterFontFamily,
                fontSize = 15.sp,
                fontWeight = FontWeight.W600,
                letterSpacing = (-0.15).sp,
            ),
            color = Color.White,
        )
    }
}

@Composable
private fun OutlinedCta(
    label: String,
    onClick: () -> Unit,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    val colors = LocalEmmColors.current
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(colors.surface1)
            .border(1.dp, colors.border, RoundedCornerShape(999.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = leadingIcon,
            contentDescription = null,
            tint = colors.textPrimary,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = label,
            style = TextStyle(
                fontFamily = InterFontFamily,
                fontSize = 15.sp,
                fontWeight = FontWeight.W600,
                letterSpacing = (-0.15).sp,
            ),
            color = colors.textPrimary,
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
                        readableDate = "Hoy",
                        readableTime = "14:30",
                        category = CategoryUi(Icons.Rounded.Category, findById("green")),
                    ),
                    TransactionUi(
                        transactionId = "2",
                        type = TransactionType.Income,
                        amount = formatIncome("3,200.00"),
                        description = "Sueldo",
                        date = 0,
                        readableDate = "Hoy",
                        readableTime = "09:00",
                        category = CategoryUi(Icons.Rounded.Category, findById("gray")),
                    ),
                    TransactionUi(
                        transactionId = "3",
                        type = TransactionType.Spend,
                        amount = formatExpense("12.00"),
                        description = "Café con Sofía",
                        date = 0,
                        readableDate = "Ayer",
                        readableTime = "16:48",
                        category = CategoryUi(Icons.Rounded.Category, findById("pink")),
                    ),
                ),
            ),
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
