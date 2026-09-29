import SwiftUI

enum EmmCategory {
    static func resolvedColor(_ colorId: String?) -> Color {
        switch colorId {
        case "blue": EmmColors.catSlate
        case "green", "teal": EmmColors.catSage
        case "red", "brown": EmmColors.catTerracotta
        case "purple", "pink": EmmColors.catMauve
        case "orange", "yellow": EmmColors.catOchre
        default: EmmColors.catGraphite
        }
    }

    static func resolvedSymbol(_ iconId: String?) -> String {
        guard let iconId else { return missingSymbol }
        return iconSymbols[iconId] ?? fallbackSymbol
    }

    static let fallbackSymbol: String = "fork.knife"
    static let missingSymbol: String = "questionmark"

    static let iconSymbols: [String: String] = [
        "food": "fork.knife",
        "fast_food": "takeoutbag.and.cup.and.straw",
        "coffee": "cup.and.saucer",
        "bar": "wineglass",
        "pizza": "fork.knife.circle",
        "ice_cream": "snowflake",
        "bakery": "birthday.cake",
        "groceries": "cart",
        "water": "drop",
        "home": "house",
        "rent": "key",
        "mortgage": "building.columns",
        "furniture": "sofa",
        "repairs": "wrench.and.screwdriver",
        "cleaning": "bubbles.and.sparkles",
        "utilities": "bolt",
        "water_service": "spigot",
        "internet": "wifi",
        "car": "car",
        "taxi": "car.side",
        "bus": "bus",
        "train": "tram",
        "bike": "bicycle",
        "fuel": "fuelpump",
        "parking": "parkingsign",
        "flight": "airplane",
        "ship": "ferry",
        "shopping": "bag",
        "clothes": "tshirt",
        "shoes": "shoe",
        "electronics": "laptopcomputer.and.iphone",
        "phone": "iphone",
        "computer": "desktopcomputer",
        "gift": "gift",
        "games": "gamecontroller",
        "movies": "film",
        "music": "music.note",
        "concert": "music.mic",
        "books": "book",
        "streaming": "tv",
        "party": "party.popper",
        "hospital": "cross.case",
        "pharmacy": "pills",
        "fitness": "dumbbell",
        "mental_health": "brain.head.profile",
        "dental": "mouth",
        "salary": "banknote",
        "freelance": "laptopcomputer",
        "business": "briefcase",
        "bonus": "chart.line.uptrend.xyaxis",
        "tips": "hands.and.sparkles",
        "taxes": "doc.text",
        "credit_card": "creditcard",
        "savings": "tray.and.arrow.down",
        "investment": "chart.xyaxis.line",
        "loan": "building.columns",
        "insurance": "lock.shield",
        "wallet": "wallet.pass",
        "family": "person.2",
        "baby": "stroller",
        "pets": "pawprint",
        "beauty": "sparkles",
        "education": "graduationcap",
        "dating": "heart",
        "tools": "hammer",
        "subscriptions": "arrow.triangle.2.circlepath",
        "cloud": "cloud",
        "security": "shield",
        "documents": "doc",
        "settings": "gearshape",
    ]
}
