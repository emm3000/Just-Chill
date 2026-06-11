package com.emm.justchill.core.ui.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.theme.InterFontFamily
import com.emm.justchill.core.theme.LocalEmmColors

/** Visual tone for [EmmSnackbarVisuals]. */
enum class EmmSnackbarTone { Success, Error }

/**
 * Custom [SnackbarVisuals] that carries a [tone] and an optional [actionLabel].
 *
 * Use [SnackbarHostState.showEmmSnackbar] as the convenient entry point.
 */
class EmmSnackbarVisuals(
    override val message: String,
    val tone: EmmSnackbarTone = EmmSnackbarTone.Success,
    override val actionLabel: String? = null,
    override val withDismissAction: Boolean = false,
    override val duration: SnackbarDuration = SnackbarDuration.Short,
) : SnackbarVisuals

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

    // Resolve tone from the visuals — fall back to Success for plain showSnackbar(message) calls.
    val tone = (data.visuals as? EmmSnackbarVisuals)?.tone ?: EmmSnackbarTone.Success

    val iconVector: ImageVector = when (tone) {
        EmmSnackbarTone.Success -> Icons.Outlined.Check
        EmmSnackbarTone.Error -> Icons.Outlined.ErrorOutline
    }
    val iconTint: Color = when (tone) {
        EmmSnackbarTone.Success -> colors.success
        EmmSnackbarTone.Error -> colors.danger
    }
    val iconBg: Color = when (tone) {
        EmmSnackbarTone.Success -> colors.posMuted
        EmmSnackbarTone.Error -> colors.negMuted
    }

    val actionLabel = data.visuals.actionLabel

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
                .background(iconBg),
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(13.dp),
            )
        }
        Text(
            text = highlightQuoted(data.visuals.message),
            fontSize = 13.sp,
            fontFamily = InterFontFamily,
            color = colors.textPrimary,
            fontWeight = FontWeight.W500,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = if (actionLabel != null) Modifier.weight(1f) else Modifier,
        )
        if (actionLabel != null) {
            Text(
                text = actionLabel.uppercase(),
                fontSize = 13.sp,
                fontFamily = InterFontFamily,
                color = colors.accent,
                fontWeight = FontWeight.W600,
                maxLines = 1,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .clickable(onClick = data::performAction)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            )
        }
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

/**
 * Convenience extension that wraps [message] in [EmmSnackbarVisuals] and shows it.
 *
 * All existing `showSnackbar(message)` call sites are unaffected — this is additive.
 */
suspend fun SnackbarHostState.showEmmSnackbar(
    message: String,
    tone: EmmSnackbarTone = EmmSnackbarTone.Success,
    actionLabel: String? = null,
): SnackbarResult = showSnackbar(EmmSnackbarVisuals(message = message, tone = tone, actionLabel = actionLabel))
