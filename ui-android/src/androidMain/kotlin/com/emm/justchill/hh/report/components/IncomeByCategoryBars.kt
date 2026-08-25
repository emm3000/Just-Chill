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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType
import com.emm.justchill.core.ui.preview.PreviewRedmi15CWidth
import com.emm.justchill.hh.report.CategoryShare
import com.emm.justchill.hh.report.domainColorToUi
import kotlinx.coroutines.delay

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
                    .background(domainColorToUi(share.colorKey)),
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
                    .background(domainColorToUi(share.colorKey)),
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
        val colors = LocalEmmColors.current
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.bg)
                .padding(16.dp),
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
