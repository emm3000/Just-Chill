package com.emm.justchill.core.ui.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import com.emm.justchill.core.ui.preview.PreviewRedmi15CWidth
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.EmmTheme
import com.emm.justchill.core.ui.theme.EmmType
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmRadii
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmType

enum class EmmSnackbarTone { Success, Error }

class EmmSnackbarVisuals(
    override val message: String,
    val tone: EmmSnackbarTone = EmmSnackbarTone.Success,
    override val actionLabel: String? = null,
    override val duration: SnackbarDuration = SnackbarDuration.Short,
) : SnackbarVisuals {
    override val withDismissAction: Boolean = false
}

@Composable
fun EmmSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(modifier = modifier, hostState = hostState) { data ->
        EmmSnackbarBody(data)
    }
}

private data class ToneVisuals(val icon: ImageVector, val tint: Color, val circleBg: Color)

@Composable
private fun EmmSnackbarBody(data: SnackbarData) {
    val colors = LocalEmmColors.current
    val radii = LocalEmmRadii.current
    val spacing = LocalEmmSpacing.current
    val type: EmmType = LocalEmmType.current
    val shape = radii.rL

    val tone = (data.visuals as? EmmSnackbarVisuals)?.tone ?: EmmSnackbarTone.Success

    val visuals: ToneVisuals = when (tone) {
        EmmSnackbarTone.Success -> ToneVisuals(
            icon = Icons.Outlined.Check,
            tint = colors.success,
            circleBg = colors.posMuted,
        )

        EmmSnackbarTone.Error -> ToneVisuals(
            icon = Icons.Outlined.ErrorOutline,
            tint = colors.danger,
            circleBg = colors.negMuted,
        )
    }

    val actionLabel = data.visuals.actionLabel

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.s4)
            .clip(shape)
            .background(colors.bg)
            .border(spacing.hairline, colors.border, shape)
            .padding(horizontal = spacing.s4, vertical = spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.s3),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(spacing.s6)
                .clip(CircleShape)
                .background(visuals.circleBg),
        ) {
            Icon(
                imageVector = visuals.icon,
                contentDescription = null,
                tint = visuals.tint,
                modifier = Modifier.size(spacing.s3),
            )
        }
        Text(
            text = highlightQuoted(data.visuals.message),
            style = type.labelL,
            color = colors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = if (actionLabel != null) Modifier.weight(1f) else Modifier,
        )
        if (actionLabel != null) {
            Text(
                text = actionLabel,
                style = type.labelL,
                color = colors.textPrimary,
                fontWeight = FontWeight.W600,
                maxLines = 1,
                modifier = Modifier
                    .padding(start = spacing.s1)
                    .heightIn(min = spacing.s12)
                    .widthIn(min = spacing.s12)
                    .clickable(onClick = data::performAction)
                    .semantics { role = Role.Button }
                    .padding(horizontal = spacing.s1, vertical = spacing.s1),
            )
        }
    }
}

internal fun highlightQuoted(message: String): AnnotatedString = buildAnnotatedString {
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

suspend fun SnackbarHostState.showEmmSnackbar(
    message: String,
    tone: EmmSnackbarTone = EmmSnackbarTone.Success,
    actionLabel: String? = null,
    duration: SnackbarDuration = SnackbarDuration.Short,
): SnackbarResult = showSnackbar(
    EmmSnackbarVisuals(
        message = message,
        tone = tone,
        actionLabel = actionLabel,
        duration = duration,
    ),
)

private class PreviewSnackbarData(override val visuals: SnackbarVisuals) : SnackbarData {
    override fun performAction() = Unit
    override fun dismiss() = Unit
}

@Preview
@PreviewRedmi15CWidth
@Composable
private fun EmmSnackbarPreview() {
    EmmTheme {
        val colors: EmmColors = LocalEmmColors.current
        val spacing: EmmSpacing = LocalEmmSpacing.current
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.s2),
            modifier = Modifier
                .background(colors.bg)
                .padding(vertical = spacing.s4),
        ) {
            EmmSnackbarBody(
                PreviewSnackbarData(EmmSnackbarVisuals(message = "Guardado «Supermercado»")),
            )
            EmmSnackbarBody(
                PreviewSnackbarData(
                    EmmSnackbarVisuals(
                        message = "No se pudo guardar",
                        tone = EmmSnackbarTone.Error,
                        actionLabel = "Reintentar",
                    ),
                ),
            )
        }
    }
}
