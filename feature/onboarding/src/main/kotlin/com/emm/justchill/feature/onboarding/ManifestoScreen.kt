package com.emm.justchill.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import com.emm.justchill.core.ui.atoms.OutlinedCta
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.EmmTheme
import com.emm.justchill.core.ui.theme.EmmType
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmType

@Composable
fun ManifestoScreen(isRevisit: Boolean, onStart: () -> Unit, modifier: Modifier = Modifier) {
    val colors: EmmColors = LocalEmmColors.current
    val type: EmmType = LocalEmmType.current
    val spacing: EmmSpacing = LocalEmmSpacing.current

    val arrow: @Composable () -> Unit = { ManifestoArrow(isRevisit = isRevisit) }
    val subheroAnnotated: AnnotatedString = buildAnnotatedString {
        withStyle(SpanStyle(color = colors.textPrimary)) {
            append("Necesita tu atención,\n30 segundos al día.")
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg),
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

        OutlinedCta(
            label = if (isRevisit) "Volver" else "Empezar",
            onClick = onStart,
            modifier = Modifier
                .padding(horizontal = spacing.s6)
                .padding(top = spacing.s4, bottom = spacing.s6),
            leading = if (isRevisit) arrow else null,
            trailing = if (isRevisit) null else arrow,
        )
    }
}

@Composable
private fun ManifestoArrow(isRevisit: Boolean) {
    val colors: EmmColors = LocalEmmColors.current
    val icon: ImageVector = if (isRevisit) {
        Icons.AutoMirrored.Outlined.ArrowBack
    } else {
        Icons.AutoMirrored.Outlined.ArrowForward
    }

    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = colors.textPrimary,
    )
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
