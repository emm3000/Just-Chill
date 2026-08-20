package com.emm.justchill.hh.profile

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType

@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
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
                text = "Si creas una cuenta (opcional), tu data se sincroniza cifrada con " +
                    "nuestros servidores para que la veas en todos tus dispositivos. " +
                    "No la vendemos ni la compartimos.",
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
                text = "Sin cuenta, si cambias de celular sin exportar primero, la data " +
                    "se pierde. Con cuenta, inicia sesión y tu data vuelve.",
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

        Button(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.accent,
                contentColor = colors.textOnAccent,
            ),
        ) {
            Text(
                text = "Volver",
                style = type.titleL,
            )
        }

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
