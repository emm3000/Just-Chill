package com.emm.justchill.hh.report.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType
import com.emm.justchill.hh.report.CategoryShare
import kotlinx.coroutines.delay

/**
 * Horizontal bars showing income (or expense) by category.
 *
 * The signature visualization of the app — US-11, the apuesta.
 * Spec: `docs/DESIGN_SYSTEM.md §7.10`.
 *
 * Decision D1 (PLAN_S1_REPORT.md): horizontal bars over donut.
 * Reason: legibility in zoom (Fase 1 §5), better for >3 categories,
 * better screenshot.
 */
@Composable
fun IncomeByCategoryBars(
    shares: List<CategoryShare>,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalEmmSpacing.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.s4),
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
private fun CategoryShareRow(
    share: CategoryShare,
    animationDelayMs: Long,
) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current

    val targetFraction = (share.percentage / 100f).coerceIn(0f, 1f)
    val animatedFraction = remember(share.categoryId) { Animatable(0f) }
    LaunchedEffect(share.categoryId, share.percentage) {
        delay(animationDelayMs)
        animatedFraction.animateTo(
            targetValue = targetFraction,
            animationSpec = tween(durationMillis = 300),
        )
    }

    val description = "${share.name}: ${share.amountFormatted}, " +
        "${share.percentage} por ciento del total"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(spacing.s2),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = share.name,
                style = type.bodyL,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = share.amountFormatted,
                style = type.amountS,
                color = colors.textPrimary,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(colors.surface1),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedFraction.value)
                    .background(share.tint),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Text(
                text = "${share.percentage}%",
                style = type.caption,
                color = colors.textSecondary,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun IncomeByCategoryBarsPreview() {
    EmmTheme {
        val colors = LocalEmmColors.current
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.bg)
                .padding(16.dp),
        ) {
            IncomeByCategoryBars(
                shares = listOf(
                    CategoryShare(
                        categoryId = "1",
                        name = "Sueldo",
                        amountFormatted = "S/ 4,500",
                        percentage = 60,
                        tint = colors.catSlate,
                    ),
                    CategoryShare(
                        categoryId = "2",
                        name = "Freelance",
                        amountFormatted = "S/ 1,200",
                        percentage = 19,
                        tint = colors.catSage,
                    ),
                    CategoryShare(
                        categoryId = "3",
                        name = "Ventas",
                        amountFormatted = "S/ 400",
                        percentage = 6,
                        tint = colors.catTerracotta,
                    ),
                    CategoryShare(
                        categoryId = "4",
                        name = "Propinas",
                        amountFormatted = "S/ 80",
                        percentage = 1,
                        tint = colors.catOchre,
                    ),
                    CategoryShare(
                        categoryId = "5",
                        name = "Otros",
                        amountFormatted = "S/ 20",
                        percentage = 0,
                        tint = colors.catGraphite,
                    ),
                ),
            )
            // Spacer just to ensure preview height
            Spacer(Modifier.height(0.dp))
        }
    }
}
