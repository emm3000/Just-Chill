package com.emm.justchill.feature.transaction.list

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.EmmRadii
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.EmmType
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmRadii
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmType

// The receipt tile; 56dp sits evenly between s12 (48dp) and s16 (64dp), so no step fits.
private val EmptyGlyphTileSize: Dp = 56.dp

// Measured so the hint breaks into two centred lines; no EmmSpacing step comes near 260dp.
private val NoTransactionsHintMaxWidth: Dp = 260.dp

// Caps the month and filter hints to a centred column narrower than the headline; no EmmSpacing step comes near 240dp.
private val EmptyHintMaxWidth: Dp = 240.dp

@Composable
internal fun EmptyNoTransactionsAtAll(modifier: Modifier = Modifier) {
    val colors: EmmColors = LocalEmmColors.current
    val type: EmmType = LocalEmmType.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val radii: EmmRadii = LocalEmmRadii.current

    Column(
        modifier = modifier.padding(horizontal = spacing.s6, vertical = spacing.s8),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(EmptyGlyphTileSize)
                .clip(radii.rXL)
                .background(colors.surface1)
                .border(spacing.hairline, colors.border, radii.rXL),
        ) {
            Icon(
                imageVector = Icons.Outlined.Receipt,
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(spacing.s6),
            )
        }
        Spacer(Modifier.height(spacing.s5))
        Text(
            text = "Aún sin transacciones",
            style = type.titleM,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(spacing.s2))
        Text(
            text = "Las que registres aparecerán acá agrupadas por día.",
            style = type.caption,
            color = colors.textTertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = NoTransactionsHintMaxWidth),
        )
    }
}

@Composable
internal fun EmptyMonth(modifier: Modifier = Modifier) {
    val colors: EmmColors = LocalEmmColors.current
    val type: EmmType = LocalEmmType.current
    val spacing: EmmSpacing = LocalEmmSpacing.current

    Column(
        modifier = modifier.padding(horizontal = spacing.s6, vertical = spacing.s8),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.CalendarMonth,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(spacing.s6),
        )
        Spacer(Modifier.height(spacing.s5))
        Text(
            text = "Sin movimientos este mes",
            style = type.titleM,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(spacing.s2))
        Text(
            text = "Cambia de mes con las flechas de arriba.",
            style = type.caption,
            color = colors.textTertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = EmptyHintMaxWidth),
        )
    }
}

@Composable
internal fun EmptyFilteredNoResults(
    query: String,
    activeCategoryName: String?,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors: EmmColors = LocalEmmColors.current
    val type: EmmType = LocalEmmType.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val radii: EmmRadii = LocalEmmRadii.current

    val headline: String = when {
        activeCategoryName != null -> "Sin resultados para «$activeCategoryName»"
        query.isNotEmpty() -> "Sin resultados para «$query»"
        else -> "Sin movimientos con esos filtros"
    }

    Column(
        modifier = modifier.padding(horizontal = spacing.s6, vertical = spacing.s8),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = headline,
            style = type.titleM,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(spacing.s2))
        Text(
            text = "Prueba con otro nombre, otro monto, o limpia los filtros activos.",
            style = type.caption,
            color = colors.textTertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = EmptyHintMaxWidth),
        )
        Spacer(Modifier.height(spacing.s5))
        val pillShape: RoundedCornerShape = radii.rFull
        val clearInteraction: MutableInteractionSource = remember { MutableInteractionSource() }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .height(spacing.s12)
                .clickable(
                    interactionSource = clearInteraction,
                    indication = null,
                    onClick = onClear,
                ),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(pillShape)
                    .border(spacing.hairline, colors.borderFocus, pillShape)
                    .indication(clearInteraction, ripple())
                    .padding(horizontal = spacing.s4, vertical = spacing.s2),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = null,
                    tint = colors.textPrimary,
                    modifier = Modifier.size(spacing.s3),
                )
                Spacer(Modifier.width(spacing.s2))
                Text(
                    text = "Limpiar filtros",
                    style = type.labelM,
                    color = colors.textPrimary,
                )
            }
        }
    }
}
