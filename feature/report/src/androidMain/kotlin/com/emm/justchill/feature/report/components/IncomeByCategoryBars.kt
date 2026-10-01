package com.emm.justchill.feature.report.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import com.emm.justchill.core.ui.category.resolvedColor
import com.emm.justchill.core.ui.preview.PreviewRedmi15CWidth
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.EmmTheme
import com.emm.justchill.core.ui.theme.EmmType
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmRadii
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmType
import com.emm.justchill.feature.report.CategoryShare
import kotlinx.coroutines.delay

@Composable
fun IncomeByCategoryBars(shares: List<CategoryShare>, modifier: Modifier = Modifier) {
    val spacing: EmmSpacing = LocalEmmSpacing.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.s3),
    ) {
        shares.forEachIndexed { index, share ->
            CategoryShareRow(
                share = share,
                animationDelayMs = index * 50L,
            )
        }
    }
}

@Composable
private fun CategoryShareRow(share: CategoryShare, animationDelayMs: Long) {
    val colors: EmmColors = LocalEmmColors.current
    val type: EmmType = LocalEmmType.current
    val spacing: EmmSpacing = LocalEmmSpacing.current

    val targetFraction: Float = (share.percentage / 100f).coerceIn(0f, 1f)
    val animatedFraction: Animatable<Float, AnimationVector1D> = remember(share.categoryId) { Animatable(0f) }
    LaunchedEffect(share.categoryId, share.percentage) {
        delay(animationDelayMs)
        animatedFraction.animateTo(
            targetValue = targetFraction,
            animationSpec = tween(durationMillis = 300),
        )
    }

    val dotColor: Color = colors.resolvedColor(share.colorKey)

    val description: String = "${share.name}: ${share.amountFormatted}, ${share.percentage} por ciento del total"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(spacing.s2),
    ) {
        CategoryShareHeader(
            gap = spacing.s2,
            dot = {
                Box(
                    modifier = Modifier
                        .size(spacing.s2)
                        .clip(CircleShape)
                        .background(dotColor),
                )
            },
            name = {
                Text(
                    text = share.name,
                    style = type.bodyL,
                    color = colors.textPrimary,
                )
            },
            figures = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = share.amountFormatted,
                        style = type.amountS,
                        color = colors.textPrimary,
                    )
                    Spacer(Modifier.width(spacing.s2))
                    Text(
                        text = "${share.percentage}%",
                        style = type.caption,
                        color = colors.textSecondary,
                    )
                }
            },
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(spacing.s1),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedFraction.value)
                    .height(spacing.s1)
                    .clip(LocalEmmRadii.current.rFull)
                    .background(dotColor),
            )
        }
    }
}

@Composable
private fun CategoryShareHeader(
    gap: Dp,
    dot: @Composable () -> Unit,
    name: @Composable () -> Unit,
    figures: @Composable () -> Unit,
) {
    Layout(
        contents = listOf(dot, name, figures),
        modifier = Modifier.fillMaxWidth(),
    ) { measurables: List<List<Measurable>>, constraints: Constraints ->
        val gapPx: Int = gap.roundToPx()
        val dotPlaceable: Placeable = measurables[0].single().measure(Constraints())
        val figuresPlaceable: Placeable = measurables[2].single().measure(Constraints(maxWidth = constraints.maxWidth))
        val nameMeasurable: Measurable = measurables[1].single()
        val nameX: Int = dotPlaceable.width + gapPx
        val nameSpace: Int = (constraints.maxWidth - nameX).coerceAtLeast(0)
        val besideSpace: Int = (nameSpace - figuresPlaceable.width).coerceAtLeast(0)
        val fitsBeside: Boolean = nameMeasurable.minIntrinsicWidth(Constraints.Infinity) <= besideSpace
        val nameWidth: Int = if (fitsBeside) besideSpace else nameSpace
        val namePlaceable: Placeable = nameMeasurable.measure(Constraints(minWidth = nameWidth, maxWidth = nameWidth))
        val figuresX: Int = constraints.maxWidth - figuresPlaceable.width
        if (fitsBeside) {
            val height: Int = maxOf(dotPlaceable.height, namePlaceable.height, figuresPlaceable.height)
            layout(constraints.maxWidth, height) {
                dotPlaceable.placeRelative(0, (height - dotPlaceable.height) / 2)
                namePlaceable.placeRelative(nameX, (height - namePlaceable.height) / 2)
                figuresPlaceable.placeRelative(figuresX, (height - figuresPlaceable.height) / 2)
            }
        } else {
            val topHeight: Int = maxOf(dotPlaceable.height, namePlaceable.height)
            val figuresY: Int = topHeight + gapPx
            layout(constraints.maxWidth, figuresY + figuresPlaceable.height) {
                dotPlaceable.placeRelative(0, (topHeight - dotPlaceable.height) / 2)
                namePlaceable.placeRelative(nameX, 0)
                figuresPlaceable.placeRelative(figuresX, figuresY)
            }
        }
    }
}

@Preview
@Composable
private fun IncomeByCategoryBarsPreview() {
    EmmTheme {
        val colors: EmmColors = LocalEmmColors.current
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.bg)
                .padding(LocalEmmSpacing.current.s4),
        ) {
            IncomeByCategoryBars(
                shares = listOf(
                    CategoryShare("1", "Sueldo", "S/ 4,500.00", 73, "red"),
                    CategoryShare("2", "Freelance", "S/ 1,200.00", 19, "blue"),
                    CategoryShare("3", "Ventas IG", "S/ 380.00", 6, "green"),
                    CategoryShare("4", "Yapes", "S/ 120.00", 2, "orange"),
                ),
            )
        }
    }
}

@Preview
@PreviewRedmi15CWidth
@Composable
private fun CategoryShareRowOverflowPreview() {
    EmmTheme {
        val colors: EmmColors = LocalEmmColors.current
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.bg)
                .padding(LocalEmmSpacing.current.s4),
        ) {
            CategoryShareRow(
                share = CategoryShare(
                    categoryId = "1",
                    name = "Cuidado personal y salud",
                    amountFormatted = "S/ 999,999.99",
                    percentage = 100,
                    colorKey = "red",
                ),
                animationDelayMs = 0L,
            )
        }
    }
}
