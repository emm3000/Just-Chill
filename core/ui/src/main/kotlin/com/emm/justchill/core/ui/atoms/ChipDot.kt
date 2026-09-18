package com.emm.justchill.core.ui.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Marks, not gaps — the spacing scale governs the distance between them, never their own size.
private val ChipDotSize: Dp = 8.dp

@Composable
internal fun ChipDot(color: Color) {
    Box(
        modifier = Modifier
            .size(ChipDotSize)
            .clip(CircleShape)
            .background(color),
    )
}
