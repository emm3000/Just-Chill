package com.emm.justchill.hh.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType

@Composable
fun ManifestoScreen(
    isRevisit: Boolean,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = spacing.s6),
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(40.dp),
        ) {
            Text(
                text = "Tu plata no necesita\nun dashboard.",
                style = type.display,
                color = colors.textPrimary,
            )
            Text(
                text = "Necesita tu atención,\n30 segundos al día.",
                style = type.display,
                color = colors.textPrimary,
            )
            Text(
                text = "Sin login.\nSin notificaciones.\nSin que te vendamos nada.",
                style = type.display,
                color = colors.textPrimary,
            )
            Text(
                text = "Solo vos, tu plata,\ny la verdad.",
                style = type.display,
                color = colors.textSecondary,
            )
        }

        Spacer(Modifier.height(48.dp))

        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.accent,
                contentColor = colors.textOnAccent,
            ),
        ) {
            Text(
                text = if (isRevisit) "Volver" else "Empezar",
                style = type.titleL,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 800)
@Composable
private fun ManifestoScreenFirstLaunchPreview() {
    EmmTheme {
        ManifestoScreen(isRevisit = false, onStart = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 800)
@Composable
private fun ManifestoScreenRevisitPreview() {
    EmmTheme {
        ManifestoScreen(isRevisit = true, onStart = {})
    }
}
