import SwiftUI

enum EmmColors {
    static let bg: Color = Color(argb: 0xFF000000)
    static let surface1: Color = Color(argb: 0xFF202020)
    static let surface2: Color = Color(argb: 0xFF262626)
    static let surface3: Color = Color(argb: 0xFF2D2D2D)
    static let border: Color = Color(argb: 0xFF2E2E2E)
    static let borderFocus: Color = Color(argb: 0xFF767676)

    static let textPrimary: Color = Color(argb: 0xFFE6E6E6)
    static let textSecondary: Color = Color(argb: 0xFFA8A8A8)
    static let textTertiary: Color = Color(argb: 0xFF767676)
    static let textDisabled: Color = Color(argb: 0xFF4A4A4A)

    static let success: Color = Color(argb: 0xFF6FA876)
    static let warning: Color = Color(argb: 0xFFB3935A)
    static let danger: Color = Color(argb: 0xFFCB6A5C)
    static let info: Color = Color(argb: 0xFF7C8B99)

    static let posMuted: Color = Color(argb: 0x246FA876)
    static let negMuted: Color = Color(argb: 0x24CB6A5C)

    static let catSlate: Color = Color(argb: 0xFF7C8B99)
    static let catSage: Color = Color(argb: 0xFF7FA075)
    static let catTerracotta: Color = Color(argb: 0xFFC97A5C)
    static let catMauve: Color = Color(argb: 0xFF9C7A95)
    static let catOchre: Color = Color(argb: 0xFFB3935A)
    static let catGraphite: Color = Color(argb: 0xFF7A7A7A)
}

private extension Color {
    init(argb: UInt32) {
        self.init(
            .sRGB,
            red: Double((argb >> 16) & 0xFF) / 255,
            green: Double((argb >> 8) & 0xFF) / 255,
            blue: Double(argb & 0xFF) / 255,
            opacity: Double((argb >> 24) & 0xFF) / 255
        )
    }
}
