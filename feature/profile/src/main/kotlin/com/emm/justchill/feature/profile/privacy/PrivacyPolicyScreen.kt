package com.emm.justchill.feature.profile.privacy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.emm.justchill.core.ui.atoms.FilledCta
import com.emm.justchill.core.ui.theme.EmmTheme
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmType

@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .padding(horizontal = spacing.s6),
    ) {
        Spacer(Modifier.height(spacing.s6))

        Text(
            text = "Tu privacidad",
            style = type.headlineL,
            color = colors.textPrimary,
        )

        Spacer(Modifier.height(spacing.s6))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.s4),
        ) {
            Text(
                text = "Tu plata vive en tu celular. Sin cuenta, nada sale de él.",
                style = type.bodyL,
                color = colors.textPrimary,
            )
            Text(
                text = "Si creas una cuenta (opcional), guardamos tu correo para " +
                    "identificarte. Tu data financiera —montos, categorías, movimientos— " +
                    "no se sube a nuestros servidores: hoy tener cuenta no la respalda " +
                    "ni la lleva entre tus celulares. No la vendemos ni la compartimos.",
                style = type.bodyL,
                color = colors.textPrimary,
            )
            Text(
                text = "Si exportas tu data a un archivo, tú decides qué hacer con él — " +
                    "guardarlo, mandarlo o borrarlo.",
                style = type.bodyL,
                color = colors.textPrimary,
            )
            Text(
                text = "No usamos analytics ni cookies. La versión de Play Store reporta " +
                    "solo crashes (Crashlytics), nunca tu data financiera.",
                style = type.bodyL,
                color = colors.textPrimary,
            )
            Text(
                text = "Si cambias de celular sin exportar primero, tu data se pierde — " +
                    "pasa igual con cuenta o sin ella, porque tu data financiera no está " +
                    "en nuestros servidores. Para recuperarla necesitas un archivo JSON " +
                    "que hayas exportado tú.",
                style = type.bodyL,
                color = colors.textPrimary,
            )
            Text(
                text = "¿Quieres borrar tu cuenta y tu data del servidor? " +
                    "Puedes hacerlo directo desde la app: Perfil → \"Eliminar cuenta\". " +
                    "También puedes escribirnos a edgardo.emm20@gmail.com.",
                style = type.bodyL,
                color = colors.textPrimary,
            )
        }

        Spacer(Modifier.weight(1f))

        FilledCta(
            label = "Volver",
            onClick = onBack,
        )

        Spacer(Modifier.height(spacing.s4))
    }
}

@Preview
@Composable
private fun PrivacyPolicyScreenPreview() {
    EmmTheme {
        PrivacyPolicyScreen(onBack = {})
    }
}
