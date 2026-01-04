package com.emm.justchill.hh.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LatoFontFamily
import com.emm.justchill.hh.transaction.EmmCenteredToolbar

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    onLogout: () -> Unit = {},
) {

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {

        EmmCenteredToolbar(
            title = "Profile",
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
        )

        Column(
            modifier = Modifier,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProfileItem(
                text = "Exportar"
            )

            ProfileItem(
                text = "Importar"
            )

            ProfileItem(
                text = "Default account"
            )

            ProfileItem(
                text = "Cerrar Sesion",
                onClick = onLogout,
            )
        }

    }
}

@Composable
fun ProfileItem(
    text: String,
    onClick: () -> Unit = {}
) {

    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp,
        onClick = onClick
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = LatoFontFamily,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview
@Composable
private fun ProfileScreenPreview() {
    EmmTheme {
        ProfileScreen()
    }
}