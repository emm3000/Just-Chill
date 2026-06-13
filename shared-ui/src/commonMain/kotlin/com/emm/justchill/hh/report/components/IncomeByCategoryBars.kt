package com.emm.justchill.hh.report.components

import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType
import com.emm.justchill.hh.report.CategoryShare
import kotlinx.coroutines.delay

/**
 * Category breakdown list — Notion-style.
 *
 * Per DS §7.10 (updated 2026-05-21): dot + name + amount + percentage in a
 * single row, with a thin colored stripe under the row whose width = `%` of
 * the row. No `surface1` track behind the stripe — just a colored stroke
 * that hints at proportion without competing with the type.
 *
 * Sorted descending by amount by the caller.
 */
@Composable
fun IncomeByCategoryBars(shares: List<CategoryShare>, modifier: Modifier = Modifier) {
    val spacing = LocalEmmSpacing.current

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

    val description = "${share.name}: ${share.amountFormatted}, ${share.percentage} por ciento del total"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(spacing.s2),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(share.tint),
            )
            Spacer(Modifier.width(spacing.s2))
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
            Spacer(Modifier.width(spacing.s2))
            Text(
                text = "${share.percentage}%",
                style = type.caption,
                color = colors.textSecondary,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedFraction.value)
                    .height(2.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(share.tint),
            )
        }
    }
}

@Preview
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
                    CategoryShare("1", "Sueldo", "S/ 4,500.00", 73, colors.catTerracotta),
                    CategoryShare("2", "Freelance", "S/ 1,200.00", 19, colors.catSlate),
                    CategoryShare("3", "Ventas IG", "S/ 380.00", 6, colors.catSage),
                    CategoryShare("4", "Yapes", "S/ 120.00", 2, colors.catOchre),
                ),
            )
        }
    }
}
