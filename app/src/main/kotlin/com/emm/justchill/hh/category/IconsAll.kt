@file:Suppress("SpellCheckingInspection")

package com.emm.justchill.hh.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsBike
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.BakeryDining
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.BusinessCenter
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.Chair
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.DirectionsBoat
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Fastfood
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Festival
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Handyman
import androidx.compose.material.icons.rounded.Hiking
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.House
import androidx.compose.material.icons.rounded.Icecream
import androidx.compose.material.icons.rounded.Laptop
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.LocalBar
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.LocalParking
import androidx.compose.material.icons.rounded.LocalPizza
import androidx.compose.material.icons.rounded.LocalTaxi
import androidx.compose.material.icons.rounded.MedicalInformation
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Train
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.ui.graphics.vector.ImageVector

data class IconCatalog(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val keywords: List<String>,
)

object AppIconCatalog {

    val catalog: List<IconCatalog> = listOf(
        IconCatalog("food", "Restaurant", Icons.Rounded.Restaurant, listOf("comida", "food", "restaurant", "almuerzo", "cena")),
        IconCatalog("fast_food", "Fastfood", Icons.Rounded.Fastfood, listOf("hamburguesa", "burger", "comida rápida", "fast food")),
        IconCatalog("coffee", "LocalCafe", Icons.Rounded.LocalCafe, listOf("café", "coffee", "capuccino", "latte")),
        IconCatalog("bar", "LocalBar", Icons.Rounded.LocalBar, listOf("bar", "alcohol", "cerveza", "drink")),
        IconCatalog("pizza", "LocalPizza", Icons.Rounded.LocalPizza, listOf("pizza", "italiano")),
        IconCatalog("ice_cream", "Icecream", Icons.Rounded.Icecream, listOf("helado", "ice cream", "postre")),
        IconCatalog("bakery", "BakeryDining", Icons.Rounded.BakeryDining, listOf("pan", "bakery", "pastelería")),
        IconCatalog("groceries", "ShoppingCart", Icons.Rounded.ShoppingCart, listOf("supermercado", "groceries", "market")),
        IconCatalog("water", "WaterDrop", Icons.Rounded.WaterDrop, listOf("agua", "water")),

        IconCatalog("home", "Home", Icons.Rounded.Home, listOf("hogar", "casa", "home")),
        IconCatalog("rent", "House", Icons.Rounded.House, listOf("renta", "alquiler", "rent")),
        IconCatalog("mortgage", "AccountBalance", Icons.Rounded.AccountBalance, listOf("hipoteca", "banco", "mortgage")),
        IconCatalog("furniture", "Chair", Icons.Rounded.Chair, listOf("muebles", "furniture")),
        IconCatalog("repairs", "Build", Icons.Rounded.Build, listOf("reparaciones", "arreglos", "tools")),
        IconCatalog("cleaning", "CleaningServices", Icons.Rounded.CleaningServices, listOf("limpieza", "cleaning")),
        IconCatalog("utilities", "FlashOn", Icons.Rounded.FlashOn, listOf("luz", "electricidad", "utilities")),
        IconCatalog("water_service", "Opacity", Icons.Rounded.Opacity, listOf("agua", "servicio")),
        IconCatalog("internet", "Wifi", Icons.Rounded.Wifi, listOf("internet", "wifi")),

        IconCatalog("car", "DirectionsCar", Icons.Rounded.DirectionsCar, listOf("auto", "carro", "car")),
        IconCatalog("taxi", "LocalTaxi", Icons.Rounded.LocalTaxi, listOf("taxi", "uber", "cab")),
        IconCatalog("bus", "DirectionsBus", Icons.Rounded.DirectionsBus, listOf("bus", "micro", "publico")),
        IconCatalog("train", "Train", Icons.Rounded.Train, listOf("tren", "metro")),
        IconCatalog("bike", "DirectionsBike", Icons.AutoMirrored.Rounded.DirectionsBike, listOf("bicicleta", "bike")),
        IconCatalog("fuel", "LocalGasStation", Icons.Rounded.LocalGasStation, listOf("gasolina", "fuel")),
        IconCatalog("parking", "LocalParking", Icons.Rounded.LocalParking, listOf("estacionamiento", "parking")),
        IconCatalog("flight", "Flight", Icons.Rounded.Flight, listOf("vuelo", "avión", "flight")),
        IconCatalog("ship", "DirectionsBoat", Icons.Rounded.DirectionsBoat, listOf("barco", "ship")),

        IconCatalog("shopping", "ShoppingBag", Icons.Rounded.ShoppingBag, listOf("compras", "shopping", "tienda")),
        IconCatalog("clothes", "Checkroom", Icons.Rounded.Checkroom, listOf("ropa", "clothes")),
        IconCatalog("shoes", "Hiking", Icons.Rounded.Hiking, listOf("zapatos", "shoes")),
        IconCatalog("electronics", "Devices", Icons.Rounded.Devices, listOf("electrónica", "gadgets")),
        IconCatalog("phone", "Smartphone", Icons.Rounded.Smartphone, listOf("celular", "phone")),
        IconCatalog("computer", "Computer", Icons.Rounded.Computer, listOf("laptop", "pc", "computer")),
        IconCatalog("gift", "CardGiftcard", Icons.Rounded.CardGiftcard, listOf("regalo", "gift")),

        IconCatalog("games", "SportsEsports", Icons.Rounded.SportsEsports, listOf("juegos", "games")),
        IconCatalog("movies", "Movie", Icons.Rounded.Movie, listOf("cine", "movies")),
        IconCatalog("music", "MusicNote", Icons.Rounded.MusicNote, listOf("música", "spotify")),
        IconCatalog("concert", "Festival", Icons.Rounded.Festival, listOf("concierto", "festival")),
        IconCatalog("books", "MenuBook", Icons.AutoMirrored.Rounded.MenuBook, listOf("libros", "books")),
        IconCatalog("streaming", "LiveTv", Icons.Rounded.LiveTv, listOf("netflix", "streaming")),
        IconCatalog("party", "Celebration", Icons.Rounded.Celebration, listOf("fiesta", "party")),

        IconCatalog("hospital", "LocalHospital", Icons.Rounded.LocalHospital, listOf("hospital", "emergencia")),
        IconCatalog("pharmacy", "MedicalServices", Icons.Rounded.MedicalServices, listOf("farmacia", "medicina")),
        IconCatalog("fitness", "FitnessCenter", Icons.Rounded.FitnessCenter, listOf("gym", "ejercicio")),
        IconCatalog("mental_health", "Psychology", Icons.Rounded.Psychology, listOf("psicólogo", "terapia")),
        IconCatalog("dental", "MedicalInformation", Icons.Rounded.MedicalInformation, listOf("dentista", "dental")),

        IconCatalog("salary", "Payments", Icons.Rounded.Payments, listOf("salario", "sueldo", "income")),
        IconCatalog("freelance", "Laptop", Icons.Rounded.Laptop, listOf("freelance", "independiente")),
        IconCatalog("business", "BusinessCenter", Icons.Rounded.BusinessCenter, listOf("empresa", "oficina")),
        IconCatalog("bonus", "TrendingUp", Icons.AutoMirrored.Rounded.TrendingUp, listOf("bono", "extra")),
        IconCatalog("tips", "VolunteerActivism", Icons.Rounded.VolunteerActivism, listOf("propina", "tips")),

        IconCatalog("taxes", "ReceiptLong", Icons.AutoMirrored.Rounded.ReceiptLong, listOf("impuestos", "taxes", "sunat")),
        IconCatalog("credit_card", "CreditCard", Icons.Rounded.CreditCard, listOf("tarjeta", "visa")),
        IconCatalog("savings", "Savings", Icons.Rounded.Savings, listOf("ahorros", "savings")),
        IconCatalog("investment", "ShowChart", Icons.AutoMirrored.Rounded.ShowChart, listOf("inversión", "stocks", "crypto")),
        IconCatalog("loan", "AccountBalance", Icons.Rounded.AccountBalance, listOf("préstamo", "banco")),
        IconCatalog("insurance", "Security", Icons.Rounded.Security, listOf("seguro", "insurance")),
        IconCatalog("wallet", "AccountBalanceWallet", Icons.Rounded.AccountBalanceWallet, listOf("billetera", "wallet")),

        IconCatalog("family", "People", Icons.Rounded.People, listOf("familia", "family")),
        IconCatalog("baby", "ChildCare", Icons.Rounded.ChildCare, listOf("bebé", "baby")),
        IconCatalog("pets", "Pets", Icons.Rounded.Pets, listOf("mascotas", "perro", "gato")),
        IconCatalog("beauty", "Spa", Icons.Rounded.Spa, listOf("belleza", "spa")),
        IconCatalog("education", "School", Icons.Rounded.School, listOf("educación", "school")),
        IconCatalog("dating", "Favorite", Icons.Rounded.Favorite, listOf("citas", "amor", "dating")),

        IconCatalog("tools", "Handyman", Icons.Rounded.Handyman, listOf("herramientas", "tools")),
        IconCatalog("subscriptions", "Autorenew", Icons.Rounded.Autorenew, listOf("suscripción", "subscription")),
        IconCatalog("cloud", "Cloud", Icons.Rounded.Cloud, listOf("nube", "cloud")),
        IconCatalog("security", "Shield", Icons.Rounded.Shield, listOf("seguridad", "security")),
        IconCatalog("documents", "Description", Icons.Rounded.Description, listOf("documentos", "docs")),
        IconCatalog("settings", "Settings", Icons.Rounded.Settings, listOf("ajustes", "settings")),
    )

    fun findById(id: String): IconCatalog = catalog.firstOrNull { it.id == id } ?: catalog.first()

    fun search(query: String): List<IconCatalog> {
        return catalog.filter { iconCatalog ->
            iconCatalog.keywords.any { keyword -> keyword.contains(query, ignoreCase = true) }
        }
    }
}
