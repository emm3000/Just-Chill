package com.emm.justchill.feature.transaction.list

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.EmmRadii
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.EmmType
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmRadii
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmType

@Composable
internal fun SearchBar(query: String, onQueryChange: (String) -> Unit, onClose: () -> Unit) {
    val focusRequester: FocusRequester = remember { FocusRequester() }
    val spacing: EmmSpacing = LocalEmmSpacing.current

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = spacing.s3,
                start = spacing.s6,
                end = spacing.s6,
                bottom = spacing.s4,
            ),
    ) {
        SearchInput(
            query = query,
            onQueryChange = onQueryChange,
            focusRequester = focusRequester,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(spacing.s2))
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
    val colors: EmmColors = LocalEmmColors.current
    val type: EmmType = LocalEmmType.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val radii: EmmRadii = LocalEmmRadii.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(spacing.s12)
            .clip(radii.rM)
            .background(colors.surface1)
            .border(spacing.hairline, colors.border, radii.rM)
            .padding(horizontal = spacing.s3),
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(spacing.s4),
        )
        Spacer(Modifier.width(spacing.s3))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = type.labelL.copy(color = colors.textPrimary),
            cursorBrush = SolidColor(colors.borderFocus),
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text(
                        text = "Buscar por descripción o monto",
                        style = type.labelL,
                        color = colors.textTertiary,
                    )
                }
                inner()
            },
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
        )
        if (query.isNotEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(spacing.s12)
                    .clip(CircleShape)
                    .clickable { onQueryChange("") },
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Limpiar búsqueda",
                    tint = colors.textTertiary,
                    modifier = Modifier.size(spacing.s4),
                )
            }
        }
    }
}
