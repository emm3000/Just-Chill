import SwiftUI

struct OutlinedButton: View {
    let title: String
    var leadingSymbol: String?
    var trailingSymbol: String?
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

    private func symbol(_ name: String) -> some View {
        Image(systemName: name)
            .accessibilityHidden(true)
    }
}
