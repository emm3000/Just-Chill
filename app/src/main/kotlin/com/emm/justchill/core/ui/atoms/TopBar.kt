package com.emm.justchill.core.ui.atoms

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.theme.InterFontFamily
import com.emm.justchill.core.theme.LocalEmmColors

/**
 * Design-system top bar with 44dp hit-zone slots for [left] and [right] actions,
 * and a centered title at 13sp w600 Inter.
 *
 * The bar itself has no background — wrap in a [androidx.compose.material3.Surface]
 * or apply background on the parent scaffold.
 */
@Composable
fun JcTopBar(
    title: String,
    left: @Composable (() -> Unit)? = null,
    right: @Composable (() -> Unit)? = null,
) {
    val colors = LocalEmmColors.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
    ) {
        // Leading slot
        if (left != null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(44.dp),
            ) {
                left()
            }
        }

        // Centered title
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.W600,
            fontFamily = InterFontFamily,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.Center),
        )

        // Trailing slot
        if (right != null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(44.dp),
            ) {
                right()
            }
        }
    }
}
