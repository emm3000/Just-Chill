package com.emm.justchill.core.ui.atoms

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmType

/**
 * Eyebrow label — 10sp, w500, letter-spacing 0.16em, uppercase.
 * Default color: [EmmColors.textTertiary].
 */
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color? = null) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current

    Text(
        text = text.uppercase(),
        style = type.eyebrow,
        color = color ?: colors.textTertiary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}
