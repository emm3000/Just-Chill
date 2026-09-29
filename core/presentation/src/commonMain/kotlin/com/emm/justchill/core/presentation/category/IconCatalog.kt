@file:Suppress("SpellCheckingInspection")

package com.emm.justchill.core.presentation.category

enum class IconCatalog(val id: String, val label: String, val keywords: List<String>) {
    Food("food", "Restaurante", listOf("comida", "almuerzo", "cena", "menú", "restaurant")),
    FastFood("fast_food", "Comida rápida", listOf("hamburguesa", "fast food", "pollo broaster", "salchipapa")),
    Coffee("coffee", "Cafetería", listOf("café", "cafecito", "capuccino", "latte")),
    Bar("bar", "Bar", listOf("bar", "cerveza", "trago", "alcohol")),
    Pizza("pizza", "Pizzería", listOf("pizza", "italiano")),
    IceCream("ice_cream", "Heladería", listOf("helado", "postre", "paleta")),
    Bakery("bakery", "Panadería", listOf("pan", "panadería", "pastelería", "keke")),
    Groceries("groceries", "Supermercado", listOf("supermercado", "mercado", "bodega", "víveres")),
    Water("water", "Agua", listOf("agua", "botella", "bidón")),

    Home("home", "Hogar", listOf("hogar", "casa", "departamento")),
    Rent("rent", "Alquiler", listOf("alquiler", "renta", "cuarto")),
    Mortgage("mortgage", "Hipoteca", listOf("hipoteca", "banco", "crédito hipotecario")),
    Furniture("furniture", "Muebles", listOf("muebles", "sillón", "mesa")),
    Repairs("repairs", "Reparaciones", listOf("reparaciones", "arreglos", "maestro")),
    Cleaning("cleaning", "Limpieza", listOf("limpieza", "aseo")),
    Utilities("utilities", "Luz", listOf("luz", "electricidad", "recibo")),
    WaterService("water_service", "Agua potable", listOf("agua", "sedapal", "servicio")),
    Internet("internet", "Internet", listOf("internet", "wifi", "movistar", "claro")),

    Car("car", "Auto", listOf("auto", "carro", "vehículo")),
    Taxi("taxi", "Taxi", listOf("taxi", "uber", "cabify", "indrive")),
    Bus("bus", "Transporte público", listOf("bus", "micro", "combi", "corredor")),
    Train("train", "Metro", listOf("metro", "tren eléctrico")),
    Bike("bike", "Bicicleta", listOf("bicicleta", "bici")),
    Fuel("fuel", "Gasolina", listOf("gasolina", "grifo", "combustible")),
    Parking("parking", "Estacionamiento", listOf("estacionamiento", "cochera")),
    Flight("flight", "Vuelo", listOf("vuelo", "avión", "pasaje")),
    Ship("ship", "Barco", listOf("barco", "lancha")),

    Shopping("shopping", "Compras", listOf("compras", "tienda", "mall")),
    Clothes("clothes", "Ropa", listOf("ropa", "polos", "pantalón")),
    Shoes("shoes", "Calzado", listOf("zapatos", "zapatillas")),
    Electronics("electronics", "Electrónica", listOf("electrónica", "gadgets", "tecnología")),
    Phone("phone", "Celular", listOf("celular", "móvil")),
    Computer("computer", "Computadora", listOf("laptop", "pc", "computadora")),
    Gift("gift", "Regalos", listOf("regalo", "cumpleaños")),

    Games("games", "Videojuegos", listOf("juegos", "play", "xbox")),
    Movies("movies", "Cine", listOf("cine", "película")),
    Music("music", "Música", listOf("música", "spotify", "concierto")),
    Concert("concert", "Conciertos", listOf("concierto", "festival")),
    Books("books", "Libros", listOf("libros", "lectura")),
    Streaming("streaming", "Streaming", listOf("netflix", "prime", "streaming")),
    Party("party", "Fiestas", listOf("fiesta", "reunión")),

    Hospital("hospital", "Hospital", listOf("hospital", "emergencia", "clínica")),
    Pharmacy("pharmacy", "Farmacia", listOf("farmacia", "medicina", "botica")),
    Fitness("fitness", "Gimnasio", listOf("gym", "ejercicio")),
    MentalHealth("mental_health", "Salud mental", listOf("psicólogo", "terapia")),
    Dental("dental", "Dentista", listOf("dentista", "odontólogo")),

    Salary("salary", "Sueldo", listOf("sueldo", "salario", "pago")),
    Freelance("freelance", "Freelance", listOf("freelance", "independiente")),
    Business("business", "Trabajo", listOf("trabajo", "empresa", "oficina")),
    Bonus("bonus", "Bonos", listOf("bono", "extra")),
    Tips("tips", "Propinas", listOf("propina", "tips")),

    Taxes("taxes", "Impuestos", listOf("impuestos", "sunat", "tributos")),
    CreditCard("credit_card", "Tarjeta de crédito", listOf("tarjeta", "visa", "mastercard")),
    Savings("savings", "Ahorros", listOf("ahorros", "guardar")),
    Investment("investment", "Inversiones", listOf("inversión", "acciones", "crypto")),
    Loan("loan", "Préstamo", listOf("préstamo", "banco", "crédito")),
    Insurance("insurance", "Seguro", listOf("seguro", "aseguradora")),
    Wallet("wallet", "Billetera", listOf("billetera", "yape", "plin")),

    Family("family", "Familia", listOf("familia", "hogar")),
    Baby("baby", "Bebé", listOf("bebé", "niño")),
    Pets("pets", "Mascotas", listOf("mascotas", "perro", "gato")),
    Beauty("beauty", "Belleza", listOf("belleza", "spa", "salón")),
    Education("education", "Educación", listOf("educación", "colegio", "universidad")),
    Dating("dating", "Citas", listOf("citas", "pareja", "amor")),

    Tools("tools", "Herramientas", listOf("herramientas", "arreglos")),
    Subscriptions("subscriptions", "Suscripciones", listOf("suscripción", "mensual")),
    Cloud("cloud", "Nube", listOf("nube", "cloud", "drive")),
    Security("security", "Seguridad", listOf("seguridad", "protección")),
    Documents("documents", "Documentos", listOf("documentos", "archivos")),
    Settings("settings", "Ajustes", listOf("ajustes", "configuración")),
}

object AppIconCatalog {

    val catalog: List<IconCatalog> = IconCatalog.entries

    fun findById(id: String): IconCatalog = catalog.firstOrNull { it.id == id } ?: catalog.first()

    fun search(query: String): List<IconCatalog> = catalog.filter { icon: IconCatalog ->
        icon.keywords.any { keyword: String -> keyword.contains(query, ignoreCase = true) }
    }
}
