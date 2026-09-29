package com.emm.justchill.feature.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun AppSection(appVersion: String, onAboutClick: () -> Unit, onPrivacyClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(text = "App")
        ProfileGroup {
            ProfileRow(
                icon = Icons.Outlined.Info,
                label = "Acerca de JustChill",
                meta = "El manifiesto · v$appVersion",
                metaIsPrimary = false,
                onClick = onAboutClick,
            )
            ProfileRow(
                icon = Icons.Outlined.Shield,
                label = "Privacidad",
                meta = "100 % local, sin cuenta",
                metaIsPrimary = false,
                onClick = onPrivacyClick,
            )
        }
    }
}
