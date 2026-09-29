import UIKit

enum EmmFonts {
    static let interRegular: String = "Inter-Regular"
    static let interMedium: String = "Inter-Medium"
    static let interSemiBold: String = "Inter-SemiBold"
    static let interBold: String = "Inter-Bold"
    static let plexMonoRegular: String = "IBMPlexMono"
    static let plexMonoMedium: String = "IBMPlexMono-Medm"
    static let plexMonoSemiBold: String = "IBMPlexMono-SmBld"

    static let all: [String] = [
        interRegular, interMedium, interSemiBold, interBold,
        plexMonoRegular, plexMonoMedium, plexMonoSemiBold,
    ]

    static func logAvailability() {
        for name in all {
            let isLoaded: Bool = UIFont(name: name, size: EmmType.bodyL.size) != nil
            print("EmmFonts \(name) loaded=\(isLoaded)")
        }
    }
}
