package com.emm.justchill.hh.report.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType
import com.emm.justchill.core.ui.atoms.Eyebrow
import com.emm.justchill.core.ui.atoms.Hairline
import com.emm.justchill.hh.report.MonthlyBarItem
import kotlinx.coroutines.delay

private val CHART_HEIGHT = 120.dp
private val BAR_WIDTH = 12.dp
private val BAR_GAP = 4.dp
private val BAR_RADIUS = 4.dp

@Composable
fun VerticalBarChart(items: List<MonthlyBarItem>, modifier: Modifier = Modifier) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current

    val maxAmount = items.maxOfOrNull { maxOf(it.incomeAmount, it.expenseAmount) }?.toFloat() ?: 1f

    val animatables = remember(items.size) { List(items.size) { Animatable(0f) } }

    items.forEachIndexed { index, _ ->
        LaunchedEffect(items[index].incomeAmount, items[index].expenseAmount) {
            delay(index * 50L)
            animatables[index].animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 300),
            )
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(CHART_HEIGHT),
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(CHART_HEIGHT)
                    .semantics { hideFromAccessibility() },
            ) {
                val chartWidthPx = size.width
                val chartHeightPx = size.height
                val barWidthPx = BAR_WIDTH.toPx()
                val barGapPx = BAR_GAP.toPx()
                val groupWidthPx = barWidthPx * 2 + barGapPx
                val n = items.size.toFloat()
                val interGroupGap = if (n > 1) (chartWidthPx - n * groupWidthPx) / (n + 1) else 0f
                val radiusPx = BAR_RADIUS.toPx()

                items.forEachIndexed { index, item ->
                    val progress = animatables[index].value
                    val groupLeft = interGroupGap + index * (groupWidthPx + interGroupGap)

                    val incomeHeightPx = if (maxAmount == 0f) {
                        0f
                    } else {
                        (item.incomeAmount.toFloat() / maxAmount) * chartHeightPx * progress
                    }
                    val expenseHeightPx = if (maxAmount == 0f) {
                        0f
                    } else {
                        (item.expenseAmount.toFloat() / maxAmount) * chartHeightPx * progress
                    }

                    if (incomeHeightPx > 0f) {
                        drawRoundRect(
                            color = colors.catSage,
                            topLeft = Offset(groupLeft, chartHeightPx - incomeHeightPx),
                            size = Size(barWidthPx, incomeHeightPx),
                            cornerRadius = CornerRadius(radiusPx, radiusPx),
                        )
                    }

                    val expenseLeft = groupLeft + barWidthPx + barGapPx
                    if (expenseHeightPx > 0f) {
                        drawRoundRect(
                            color = colors.catTerracotta,
                            topLeft = Offset(expenseLeft, chartHeightPx - expenseHeightPx),
                            size = Size(barWidthPx, expenseHeightPx),
                            cornerRadius = CornerRadius(radiusPx, radiusPx),
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            items.forEach { item ->
                val textColor = if (item.isCurrentMonth) colors.textPrimary else colors.textSecondary
                val fontWeight = if (item.isCurrentMonth) FontWeight.W600 else FontWeight.W400
                Text(
                    text = item.monthShortLabel,
                    style = type.caption,
                    color = textColor,
                    fontWeight = fontWeight,
                    modifier = Modifier.semantics {
                        contentDescription =
                            "${item.monthShortLabel}: entró ${item.incomeFormatted}, salió ${item.expenseFormatted}"
                    },
                )
            }
        }
    }
}

@Composable
fun EntroVsSalioCard(
    items: List<MonthlyBarItem>,
    averageIncomeFormatted: String,
    averageExpenseFormatted: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEmmColors.current
    val spacing = LocalEmmSpacing.current
    val type = LocalEmmType.current
    val radii = com.emm.justchill.core.theme.LocalEmmRadii.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(radii.rM)
            .background(colors.surface1)
            .border(width = 1.dp, color = colors.border, shape = radii.rM)
            .padding(spacing.s4),
        verticalArrangement = Arrangement.spacedBy(spacing.s4),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Eyebrow(text = "ENTRÓ VS SALIÓ")
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.s3),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LegendDot(color = colors.catSage, label = "Entró")
                LegendDot(color = colors.catTerracotta, label = "Salió")
            }
        }

        VerticalBarChart(items = items)

        Hairline()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Promedio mensual",
                style = type.labelM,
                color = colors.textSecondary,
            )
            Text(
                text = "$averageIncomeFormatted · $averageExpenseFormatted",
                style = type.amountS,
                color = colors.textSecondary,
            )
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    val type = LocalEmmType.current
    val colors = LocalEmmColors.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = label,
            style = type.labelM,
            color = colors.textSecondary,
        )
    }
}

@Preview
@Composable
private fun EntroVsSalioCardPreview() {
    EmmTheme {
        val colors = LocalEmmColors.current
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.bg)
                .padding(16.dp),
        ) {
            EntroVsSalioCard(
                items = listOf(
                    MonthlyBarItem("Dic", false, 580_000L, 420_000L, "S/ 5,800", "S/ 4,200"),
                    MonthlyBarItem("Ene", false, 620_000L, 430_000L, "S/ 6,200", "S/ 4,300"),
                    MonthlyBarItem("Feb", false, 550_000L, 380_000L, "S/ 5,500", "S/ 3,800"),
                    MonthlyBarItem("Mar", false, 610_000L, 410_000L, "S/ 6,100", "S/ 4,100"),
                    MonthlyBarItem("Abr", false, 590_000L, 440_000L, "S/ 5,900", "S/ 4,400"),
                    MonthlyBarItem("May", true, 630_000L, 460_000L, "S/ 6,300", "S/ 4,600"),
                ),
                averageIncomeFormatted = "S/ 5,973",
                averageExpenseFormatted = "S/ 4,240",
            )
        }
    }
}
