@preconcurrency import JustChillKit
import SwiftUI

extension LoansScreen {
    struct TopBar: View {
        let title: String
        let onBack: () -> Void
        var onAdd: (() -> Void)?

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
                    if let onAdd {
                        barButton(symbol: "plus", label: "Nuevo préstamo", action: onAdd)
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

    struct PersonRow: View {
        let person: PersonRowUi
        let onOpen: () -> Void

        var body: some View {
            VStack(spacing: EmmSpacing.s0) {
                Button(action: onOpen) { layout }
                    .buttonStyle(.plain)
                    .accessibilityHint("Ver los préstamos de esta persona")
                Hairline()
            }
        }

        private var layout: some View {
            ViewThatFits(in: .horizontal) {
                HStack(spacing: EmmSpacing.s3) {
                    tile
                    name
                        .frame(maxWidth: .infinity, alignment: .leading)
                    remaining
                    chevron
                }
                VStack(alignment: .leading, spacing: EmmSpacing.s1) {
                    HStack(alignment: .top, spacing: EmmSpacing.s3) {
                        tile
                        name
                            .frame(maxWidth: .infinity, alignment: .leading)
                        chevron
                    }
                    remaining
                }
            }
            .padding(.horizontal, EmmSpacing.s5)
            .padding(.vertical, EmmSpacing.s3)
            .contentShape(Rectangle())
            .accessibilityElement(children: .combine)
        }

        private var tile: some View {
            Image(systemName: "person")
                .resizable()
                .scaledToFit()
                .frame(width: EmmSpacing.s4, height: EmmSpacing.s4)
                .foregroundStyle(EmmColors.textSecondary)
                .frame(width: EmmSpacing.s8, height: EmmSpacing.s8)
                .overlay { EmmRadii.rXS.stroke(EmmColors.border, lineWidth: EmmSpacing.hairline) }
                .accessibilityHidden(true)
        }

        private var name: some View {
            Text(person.personName)
                .emmTextStyle(EmmType.titleM)
                .foregroundStyle(person.isSettled ? EmmColors.textTertiary : EmmColors.textPrimary)
                .multilineTextAlignment(.leading)
        }

        private var remaining: some View {
            VStack(alignment: .trailing, spacing: EmmSpacing.s1) {
                Text(person.remaining)
                    .emmTextStyle(EmmType.amountM)
                    .foregroundStyle(remainingColor)
                    .fixedSize(horizontal: true, vertical: false)
                if person.isSettled {
                    SettledPill()
                }
            }
        }

        private var chevron: some View {
            Image(systemName: "chevron.right")
                .resizable()
                .scaledToFit()
                .frame(width: EmmSpacing.s3, height: EmmSpacing.s3)
                .foregroundStyle(EmmColors.textTertiary)
                .accessibilityHidden(true)
        }

        private var remainingColor: Color {
            switch person.tone {
            case .muted: EmmColors.textTertiary
            case .positive: EmmColors.success
            case .neutral: EmmColors.textPrimary
            }
        }
    }

    struct SettledPill: View {
        var body: some View {
            Text("Liquidado")
                .emmTextStyle(EmmType.labelM)
                .foregroundStyle(EmmColors.success)
                .lineLimit(1)
                .fixedSize()
                .padding(.horizontal, EmmSpacing.s2)
                .padding(.vertical, EmmSpacing.s1)
                .overlay { EmmRadii.rFull.stroke(EmmColors.success, lineWidth: EmmSpacing.hairline) }
        }
    }

    struct EmptyState: View {
        var body: some View {
            VStack(spacing: EmmSpacing.s0) {
                Image(systemName: "person.2")
                    .resizable()
                    .scaledToFit()
                    .frame(width: EmmSpacing.s10, height: EmmSpacing.s10)
                    .foregroundStyle(EmmColors.textTertiary)
                    .accessibilityHidden(true)
                Text("Aún sin préstamos")
                    .emmTextStyle(EmmType.titleL)
                    .foregroundStyle(EmmColors.textPrimary)
                    .padding(.top, EmmSpacing.s4)
                Text("Lo que prestes y te devuelvan aparece aquí, por persona")
                    .emmTextStyle(EmmType.bodyM)
                    .foregroundStyle(EmmColors.textSecondary)
                    .multilineTextAlignment(.center)
                    .padding(.top, EmmSpacing.s2)
            }
            .padding(.horizontal, EmmSpacing.s6)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
    }
}
