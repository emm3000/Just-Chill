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
import com.emm.justchill.core.presentation.category.CategoryIcon

val CategoryIcon.icon: ImageVector
    get() = vectorByIcon.getValue(this)

private val vectorByIcon: Map<CategoryIcon, ImageVector> = mapOf(
    CategoryIcon.Food to Icons.Rounded.Restaurant,
    CategoryIcon.FastFood to Icons.Rounded.Fastfood,
    CategoryIcon.Coffee to Icons.Rounded.LocalCafe,
    CategoryIcon.Bar to Icons.Rounded.LocalBar,
    CategoryIcon.Pizza to Icons.Rounded.LocalPizza,
    CategoryIcon.IceCream to Icons.Rounded.Icecream,
    CategoryIcon.Bakery to Icons.Rounded.BakeryDining,
    CategoryIcon.Groceries to Icons.Rounded.ShoppingCart,
    CategoryIcon.Water to Icons.Rounded.WaterDrop,

    CategoryIcon.Home to Icons.Rounded.Home,
    CategoryIcon.Rent to Icons.Rounded.House,
    CategoryIcon.Mortgage to Icons.Rounded.AccountBalance,
    CategoryIcon.Furniture to Icons.Rounded.Chair,
    CategoryIcon.Repairs to Icons.Rounded.Build,
    CategoryIcon.Cleaning to Icons.Rounded.CleaningServices,
    CategoryIcon.Utilities to Icons.Rounded.FlashOn,
    CategoryIcon.WaterService to Icons.Rounded.Opacity,
    CategoryIcon.Internet to Icons.Rounded.Wifi,

    CategoryIcon.Car to Icons.Rounded.DirectionsCar,
    CategoryIcon.Taxi to Icons.Rounded.LocalTaxi,
    CategoryIcon.Bus to Icons.Rounded.DirectionsBus,
    CategoryIcon.Train to Icons.Rounded.Train,
    CategoryIcon.Bike to Icons.AutoMirrored.Rounded.DirectionsBike,
    CategoryIcon.Fuel to Icons.Rounded.LocalGasStation,
    CategoryIcon.Parking to Icons.Rounded.LocalParking,
    CategoryIcon.Flight to Icons.Rounded.Flight,
    CategoryIcon.Ship to Icons.Rounded.DirectionsBoat,

    CategoryIcon.Shopping to Icons.Rounded.ShoppingBag,
    CategoryIcon.Clothes to Icons.Rounded.Checkroom,
    CategoryIcon.Shoes to Icons.Rounded.Hiking,
    CategoryIcon.Electronics to Icons.Rounded.Devices,
    CategoryIcon.Phone to Icons.Rounded.Smartphone,
    CategoryIcon.Computer to Icons.Rounded.Computer,
    CategoryIcon.Gift to Icons.Rounded.CardGiftcard,

    CategoryIcon.Games to Icons.Rounded.SportsEsports,
    CategoryIcon.Movies to Icons.Rounded.Movie,
    CategoryIcon.Music to Icons.Rounded.MusicNote,
    CategoryIcon.Concert to Icons.Rounded.Festival,
    CategoryIcon.Books to Icons.AutoMirrored.Rounded.MenuBook,
    CategoryIcon.Streaming to Icons.Rounded.LiveTv,
    CategoryIcon.Party to Icons.Rounded.Celebration,

    CategoryIcon.Hospital to Icons.Rounded.LocalHospital,
    CategoryIcon.Pharmacy to Icons.Rounded.MedicalServices,
    CategoryIcon.Fitness to Icons.Rounded.FitnessCenter,
    CategoryIcon.MentalHealth to Icons.Rounded.Psychology,
    CategoryIcon.Dental to Icons.Rounded.MedicalInformation,

    CategoryIcon.Salary to Icons.Rounded.Payments,
    CategoryIcon.Freelance to Icons.Rounded.Laptop,
    CategoryIcon.Business to Icons.Rounded.BusinessCenter,
    CategoryIcon.Bonus to Icons.AutoMirrored.Rounded.TrendingUp,
    CategoryIcon.Tips to Icons.Rounded.VolunteerActivism,

    CategoryIcon.Taxes to Icons.AutoMirrored.Rounded.ReceiptLong,
    CategoryIcon.CreditCard to Icons.Rounded.CreditCard,
    CategoryIcon.Savings to Icons.Rounded.Savings,
    CategoryIcon.Investment to Icons.AutoMirrored.Rounded.ShowChart,
    CategoryIcon.Loan to Icons.Rounded.AccountBalance,
    CategoryIcon.Insurance to Icons.Rounded.Security,
    CategoryIcon.Wallet to Icons.Rounded.AccountBalanceWallet,

    CategoryIcon.Family to Icons.Rounded.People,
    CategoryIcon.Baby to Icons.Rounded.ChildCare,
    CategoryIcon.Pets to Icons.Rounded.Pets,
    CategoryIcon.Beauty to Icons.Rounded.Spa,
    CategoryIcon.Education to Icons.Rounded.School,
    CategoryIcon.Dating to Icons.Rounded.Favorite,

    CategoryIcon.Tools to Icons.Rounded.Handyman,
    CategoryIcon.Subscriptions to Icons.Rounded.Autorenew,
    CategoryIcon.Cloud to Icons.Rounded.Cloud,
    CategoryIcon.Security to Icons.Rounded.Shield,
    CategoryIcon.Documents to Icons.Rounded.Description,
    CategoryIcon.Settings to Icons.Rounded.Settings,
)
