import SwiftUI

struct TopBar: View {
    struct Action {
        let label: String
        let perform: () -> Void
    }

    let title: String
    let onBack: () -> Void
    var add: Action?

    var body: some View {
        VStack(spacing: EmmSpacing.s0) {
            HStack(spacing: EmmSpacing.s2) {
                barButton(symbol: "chevron.left", label: "Volver", action: onBack)
                Text(title)
                    .emmTextStyle(EmmType.titleL)
                    .foregroundStyle(EmmColors.textPrimary)
                    .lineLimit(1)
                    .minimumScaleFactor(0.5)
                    .accessibilityAddTraits(.isHeader)
                    .frame(maxWidth: .infinity, minHeight: EmmSpacing.s16, alignment: .leading)
                if let add {
                    barButton(symbol: "plus", label: add.label, action: add.perform)
                }
            }
            .padding(.horizontal, EmmSpacing.s2)
            Hairline()
        }
    }

    private func barButton(symbol: String, label: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: symbol)
                .resizable()
                .scaledToFit()
                .frame(width: EmmSpacing.s5, height: EmmSpacing.s5)
                .foregroundStyle(EmmColors.textPrimary)
                .frame(width: EmmSpacing.s12, height: EmmSpacing.s12)
                .contentShape(Rectangle())
        }
        .accessibilityLabel(label)
    }
}
