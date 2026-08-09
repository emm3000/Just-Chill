package com.emm.justchill.hh.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.theme.LocalEmmType

/** Shared action button for the confirmation dialogs in Perfil. */
@Composable
internal fun DialogActionButton(
    label: String,
    style: DialogActionStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val type = LocalEmmType.current
    val shape = RoundedCornerShape(14.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(shape)
            .background(style.bg)
            .border(1.dp, style.border, shape)
            .clickable(onClick = onClick)
            .padding(PaddingValues(horizontal = 16.dp, vertical = 12.dp)),
    ) {
        Text(
            text = label,
            style = type.titleM,
            color = style.textColor,
        )
    }
}
