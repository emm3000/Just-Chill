package com.emm.justchill.core.ui.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.theme.LocalEmmColors

/**
 * A 1dp horizontal divider.
 *
 * @param insetStart  leading horizontal indent (e.g. to align with list content).
 * @param insetEnd    trailing horizontal indent.
 * @param color       divider color; defaults to [EmmColors.border].
 */
@Composable
fun Hairline(insetStart: Dp = 0.dp, insetEnd: Dp = 0.dp, color: Color? = null) {
    val colors = LocalEmmColors.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = insetStart, end = insetEnd)
            .height(1.dp)
            .background(color ?: colors.border),
    )
}
