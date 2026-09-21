package com.emm.justchill.core.ui.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.emm.justchill.core.ui.format.NumberFormatEs
import com.emm.justchill.core.ui.preview.PreviewRedmi15CWidth
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.EmmTheme
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmType

@Composable
fun MoneyInline(value: Double, modifier: Modifier = Modifier, color: Color? = null) {
    val colors = LocalEmmColors.current

    val formatted = remember(value) {
        val prefix = if (value < 0) "− S/ " else "S/ "
        "$prefix${NumberFormatEs.decimal2(value)}"
    }

    Text(
        text = formatted,
        style = LocalEmmType.current.amountM,
        color = color ?: colors.textPrimary,
        modifier = modifier,
    )
}

@Preview
@PreviewRedmi15CWidth
@Composable
private fun MoneyInlinePreview() {
    EmmTheme {
        val colors: EmmColors = LocalEmmColors.current
        val spacing: EmmSpacing = LocalEmmSpacing.current
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.s2),
            modifier = Modifier
                .background(colors.bg)
                .padding(spacing.s4),
        ) {
            MoneyInline(value = 1234.5)
            MoneyInline(value = -89.9)
        }
    }
}
