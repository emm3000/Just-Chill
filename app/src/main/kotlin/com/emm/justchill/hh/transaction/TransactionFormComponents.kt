package com.emm.justchill.hh.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmType

@Composable
internal fun SignChip(sign: String, onClick: () -> Unit) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(colors.surface1)
            .border(1.dp, colors.border, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = sign,
            style = type.headlineM,
            color = colors.textPrimary,
        )
    }
}
