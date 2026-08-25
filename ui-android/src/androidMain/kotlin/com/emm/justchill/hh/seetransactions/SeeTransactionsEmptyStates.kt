package com.emm.justchill.hh.seetransactions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmType

@Composable
internal fun EmptyNoTransactionsAtAll(modifier: Modifier = Modifier) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current

    Column(
        modifier = modifier.padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surface1)
                .border(1.dp, colors.border, RoundedCornerShape(16.dp)),
        ) {
            Icon(
                imageVector = Icons.Outlined.Receipt,
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = "Aún sin transacciones",
            style = type.titleL.copy(fontSize = 15.sp, letterSpacing = (-0.075).sp),
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Las que registres aparecerán acá agrupadas por día.",
            style = type.caption.copy(lineHeight = 18.sp),
            color = colors.textTertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 260.dp),
        )
    }
}

@Composable
internal fun EmptyMonth(modifier: Modifier = Modifier) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current

    Column(
        modifier = modifier.padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.CalendarMonth,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(26.dp),
        )
        Spacer(Modifier.height(18.dp))
        Text(
            text = "Sin movimientos este mes",
            style = type.titleL.copy(fontSize = 15.sp, letterSpacing = (-0.075).sp),
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Cambia de mes con las flechas de arriba.",
            style = type.caption.copy(lineHeight = 18.sp),
            color = colors.textTertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 240.dp),
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
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current

    val headline = buildAnnotatedString {
        withStyle(
            SpanStyle(
                color = colors.textPrimary,
                fontSize = 15.sp,
                fontFamily = type.labelM.fontFamily,
                fontWeight = FontWeight.W600,
                letterSpacing = (-0.15).sp,
            ),
        ) { append("Sin resultados para ") }

        when {
            activeCategoryName != null -> withStyle(
                SpanStyle(
                    color = colors.accent,
                    fontFamily = type.labelM.fontFamily,
                    fontWeight = FontWeight.W600,
                    fontSize = 15.sp,
                    letterSpacing = (-0.15).sp,
                ),
            ) { append("«$activeCategoryName»") }

            query.isNotEmpty() -> withStyle(
                SpanStyle(
                    color = colors.accent,
                    fontFamily = type.labelM.fontFamily,
                    fontWeight = FontWeight.W600,
                    fontSize = 15.sp,
                    letterSpacing = (-0.15).sp,
                ),
            ) { append("«$query»") }
        }
    }

    Column(
        modifier = modifier.padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (activeCategoryName != null || query.isNotEmpty()) {
            Text(text = headline, textAlign = TextAlign.Center)
        } else {
            Text(
                text = "Sin movimientos con esos filtros",
                style = type.labelM.copy(fontSize = 15.sp, letterSpacing = (-0.15).sp),
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Prueba con otro nombre, otro monto, o limpia los filtros activos.",
            style = type.caption.copy(lineHeight = 18.sp),
            color = colors.textTertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 240.dp),
        )
        Spacer(Modifier.height(20.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .border(1.dp, colors.borderFocus, RoundedCornerShape(999.dp))
                .clickable { onClear() }
                .padding(horizontal = 14.dp, vertical = 9.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = null,
                tint = colors.textPrimary,
                modifier = Modifier.size(11.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Limpiar filtros",
                style = type.labelM.copy(fontSize = 12.sp, letterSpacing = 0.sp),
                color = colors.textPrimary,
            )
        }
    }
}
