package com.emm.justchill.experiences.padding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.CombinedModifier
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.theme.EmmTheme

@Composable
fun RandomAccess(modifier: Modifier = Modifier) {
    Text(text = "como es la nuez")
}

@Composable
fun PaddingScreen() {

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Sample Text",
            style = TextStyle(
                color = Color.White,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
                .padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun Gaa() {
    EmmTheme {
        RandomAccess()
    }
}

@Preview(showBackground = true)
@Composable
fun PaddingScreenPreview() {
    EmmTheme {
        PaddingScreen()
    }
}

