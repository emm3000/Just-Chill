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
        IconCatalog("food", "Restaurante", Icons.Rounded.Restaurant, listOf("comida", "almuerzo", "cena", "menú", "restaurant")),
        IconCatalog("fast_food", "Comida rápida", Icons.Rounded.Fastfood, listOf("hamburguesa", "fast food", "pollo broaster", "salchipapa")),
        IconCatalog("coffee", "Cafetería", Icons.Rounded.LocalCafe, listOf("café", "cafecito", "capuccino", "latte")),
        IconCatalog("bar", "Bar", Icons.Rounded.LocalBar, listOf("bar", "cerveza", "trago", "alcohol")),
        IconCatalog("pizza", "Pizzería", Icons.Rounded.LocalPizza, listOf("pizza", "italiano")),
        IconCatalog("ice_cream", "Heladería", Icons.Rounded.Icecream, listOf("helado", "postre", "paleta")),
        IconCatalog("bakery", "Panadería", Icons.Rounded.BakeryDining, listOf("pan", "panadería", "pastelería", "keke")),
        IconCatalog("groceries", "Supermercado", Icons.Rounded.ShoppingCart, listOf("supermercado", "mercado", "bodega", "víveres")),
        IconCatalog("water", "Agua", Icons.Rounded.WaterDrop, listOf("agua", "botella", "bidón")),

        IconCatalog("home", "Hogar", Icons.Rounded.Home, listOf("hogar", "casa", "departamento")),
        IconCatalog("rent", "Alquiler", Icons.Rounded.House, listOf("alquiler", "renta", "cuarto")),
        IconCatalog("mortgage", "Hipoteca", Icons.Rounded.AccountBalance, listOf("hipoteca", "banco", "crédito hipotecario")),
        IconCatalog("furniture", "Muebles", Icons.Rounded.Chair, listOf("muebles", "sillón", "mesa")),
        IconCatalog("repairs", "Reparaciones", Icons.Rounded.Build, listOf("reparaciones", "arreglos", "maestro")),
        IconCatalog("cleaning", "Limpieza", Icons.Rounded.CleaningServices, listOf("limpieza", "aseo")),
        IconCatalog("utilities", "Luz", Icons.Rounded.FlashOn, listOf("luz", "electricidad", "recibo")),
        IconCatalog("water_service", "Agua potable", Icons.Rounded.Opacity, listOf("agua", "sedapal", "servicio")),
        IconCatalog("internet", "Internet", Icons.Rounded.Wifi, listOf("internet", "wifi", "movistar", "claro")),

        IconCatalog("car", "Auto", Icons.Rounded.DirectionsCar, listOf("auto", "carro", "vehículo")),
        IconCatalog("taxi", "Taxi", Icons.Rounded.LocalTaxi, listOf("taxi", "uber", "cabify", "indrive")),
        IconCatalog("bus", "Transporte público", Icons.Rounded.DirectionsBus, listOf("bus", "micro", "combi", "corredor")),
        IconCatalog("train", "Metro", Icons.Rounded.Train, listOf("metro", "tren eléctrico")),
        IconCatalog("bike", "Bicicleta", Icons.AutoMirrored.Rounded.DirectionsBike, listOf("bicicleta", "bici")),
        IconCatalog("fuel", "Gasolina", Icons.Rounded.LocalGasStation, listOf("gasolina", "grifo", "combustible")),
        IconCatalog("parking", "Estacionamiento", Icons.Rounded.LocalParking, listOf("estacionamiento", "cochera")),
        IconCatalog("flight", "Vuelo", Icons.Rounded.Flight, listOf("vuelo", "avión", "pasaje")),
        IconCatalog("ship", "Barco", Icons.Rounded.DirectionsBoat, listOf("barco", "lancha")),

        IconCatalog("shopping", "Compras", Icons.Rounded.ShoppingBag, listOf("compras", "tienda", "mall")),
        IconCatalog("clothes", "Ropa", Icons.Rounded.Checkroom, listOf("ropa", "polos", "pantalón")),
        IconCatalog("shoes", "Calzado", Icons.Rounded.Hiking, listOf("zapatos", "zapatillas")),
        IconCatalog("electronics", "Electrónica", Icons.Rounded.Devices, listOf("electrónica", "gadgets", "tecnología")),
        IconCatalog("phone", "Celular", Icons.Rounded.Smartphone, listOf("celular", "móvil")),
        IconCatalog("computer", "Computadora", Icons.Rounded.Computer, listOf("laptop", "pc", "computadora")),
        IconCatalog("gift", "Regalos", Icons.Rounded.CardGiftcard, listOf("regalo", "cumpleaños")),

        IconCatalog("games", "Videojuegos", Icons.Rounded.SportsEsports, listOf("juegos", "play", "xbox")),
        IconCatalog("movies", "Cine", Icons.Rounded.Movie, listOf("cine", "película")),
        IconCatalog("music", "Música", Icons.Rounded.MusicNote, listOf("música", "spotify", "concierto")),
        IconCatalog("concert", "Conciertos", Icons.Rounded.Festival, listOf("concierto", "festival")),
        IconCatalog("books", "Libros", Icons.AutoMirrored.Rounded.MenuBook, listOf("libros", "lectura")),
        IconCatalog("streaming", "Streaming", Icons.Rounded.LiveTv, listOf("netflix", "prime", "streaming")),
        IconCatalog("party", "Fiestas", Icons.Rounded.Celebration, listOf("fiesta", "reunión")),

        IconCatalog("hospital", "Hospital", Icons.Rounded.LocalHospital, listOf("hospital", "emergencia", "clínica")),
        IconCatalog("pharmacy", "Farmacia", Icons.Rounded.MedicalServices, listOf("farmacia", "medicina", "botica")),
        IconCatalog("fitness", "Gimnasio", Icons.Rounded.FitnessCenter, listOf("gym", "ejercicio")),
        IconCatalog("mental_health", "Salud mental", Icons.Rounded.Psychology, listOf("psicólogo", "terapia")),
        IconCatalog("dental", "Dentista", Icons.Rounded.MedicalInformation, listOf("dentista", "odontólogo")),

        IconCatalog("salary", "Sueldo", Icons.Rounded.Payments, listOf("sueldo", "salario", "pago")),
        IconCatalog("freelance", "Freelance", Icons.Rounded.Laptop, listOf("freelance", "independiente")),
        IconCatalog("business", "Trabajo", Icons.Rounded.BusinessCenter, listOf("trabajo", "empresa", "oficina")),
        IconCatalog("bonus", "Bonos", Icons.AutoMirrored.Rounded.TrendingUp, listOf("bono", "extra")),
        IconCatalog("tips", "Propinas", Icons.Rounded.VolunteerActivism, listOf("propina", "tips")),

        IconCatalog("taxes", "Impuestos", Icons.AutoMirrored.Rounded.ReceiptLong, listOf("impuestos", "sunat", "tributos")),
        IconCatalog("credit_card", "Tarjeta de crédito", Icons.Rounded.CreditCard, listOf("tarjeta", "visa", "mastercard")),
        IconCatalog("savings", "Ahorros", Icons.Rounded.Savings, listOf("ahorros", "guardar")),
        IconCatalog("investment", "Inversiones", Icons.AutoMirrored.Rounded.ShowChart, listOf("inversión", "acciones", "crypto")),
        IconCatalog("loan", "Préstamo", Icons.Rounded.AccountBalance, listOf("préstamo", "banco", "crédito")),
        IconCatalog("insurance", "Seguro", Icons.Rounded.Security, listOf("seguro", "aseguradora")),
        IconCatalog("wallet", "Billetera", Icons.Rounded.AccountBalanceWallet, listOf("billetera", "yape", "plin")),

        IconCatalog("family", "Familia", Icons.Rounded.People, listOf("familia", "hogar")),
        IconCatalog("baby", "Bebé", Icons.Rounded.ChildCare, listOf("bebé", "niño")),
        IconCatalog("pets", "Mascotas", Icons.Rounded.Pets, listOf("mascotas", "perro", "gato")),
        IconCatalog("beauty", "Belleza", Icons.Rounded.Spa, listOf("belleza", "spa", "salón")),
        IconCatalog("education", "Educación", Icons.Rounded.School, listOf("educación", "colegio", "universidad")),
        IconCatalog("dating", "Citas", Icons.Rounded.Favorite, listOf("citas", "pareja", "amor")),

        IconCatalog("tools", "Herramientas", Icons.Rounded.Handyman, listOf("herramientas", "arreglos")),
        IconCatalog("subscriptions", "Suscripciones", Icons.Rounded.Autorenew, listOf("suscripción", "mensual")),
        IconCatalog("cloud", "Nube", Icons.Rounded.Cloud, listOf("nube", "cloud", "drive")),
        IconCatalog("security", "Seguridad", Icons.Rounded.Shield, listOf("seguridad", "protección")),
        IconCatalog("documents", "Documentos", Icons.Rounded.Description, listOf("documentos", "archivos")),
        IconCatalog("settings", "Ajustes", Icons.Rounded.Settings, listOf("ajustes", "configuración")),
    )


    fun findById(id: String): IconCatalog = catalog.firstOrNull { it.id == id } ?: catalog.first()

    fun search(query: String): List<IconCatalog> {
        return catalog.filter { iconCatalog ->
            iconCatalog.keywords.any { keyword -> keyword.contains(query, ignoreCase = true) }
        }
    }
}
