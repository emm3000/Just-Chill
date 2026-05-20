package com.emm.justchill.hh.category

import androidx.compose.ui.graphics.Color

data class CategoryColor(
    val id: String,
    val primary: Color,
    val container: Color,
    val onPrimary: Color,
    val darkContainer: Color,
)

val allColors: List<CategoryColor> = listOf(
    CategoryColor(
        id = "blue",
        primary = Color(0xFF4F8CFF),
        container = Color(0xFFE8F0FF),
        onPrimary = Color.White,
        darkContainer = Color(0xFF1C2A4A),
    ),
    CategoryColor(
        id = "purple",
        primary = Color(0xFF9B6CFF),
        container = Color(0xFFF2ECFF),
        onPrimary = Color.White,
        darkContainer = Color(0xFF2B1D4D),
    ),
    CategoryColor(
        id = "green",
        primary = Color(0xFF4CAF50),
        container = Color(0xFFE9F7EA),
        onPrimary = Color.White,
        darkContainer = Color(0xFF1B3A1D),
    ),
    CategoryColor(
        id = "yellow",
        primary = Color(0xFFFFC107),
        container = Color(0xFFFFF6D9),
        onPrimary = Color.White,
        darkContainer = Color(0xFF4A3B00),
    ),
    CategoryColor(
        id = "orange",
        primary = Color(0xFFFF8A50),
        container = Color(0xFFFFEDE4),
        onPrimary = Color.White,
        darkContainer = Color(0xFF4A2A1B),
    ),
    CategoryColor(
        id = "red",
        primary = Color(0xFFEF5350),
        container = Color(0xFFFFEBEE),
        onPrimary = Color.White,
        darkContainer = Color(0xFF3A1516),
    ),
    CategoryColor(
        id = "brown",
        primary = Color(0xFF8D6E63),
        container = Color(0xFFF3ECE9),
        onPrimary = Color.White,
        darkContainer = Color(0xFF2F1E1A),
    ),
    CategoryColor(
        id = "gray",
        primary = Color(0xFF90A4AE),
        container = Color(0xFFF1F3F4),
        onPrimary = Color.White,
        darkContainer = Color(0xFF1E2A30),
    ),
    CategoryColor(
        id = "pink",
        primary = Color(0xFFEC407A),
        container = Color(0xFFFFEBF0),
        onPrimary = Color.White,
        darkContainer = Color(0xFF3A1422),
    ),
    CategoryColor(
        id = "teal",
        primary = Color(0xFF26A69A),
        container = Color(0xFFE0F4F2),
        onPrimary = Color.White,
        darkContainer = Color(0xFF123836),
    ),
)

fun findById(id: String): CategoryColor = allColors.find { it.id == id } ?: allColors.first()
