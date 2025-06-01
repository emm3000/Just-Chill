package com.emm.justchill.hh.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

val AvailableColors = listOf(
    Color(0xFFEF5350), // Rojo
    Color(0xFFEC407A), // Rosa
    Color(0xFFAB47BC), // Púrpura
    Color(0xFF7E57C2), // Morado oscuro
    Color(0xFF5C6BC0), // Índigo
    Color(0xFF42A5F5), // Azul
    Color(0xFF29B6F6), // Azul claro
    Color(0xFF26C6DA), // Cian
    Color(0xFF26A69A), // Teal
    Color(0xFF66BB6A), // Verde
    Color(0xFF9CCC65), // Verde claro
    Color(0xFFD4E157), // Lima
    Color(0xFFFFEE58), // Amarillo
    Color(0xFFFFCA28), // Ámbar
    Color(0xFFFF9800), // Naranja
    Color(0xFFFFA726), // Naranja oscuro
    Color(0xFF8D6E63), // Marrón
    Color(0xFF757575), // Gris
    Color(0xFF546E7A)  // Azul grisáceo
)

val AvailableIconsMap: Map<String, ImageVector> = mapOf(
    "Fastfood" to Icons.Default.Fastfood,
    "DirectionsBus" to Icons.Default.DirectionsBus,
    "Home" to Icons.Default.Home,
    "ShoppingBag" to Icons.Default.ShoppingBag,
    "Restaurant" to Icons.Default.Restaurant,
    "FitnessCenter" to Icons.Default.FitnessCenter,
    "MedicalServices" to Icons.Default.MedicalServices,
    "School" to Icons.Default.School,
    "Computer" to Icons.Default.Computer,
    "Work" to Icons.Default.Work,
    "TravelExplore" to Icons.Default.TravelExplore,
    "Pets" to Icons.Default.Pets,
    "FamilyRestroom" to Icons.Default.FamilyRestroom,
    "Payments" to Icons.Default.Payments,
    "Savings" to Icons.Default.Savings,
    "VolunteerActivism" to Icons.Default.VolunteerActivism,
    "MoreHoriz" to Icons.Default.MoreHoriz
)

val AvailableIcons: List<ImageVector> = AvailableIconsMap.values.toList()

fun ImageVector.toIconName(): String {
    return AvailableIconsMap.entries.find { it.value == this }?.key ?: "MoreHoriz"
}

fun String.toImageVector(): ImageVector {
    return AvailableIconsMap[this] ?: Icons.Default.MoreHoriz
}

fun Color.toHexString(): String {
    val alpha = (alpha * 255).toInt().toString(16).padStart(2, '0')
    val red = (red * 255).toInt().toString(16).padStart(2, '0')
    val green = (green * 255).toInt().toString(16).padStart(2, '0')
    val blue = (blue * 255).toInt().toString(16).padStart(2, '0')
    return "#$alpha$red$green$blue".uppercase()
}

fun String.toColor(): Color {
    return try {
        val colorString = if (startsWith("#")) substring(1) else this
        val argb = colorString.toLong(16)
        Color(argb.toInt())
    } catch (e: Exception) {
        Color.Gray
    }
}