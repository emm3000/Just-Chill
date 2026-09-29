package com.emm.justchill.core.ui.category

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
import com.emm.justchill.core.presentation.category.IconCatalog

val IconCatalog.icon: ImageVector
    get() = vectorByIcon.getValue(this)

private val vectorByIcon: Map<IconCatalog, ImageVector> = mapOf(
    IconCatalog.Food to Icons.Rounded.Restaurant,
    IconCatalog.FastFood to Icons.Rounded.Fastfood,
    IconCatalog.Coffee to Icons.Rounded.LocalCafe,
    IconCatalog.Bar to Icons.Rounded.LocalBar,
    IconCatalog.Pizza to Icons.Rounded.LocalPizza,
    IconCatalog.IceCream to Icons.Rounded.Icecream,
    IconCatalog.Bakery to Icons.Rounded.BakeryDining,
    IconCatalog.Groceries to Icons.Rounded.ShoppingCart,
    IconCatalog.Water to Icons.Rounded.WaterDrop,

    IconCatalog.Home to Icons.Rounded.Home,
    IconCatalog.Rent to Icons.Rounded.House,
    IconCatalog.Mortgage to Icons.Rounded.AccountBalance,
    IconCatalog.Furniture to Icons.Rounded.Chair,
    IconCatalog.Repairs to Icons.Rounded.Build,
    IconCatalog.Cleaning to Icons.Rounded.CleaningServices,
    IconCatalog.Utilities to Icons.Rounded.FlashOn,
    IconCatalog.WaterService to Icons.Rounded.Opacity,
    IconCatalog.Internet to Icons.Rounded.Wifi,

    IconCatalog.Car to Icons.Rounded.DirectionsCar,
    IconCatalog.Taxi to Icons.Rounded.LocalTaxi,
    IconCatalog.Bus to Icons.Rounded.DirectionsBus,
    IconCatalog.Train to Icons.Rounded.Train,
    IconCatalog.Bike to Icons.AutoMirrored.Rounded.DirectionsBike,
    IconCatalog.Fuel to Icons.Rounded.LocalGasStation,
    IconCatalog.Parking to Icons.Rounded.LocalParking,
    IconCatalog.Flight to Icons.Rounded.Flight,
    IconCatalog.Ship to Icons.Rounded.DirectionsBoat,

    IconCatalog.Shopping to Icons.Rounded.ShoppingBag,
    IconCatalog.Clothes to Icons.Rounded.Checkroom,
    IconCatalog.Shoes to Icons.Rounded.Hiking,
    IconCatalog.Electronics to Icons.Rounded.Devices,
    IconCatalog.Phone to Icons.Rounded.Smartphone,
    IconCatalog.Computer to Icons.Rounded.Computer,
    IconCatalog.Gift to Icons.Rounded.CardGiftcard,

    IconCatalog.Games to Icons.Rounded.SportsEsports,
    IconCatalog.Movies to Icons.Rounded.Movie,
    IconCatalog.Music to Icons.Rounded.MusicNote,
    IconCatalog.Concert to Icons.Rounded.Festival,
    IconCatalog.Books to Icons.AutoMirrored.Rounded.MenuBook,
    IconCatalog.Streaming to Icons.Rounded.LiveTv,
    IconCatalog.Party to Icons.Rounded.Celebration,

    IconCatalog.Hospital to Icons.Rounded.LocalHospital,
    IconCatalog.Pharmacy to Icons.Rounded.MedicalServices,
    IconCatalog.Fitness to Icons.Rounded.FitnessCenter,
    IconCatalog.MentalHealth to Icons.Rounded.Psychology,
    IconCatalog.Dental to Icons.Rounded.MedicalInformation,

    IconCatalog.Salary to Icons.Rounded.Payments,
    IconCatalog.Freelance to Icons.Rounded.Laptop,
    IconCatalog.Business to Icons.Rounded.BusinessCenter,
    IconCatalog.Bonus to Icons.AutoMirrored.Rounded.TrendingUp,
    IconCatalog.Tips to Icons.Rounded.VolunteerActivism,

    IconCatalog.Taxes to Icons.AutoMirrored.Rounded.ReceiptLong,
    IconCatalog.CreditCard to Icons.Rounded.CreditCard,
    IconCatalog.Savings to Icons.Rounded.Savings,
    IconCatalog.Investment to Icons.AutoMirrored.Rounded.ShowChart,
    IconCatalog.Loan to Icons.Rounded.AccountBalance,
    IconCatalog.Insurance to Icons.Rounded.Security,
    IconCatalog.Wallet to Icons.Rounded.AccountBalanceWallet,

    IconCatalog.Family to Icons.Rounded.People,
    IconCatalog.Baby to Icons.Rounded.ChildCare,
    IconCatalog.Pets to Icons.Rounded.Pets,
    IconCatalog.Beauty to Icons.Rounded.Spa,
    IconCatalog.Education to Icons.Rounded.School,
    IconCatalog.Dating to Icons.Rounded.Favorite,

    IconCatalog.Tools to Icons.Rounded.Handyman,
    IconCatalog.Subscriptions to Icons.Rounded.Autorenew,
    IconCatalog.Cloud to Icons.Rounded.Cloud,
    IconCatalog.Security to Icons.Rounded.Shield,
    IconCatalog.Documents to Icons.Rounded.Description,
    IconCatalog.Settings to Icons.Rounded.Settings,
)
