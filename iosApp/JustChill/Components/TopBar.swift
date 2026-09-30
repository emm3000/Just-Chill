import SwiftUI

struct TopBar: View {
    struct Action {
        enum Tone {
            case neutral
            case danger
        }

        let symbol: String
        let label: String
        var tone: Tone = .neutral
        let perform: () -> Void
    }

    let title: String
    let onBack: () -> Void
    var trailing: [Action] = []

    var body: some View {
        VStack(spacing: EmmSpacing.s0) {
            HStack(spacing: EmmSpacing.s2) {
                barButton(symbol: "chevron.left", label: "Volver", color: EmmColors.textPrimary, action: onBack)
                Text(title)
                    .emmTextStyle(EmmType.titleL)
                    .foregroundStyle(EmmColors.textPrimary)
                    .lineLimit(1)
                    .minimumScaleFactor(0.5)
                    .accessibilityAddTraits(.isHeader)
                    .frame(maxWidth: .infinity, minHeight: EmmSpacing.s16, alignment: .leading)
                ForEach(trailing, id: \.label) { action in
                    barButton(
                        symbol: action.symbol,
                        label: action.label,
                        color: color(for: action.tone),
                        action: action.perform
                    )
                }
            }
            .padding(.horizontal, EmmSpacing.s2)
            Hairline()
        }
    }

    private func color(for tone: Action.Tone) -> Color {
        switch tone {
        case .neutral: EmmColors.textPrimary
        case .danger: EmmColors.danger
        }
    }

    private func barButton(symbol: String, label: String, color: Color, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: symbol)
                .resizable()
                .scaledToFit()
                .frame(width: EmmSpacing.s5, height: EmmSpacing.s5)
                .foregroundStyle(color)
                .frame(width: EmmSpacing.s12, height: EmmSpacing.s12)
                .contentShape(Rectangle())
        }
        .accessibilityLabel(label)
    }
}
