import SwiftUI
import UIKit

struct EmmTextStyle: Sendable {
    let fontName: String
    let size: CGFloat
    let lineHeight: CGFloat
    let tracking: CGFloat
    let textStyle: Font.TextStyle
    let isTabular: Bool

    fileprivate var font: Font {
        let base = Font.custom(fontName, size: size, relativeTo: textStyle)
        return isTabular ? base.monospacedDigit() : base
    }

    fileprivate var extraLineSpacing: CGFloat {
        let naturalLineHeight: CGFloat = UIFont(name: fontName, size: size)?.lineHeight ?? size
        return max(lineHeight - naturalLineHeight, 0)
    }
}

enum EmmType {
    static let amountHero: EmmTextStyle = mono(size: 64, lineHeight: 64, tracking: -2.56, relativeTo: .largeTitle)
    static let amountL: EmmTextStyle = mono(size: 52, lineHeight: 52, tracking: -2.08, relativeTo: .largeTitle)
    static let amountCard: EmmTextStyle = mono(size: 32, lineHeight: 32, tracking: -1.28, relativeTo: .largeTitle)
    static let amountLead: EmmTextStyle = mono(size: 18, lineHeight: 24, tracking: -0.36, relativeTo: .body)
    static let amountM: EmmTextStyle = mono(size: 15, lineHeight: 20, tracking: -0.3, relativeTo: .subheadline)
    static let amountS: EmmTextStyle = mono(size: 13, lineHeight: 18, tracking: 0, relativeTo: .footnote)

    static let display: EmmTextStyle = inter(
        EmmFonts.interSemiBold, size: 36, lineHeight: 44, tracking: -0.5, relativeTo: .largeTitle)
    static let headlineL: EmmTextStyle = inter(
        EmmFonts.interSemiBold, size: 28, lineHeight: 36, tracking: -0.25, relativeTo: .title)
    static let headlineM: EmmTextStyle = inter(
        EmmFonts.interSemiBold, size: 22, lineHeight: 28, tracking: 0, relativeTo: .title2)

    static let titleL: EmmTextStyle = inter(
        EmmFonts.interSemiBold, size: 18, lineHeight: 24, tracking: 0, relativeTo: .title3)
    static let titleM: EmmTextStyle = inter(
        EmmFonts.interSemiBold, size: 16, lineHeight: 22, tracking: 0.1, relativeTo: .headline)

    static let bodyL: EmmTextStyle = inter(
        EmmFonts.interRegular, size: 16, lineHeight: 24, tracking: 0.15, relativeTo: .body)
    static let bodyM: EmmTextStyle = inter(
        EmmFonts.interRegular, size: 14, lineHeight: 20, tracking: 0.2, relativeTo: .subheadline)

    static let labelL: EmmTextStyle = inter(
        EmmFonts.interMedium, size: 14, lineHeight: 20, tracking: 0.1, relativeTo: .subheadline)
    static let labelM: EmmTextStyle = inter(
        EmmFonts.interMedium, size: 12, lineHeight: 16, tracking: 0.4, relativeTo: .caption)

    static let caption: EmmTextStyle = inter(
        EmmFonts.interRegular, size: 11, lineHeight: 16, tracking: 0.5, relativeTo: .caption2)

    static let eyebrow: EmmTextStyle = inter(
        EmmFonts.interMedium, size: 10, lineHeight: 14, tracking: 1.6, relativeTo: .caption2)

    private static func mono(
        size: CGFloat, lineHeight: CGFloat, tracking: CGFloat, relativeTo textStyle: Font.TextStyle
    ) -> EmmTextStyle {
        EmmTextStyle(
            fontName: EmmFonts.plexMonoMedium,
            size: size,
            lineHeight: lineHeight,
            tracking: tracking,
            textStyle: textStyle,
            isTabular: true
        )
    }

    private static func inter(
        _ fontName: String,
        size: CGFloat,
        lineHeight: CGFloat,
        tracking: CGFloat,
        relativeTo textStyle: Font.TextStyle
    ) -> EmmTextStyle {
        EmmTextStyle(
            fontName: fontName,
            size: size,
            lineHeight: lineHeight,
            tracking: tracking,
            textStyle: textStyle,
            isTabular: false
        )
    }
}

private struct EmmTextStyleModifier: ViewModifier {
    let style: EmmTextStyle
    @ScaledMetric private var tracking: CGFloat
    @ScaledMetric private var lineSpacing: CGFloat

    init(style: EmmTextStyle) {
        self.style = style
        _tracking = ScaledMetric(wrappedValue: style.tracking, relativeTo: style.textStyle)
        _lineSpacing = ScaledMetric(wrappedValue: style.extraLineSpacing, relativeTo: style.textStyle)
    }

    func body(content: Content) -> some View {
        content
            .font(style.font)
            .tracking(tracking)
            .lineSpacing(lineSpacing)
    }
}

extension EmmType {
    static let labelMEmphasis: Font = Font.custom(
        EmmFonts.interSemiBold, size: labelM.size, relativeTo: labelM.textStyle)
}

extension View {
    func emmTextStyle(_ style: EmmTextStyle) -> some View {
        modifier(EmmTextStyleModifier(style: style))
    }
}
