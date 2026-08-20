package com.emm.justchill.hh.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType

@Composable
fun ManifestoScreen(isRevisit: Boolean, onStart: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current

    val subheroAnnotated = buildAnnotatedString {
        withStyle(SpanStyle(color = colors.textPrimary)) {
            append("Necesita tu atención,\n")
        }
        withStyle(SpanStyle(color = colors.accent)) {
            append("30 segundos al día.")
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.s6)
                .padding(top = spacing.s10, bottom = spacing.s4),
            verticalArrangement = Arrangement.spacedBy(spacing.s8),
        ) {
            Text(
                text = "Tu plata no necesita\nun dashboard.",
                style = type.headlineL,
                color = colors.textPrimary,
            )

            Text(
                text = subheroAnnotated,
                style = type.headlineM,
            )

            Text(
                text = "Sin cuenta obligatoria.\nSin notificaciones.\nSin que te vendamos nada.",
                style = type.bodyL,
                color = colors.textPrimary,
            )

            Text(
                text = "Solo tú, tu plata,\ny la verdad.",
                style = type.bodyL,
                color = colors.textTertiary,
            )
        }

        StartButton(
            label = if (isRevisit) "Volver" else "Empezar",
            icon = if (isRevisit) Icons.AutoMirrored.Outlined.ArrowBack else Icons.AutoMirrored.Outlined.ArrowForward,
            iconLeading = isRevisit,
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.s6)
                .padding(top = spacing.s4, bottom = spacing.s6),
        )
    }
}

@Composable
private fun StartButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconLeading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val shape = RoundedCornerShape(18.dp)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .heightIn(min = 56.dp)
            .clip(shape)
            .background(colors.surface1)
            .border(1.dp, colors.border, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (iconLeading) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.textPrimary,
                )
            }
            Text(
                text = label,
                style = type.titleL,
                color = colors.textPrimary,
            )
            if (!iconLeading) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.textPrimary,
                )
            }
        }
    }
}

@Preview
@Composable
private fun ManifestoScreenFirstLaunchPreview() {
    EmmTheme {
        ManifestoScreen(isRevisit = false, onStart = {})
    }
}

@Preview
@Composable
private fun ManifestoScreenSmallScreenPreview() {
    EmmTheme {
        ManifestoScreen(isRevisit = false, onStart = {})
    }
}

@Preview
@Composable
private fun ManifestoScreenRevisitPreview() {
    EmmTheme {
        ManifestoScreen(isRevisit = true, onStart = {})
    }
}
