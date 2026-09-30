import SwiftUI

struct OutlinedButton: View {
    let title: String
    var leadingSymbol: String?
    var trailingSymbol: String?
    var symbolSize: CGFloat?
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: EmmSpacing.s2) {
                if let leadingSymbol {
                    symbol(leadingSymbol)
                }
                Text(title)
                    .emmTextStyle(EmmType.titleM)
                if let trailingSymbol {
                    symbol(trailingSymbol)
                }
            }
            .foregroundStyle(EmmColors.textPrimary)
            .frame(maxWidth: .infinity, minHeight: EmmSpacing.s12 + EmmSpacing.s1)
            .overlay { EmmRadii.rL.stroke(EmmColors.border, lineWidth: EmmSpacing.hairline) }
            .contentShape(EmmRadii.rL)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(title)
        }
    }

    @ViewBuilder
    private func symbol(_ name: String) -> some View {
        if let symbolSize {
            Image(systemName: name)
                .resizable()
                .scaledToFit()
                .frame(width: symbolSize, height: symbolSize)
                .accessibilityHidden(true)
        } else {
            Image(systemName: name)
                .accessibilityHidden(true)
        }
    }
}
