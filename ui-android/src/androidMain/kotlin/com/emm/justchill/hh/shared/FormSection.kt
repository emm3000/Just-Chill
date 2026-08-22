package com.emm.justchill.hh.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.ui.atoms.Eyebrow

@Composable
fun FormSection(eyebrow: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Eyebrow(text = eyebrow)
        content()
    }
}
