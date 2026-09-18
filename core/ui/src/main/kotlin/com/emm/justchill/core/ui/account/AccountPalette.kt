package com.emm.justchill.core.ui.account

import androidx.compose.ui.graphics.Color
import com.emm.justchill.core.ui.theme.EmmColors

fun accountDotColor(name: String, colors: EmmColors): Color {
    val lower: String = name.lowercase()
    return when {
        "yape" in lower -> colors.catMauve
        "plin" in lower -> colors.catSage
        "bcp" in lower -> colors.catSlate
        "bbva" in lower -> colors.catTerracotta
        "interbank" in lower -> colors.catOchre
        "scotiabank" in lower -> colors.catMauve
        "efectivo" in lower || "cash" in lower -> colors.catOchre
        else -> colors.catGraphite
    }
}
