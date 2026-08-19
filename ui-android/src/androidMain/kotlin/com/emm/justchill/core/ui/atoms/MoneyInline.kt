package com.emm.justchill.core.ui.atoms

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.theme.InterFontFamily
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.hh.shared.NumberFormatEs

@Composable
fun MoneyInline(value: Double, modifier: Modifier = Modifier, color: Color? = null) {
    val colors = LocalEmmColors.current

    val formatted = remember(value) {
        val prefix = if (value < 0) "− S/ " else "S/ "
        "$prefix${NumberFormatEs.decimal2(value)}"
    }

    Text(
        text = formatted,
        style = TextStyle(
            color = color ?: colors.textPrimary,
            fontFamily = InterFontFamily,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.W600,
            letterSpacing = (-0.15).sp,
            fontFeatureSettings = "tnum",
        ),
        modifier = modifier,
    )
}
