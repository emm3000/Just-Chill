package com.emm.justchill.core.ui.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.ui.preview.PreviewRedmi15CWidth
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.EmmTheme
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmSpacing

// Marks, not gaps — the spacing scale governs the distance between them, never their own size.
private val CategoryDotSize: Dp = 8.dp

@Composable
fun CategoryDot(color: Color) {
    Box(
        modifier = Modifier
            .size(CategoryDotSize)
            .clip(CircleShape)
            .background(color),
    )
}

// A row whose lines take the dot one at a time keeps the slot on every line: a text column that
// starts 8dp further right on the lines without one is the ragged column the rules forbid.
@Composable
fun CategoryDotSlot(color: Color?) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(CategoryDotSize),
    ) {
        if (color != null) {
            CategoryDot(color = color)
        }
    }
}

@Preview
@PreviewRedmi15CWidth
@Composable
private fun CategoryDotPreview() {
    EmmTheme {
        val colors: EmmColors = LocalEmmColors.current
        val spacing: EmmSpacing = LocalEmmSpacing.current
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.s2),
            modifier = Modifier
                .background(colors.bg)
                .padding(spacing.s4),
        ) {
            CategoryDot(color = colors.catSage)
            CategoryDot(color = colors.catOchre)
            CategoryDot(color = colors.catGraphite)
        }
    }
}
