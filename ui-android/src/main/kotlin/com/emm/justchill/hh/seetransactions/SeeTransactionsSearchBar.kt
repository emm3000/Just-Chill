package com.emm.justchill.hh.seetransactions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType

@Composable
internal fun SearchBar(query: String, onQueryChange: (String) -> Unit, onClose: () -> Unit) {
    val focusRequester = remember { FocusRequester() }
    val spacing = LocalEmmSpacing.current

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = 12.dp,
                start = 24.dp,
                end = spacing.s6 - spacing.headerEdgeGiveback,
                bottom = 14.dp,
            ),
    ) {
        SearchInput(
            query = query,
            onQueryChange = onQueryChange,
            focusRequester = focusRequester,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        HeaderAction(
            icon = Icons.Outlined.Close,
            contentDescription = "Cerrar búsqueda",
            onClick = onClose,
        )
    }
}

@Composable
private fun SearchInput(
    query: String,
    onQueryChange: (String) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface1)
            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 11.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = type.labelM.copy(
                color = colors.textPrimary,
                fontSize = 13.sp,
                letterSpacing = 0.sp,
            ),
            cursorBrush = SolidColor(colors.accent),
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text(
                        text = "Buscar por descripción o monto",
                        style = type.labelM.copy(
                            color = colors.textTertiary,
                            fontSize = 13.sp,
                            letterSpacing = 0.sp,
                        ),
                    )
                }
                inner()
            },
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
        )
        if (query.isNotEmpty()) {
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Limpiar búsqueda",
                tint = colors.textTertiary,
                modifier = Modifier
                    .size(14.dp)
                    .clickable { onQueryChange("") },
            )
        }
    }
}
