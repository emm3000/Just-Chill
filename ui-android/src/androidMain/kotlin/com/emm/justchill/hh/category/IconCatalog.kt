package com.emm.justchill.hh.category

import androidx.compose.ui.graphics.vector.ImageVector

data class IconCatalog(val id: String, val name: String, val icon: ImageVector, val keywords: List<String>) {

    val cleanKeywords: List<String>
        get() = keywords.map(String::normalizeForSearch)
}
