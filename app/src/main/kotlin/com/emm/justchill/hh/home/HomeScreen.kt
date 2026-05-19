package com.emm.justchill.hh.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.emm.justchill.core.theme.LocalEmmRadii
import com.emm.justchill.core.theme.LocalEmmType
import com.emm.justchill.core.theme.PlexMonoFontFamily
import com.emm.justchill.core.ui.atoms.AmountHero
import com.emm.justchill.core.ui.atoms.AmountTone
import com.emm.justchill.core.ui.atoms.Eyebrow
import com.emm.justchill.core.ui.atoms.IconTile
import com.emm.justchill.core.ui.atoms.IconTileSize
import com.emm.justchill.core.ui.atoms.IconTileTone
import com.emm.justchill.core.ui.atoms.MoneyInline
import com.emm.justchill.core.ui.atoms.MonthSelector
import com.emm.justchill.core.ui.atoms.Pill
import com.emm.justchill.core.ui.atoms.PillTone
import com.emm.justchill.hh.category.findById
import com.emm.justchill.hh.shared.formatExpense
import com.emm.justchill.hh.shared.formatIncome
import com.emm.justchill.hh.shared.fullLabel
import com.emm.justchill.hh.shared.shortLabel
import com.emm.justchill.hh.transaction.CategoryUi
import com.emm.justchill.hh.transaction.TransactionUi
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel = koinViewModel(),
    navigateToAll: () -> Unit = {},
    navigateToReport: () -> Unit = {},
    navigateToAdd: () -> Unit = {},
) {
    val state: HomeUiState by homeViewModel.state.collectAsStateWithLifecycle()
    HomeScreen(
        homeData = state,
        navigateToAll = navigateToAll,
        navigateToReport = navigateToReport,
        navigateToAdd = navigateToAdd,
        onPreviousMonth = { homeViewModel.onIntent(HomeIntent.PreviousMonth) },
        onNextMonth = { homeViewModel.onIntent(HomeIntent.NextMonth) },
    )
}

@Composable
fun HomeScreen(
    homeData: HomeUiState,
    navigateToAll: () -> Unit = {},
    navigateToReport: () -> Unit = {},
    navigateToAdd: () -> Unit = {},
    onPreviousMonth: () -> Unit = {},
    onNextMonth: () -> Unit = {},
) {
    val colors = LocalEmmColors.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        // Month pill — centered, 14dp top padding
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp, start = 20.dp, end = 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                MonthSelector(
                    label = homeData.month.fullLabel(),
                    onPrev = onPreviousMonth,
                    onNext = onNextMonth,
                )
            }
        }

        // Hero balance — 30dp top, 24dp horizontal
        item {
            HeroBalance(balance = homeData.balance, month = homeData.month.shortLabel())
        }

        // In/Out row — 24dp top, 24dp horizontal
        item {
            InOutRow(income = homeData.income, spend = homeData.spend)
        }

        // Report preview card — 22dp top, 20dp horizontal
        item {
            ReportPreviewCard(onClick = navigateToReport)
        }

        // Recents header — 24dp top, 24dp horizontal, 8dp bottom
        item {
            RecentsHeader(
                onViewAll = navigateToAll,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 8.dp),
            )
        }

        // Recents list or empty state
        if (homeData.lastTransactions.isEmpty()) {
            item {
                EmptyHomeCard(
                    onAddClick = navigateToAdd,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                )
            }
        } else {
            items(homeData.lastTransactions, TransactionUi::transactionId) { tx ->
                TransactionRow(tx = tx)
            }
        }

        // Bottom padding
        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ── Hero balance ──────────────────────────────────────────────────────────────

@Composable
private fun HeroBalance(balance: Money, month: String) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current

    val balanceDouble = balance.cents.toDouble() / 100.0
    val tone = when {
        balance.cents > 0L -> AmountTone.Pos
        balance.cents < 0L -> AmountTone.Neg
        else -> AmountTone.Mute
    }

    // derive previous month name for the caption
    // TODO(SR-10): wire to real comparison
    val prevMonthName = "abril"

    Column(
        modifier = Modifier.padding(top = 30.dp, start = 24.dp, end = 24.dp),
    ) {
        Eyebrow(text = "Balance · ${month.lowercase()}")
        Spacer(Modifier.height(12.dp))
        AmountHero(value = balanceDouble, size = 52.sp, tone = tone)
        Spacer(Modifier.height(12.dp))
        // TODO(SR-10): wire to real comparison
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Pill(text = "12%", tone = PillTone.Pos, leadingIcon = Icons.Outlined.ArrowUpward)
            Text(
                text = "vs. $prevMonthName",
                style = type.caption,
                color = colors.textTertiary,
            )
        }
    }
}

// ── In/Out row ────────────────────────────────────────────────────────────────

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
        // Entró
        Column {
            Eyebrow(text = "Entró", color = colors.textDisabled)
            Spacer(Modifier.height(4.dp))
            MoneyInline(value = incomeDouble, color = colors.textPrimary)
        }

        // Divider
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(28.dp)
                .background(colors.border),
        )

        // Salió
        Column {
            Eyebrow(text = "Salió", color = colors.textDisabled)
            Spacer(Modifier.height(4.dp))
            MoneyInline(value = spendDouble, color = colors.textSecondary)
        }
    }
}

// ── Report preview card ───────────────────────────────────────────────────────

private data class ReportSegment(val color: Color, val pct: Float, val label: String, val value: String)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReportPreviewCard(onClick: () -> Unit) {
    val colors = LocalEmmColors.current
    val radii = LocalEmmRadii.current
    val type = LocalEmmType.current
    val interactionSource = remember { MutableInteractionSource() }

    // TODO(SR-10): wire to real income-by-category breakdown
    val segments = listOf(
        ReportSegment(colors.catTerracotta, 0.72f, "Sueldo", "72%"),
        ReportSegment(colors.catSlate,      0.19f, "Freelance", "19%"),
        ReportSegment(colors.catSage,       0.06f, "Ventas", "6%"),
        ReportSegment(colors.catOchre,      0.03f, "Yapes", "3%"),
    )

    Box(
        modifier = Modifier
            .padding(top = 22.dp, start = 20.dp, end = 20.dp)
            .fillMaxWidth()
            .clip(radii.rXL)
            .background(colors.surface1)
            .border(1.dp, colors.border, radii.rXL)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Column {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Eyebrow(text = "Reporte de ingresos")
                Spacer(Modifier.weight(1f))
                Text(
                    text = "Ver",
                    style = type.caption.copy(fontWeight = FontWeight.W500),
                    color = colors.textSecondary,
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(12.dp),
                )
            }

            Spacer(Modifier.height(12.dp))

            // Mini stacked horizontal bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(colors.surface2),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                segments.forEach { seg ->
                    Box(
                        modifier = Modifier
                            .weight(seg.pct)
                            .height(8.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(seg.color),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Legend chips
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                segments.forEach { seg ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(seg.color),
                        )
                        Text(
                            text = seg.label,
                            style = TextStyle(
                                fontFamily = InterFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.W500,
                            ),
                            color = colors.textTertiary,
                        )
                        Text(
                            text = seg.value,
                            style = TextStyle(
                                fontFamily = PlexMonoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.W500,
                                fontFeatureSettings = "tnum",
                            ),
                            color = colors.textDisabled,
                        )
                    }
                }
            }
        }
    }
}

// ── Recents header ────────────────────────────────────────────────────────────

@Composable
private fun RecentsHeader(
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
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
                style = type.caption.copy(fontWeight = FontWeight.W500),
                color = colors.textSecondary,
            )
        }
    }
}

// ── Transaction row ───────────────────────────────────────────────────────────

@Composable
private fun TransactionRow(tx: TransactionUi) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current

    val amountColor = if (tx.type == TransactionType.Income) colors.success else colors.textSecondary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconTile(
            icon = tx.category.categoryIcon,
            size = IconTileSize.Sm,
            tone = IconTileTone.Swatch,
            swatch = tx.category.categoryColor.primary,
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tx.description.ifBlank { "Sin descripción" },
                style = type.amountS.copy(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.W500,
                    letterSpacing = (-0.065).sp, // ≈ -0.005em × 13sp
                    fontFeatureSettings = "",
                ),
                color = colors.textPrimary,
                maxLines = 1,
            )
            Text(
                text = "${tx.readableDate} · ${tx.readableTime}",
                style = type.caption.copy(
                    letterSpacing = 0.11.sp, // ≈ 0.01em × 11sp
                ),
                color = colors.textTertiary,
                maxLines = 1,
            )
        }

        // tx.amount is pre-formatted (e.g. "+S/ 3,200.00" or "−S/ 84.20")
        Text(
            text = tx.amount,
            style = type.amountS,
            color = amountColor,
        )
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyHomeCard(
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val radii = LocalEmmRadii.current
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = colors.borderFocus,
                    shape = RoundedCornerShape(18.dp),
                )
                .padding(horizontal = 24.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Accent-tinted sparkle icon
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.accentMuted),
            ) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Aún no hay movimientos",
                style = type.titleM,
                color = colors.textPrimary,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Tu primer movimiento te toma 15 segundos. Tipea el monto, elige la categoría, listo.",
                style = type.bodyM.copy(lineHeight = (13 * 1.5).sp),
                color = colors.textTertiary,
                modifier = Modifier.widthIn(max = 260.dp),
            )

            Spacer(Modifier.height(20.dp))

            // CTA button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.accent)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onAddClick,
                    )
                    .padding(horizontal = 18.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = "Anotar el primero",
                    style = TextStyle(
                        fontFamily = InterFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.W600,
                    ),
                    color = Color.White,
                )
            }
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

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
