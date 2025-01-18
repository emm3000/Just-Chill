package com.emm.justchill.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.HhBackgroundColor
import com.emm.justchill.core.theme.HhDisabledPrimaryButtonColor
import com.emm.justchill.core.theme.HhDisabledTextPrimaryBackground
import com.emm.justchill.core.theme.HhOnBackgroundColor
import com.emm.justchill.core.theme.HhPrimaryTextColor

@Composable
fun HhPrimaryButton(
    text: String,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {

    FilledTonalButton(
        onClick = onClick,
        modifier = modifier
            .height(46.dp),
        shape = RoundedCornerShape(8),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = HhOnBackgroundColor,
            contentColor = HhPrimaryTextColor,
            disabledContainerColor = HhDisabledPrimaryButtonColor,
            disabledContentColor = HhDisabledTextPrimaryBackground,
        ),
        enabled = isEnabled,
    ) {
        Text(
            text = text,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HhPrimaryButtonPreview() {
    EmmTheme {
        Column(
            modifier = Modifier.fillMaxWidth()
                .background(HhBackgroundColor)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HhPrimaryButton(
                text = "Primary Button",
                isEnabled = true,
                onClick = {},
                modifier = Modifier.fillMaxWidth()
            )

            HhPrimaryButton(
                text = "Primary Button Disabled",
                isEnabled = false,
                onClick = {},
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}