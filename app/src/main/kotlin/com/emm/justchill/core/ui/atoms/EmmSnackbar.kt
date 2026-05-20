package com.emm.justchill.core.ui.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.theme.InterFontFamily
import com.emm.justchill.core.theme.LocalEmmColors

@Composable
fun EmmSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(modifier = modifier, hostState = hostState) { data ->
        EmmSnackbarBody(data)
    }
}

@Composable
private fun EmmSnackbarBody(data: SnackbarData) {
    val colors = LocalEmmColors.current
    val shape = RoundedCornerShape(14.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(shape)
            .background(colors.surface2)
            .border(1.dp, colors.border, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(colors.posMuted),
        ) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = colors.success,
                modifier = Modifier.size(13.dp),
            )
        }
        Text(
            text = highlightQuoted(data.visuals.message),
            fontSize = 13.sp,
            fontFamily = InterFontFamily,
            color = colors.textPrimary,
            fontWeight = FontWeight.W500,
        )
    }
}

private fun highlightQuoted(message: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < message.length) {
        val open = message.indexOf('«', i)
        if (open == -1) {
            append(message.substring(i))
            return@buildAnnotatedString
        }
        val close = message.indexOf('»', open + 1)
        if (close == -1) {
            append(message.substring(i))
            return@buildAnnotatedString
        }
        append(message.substring(i, open + 1))
        withStyle(SpanStyle(fontWeight = FontWeight.W700)) {
            append(message.substring(open + 1, close))
        }
        append('»')
        i = close + 1
    }
}
