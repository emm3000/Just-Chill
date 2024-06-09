package com.emm.retrofit.core

import android.annotation.SuppressLint
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@SuppressLint("ModifierFactoryUnreferencedReceiver")
fun Modifier.log(color: Color = Color.Red): Modifier = border(1.dp, color = color)