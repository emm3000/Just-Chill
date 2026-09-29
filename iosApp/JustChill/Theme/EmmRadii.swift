import SwiftUI

enum EmmRadii {
    static let r0: RoundedRectangle = RoundedRectangle(cornerRadius: 0)
    static let rXS: RoundedRectangle = RoundedRectangle(cornerRadius: 8)
    static let rS: RoundedRectangle = RoundedRectangle(cornerRadius: 10)
    static let rM: RoundedRectangle = RoundedRectangle(cornerRadius: 12)
    static let rL: RoundedRectangle = RoundedRectangle(cornerRadius: 14)
    static let rXL: RoundedRectangle = RoundedRectangle(cornerRadius: 16)
    static let rXXL: RoundedRectangle = RoundedRectangle(cornerRadius: 20)
    static let rLTop: UnevenRoundedRectangle = UnevenRoundedRectangle(topLeadingRadius: 20, topTrailingRadius: 20)
    static let rFull: RoundedRectangle = RoundedRectangle(cornerRadius: 999)
}
