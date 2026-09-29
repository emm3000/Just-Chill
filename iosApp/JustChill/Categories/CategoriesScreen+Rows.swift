@preconcurrency import JustChillKit
import SwiftUI

extension CategoriesScreen {
    struct TopBar: View {
        let onBack: () -> Void
        let onAdd: () -> Void

        var body: some View {
            VStack(spacing: EmmSpacing.s0) {
                HStack(spacing: EmmSpacing.s2) {
                    barButton(symbol: "chevron.left", label: "Volver", action: onBack)
                    Text("Categorías")
                        .emmTextStyle(EmmType.titleL)
                        .foregroundStyle(EmmColors.textPrimary)
                        .lineLimit(1)
                        .minimumScaleFactor(0.5)
                        .accessibilityAddTraits(.isHeader)
                        .frame(maxWidth: .infinity, minHeight: EmmSpacing.s16, alignment: .leading)
                    barButton(symbol: "plus", label: "Nueva categoría", action: onAdd)
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

    struct SectionHeader: View {
        let label: String
        let count: Int

        var body: some View {
            Eyebrow(text: label + " · " + String(count))
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, EmmSpacing.s5)
                .padding(.top, EmmSpacing.s5)
                .padding(.bottom, EmmSpacing.s2)
        }
    }

    struct CategoryRow: View {
        let row: CategoryRowUi
        let onEdit: () -> Void

        var body: some View {
            Button(action: onEdit) {
                RowLayout(
                    symbol: EmmCategory.resolvedSymbol(row.iconId),
                    color: EmmCategory.resolvedColor(row.colorId),
                    name: row.name,
                    countLabel: row.movementCountLabel,
                    isMuted: false
                )
            }
            .accessibilityElement(children: .combine)
            .accessibilityHint("Editar categoría")
        }
    }

    struct UncategorizedRow: View {
        let countLabel: String

        var body: some View {
            RowLayout(
                symbol: EmmCategory.resolvedSymbol(nil),
                color: EmmCategory.resolvedColor(nil),
                name: "Sin categoría",
                countLabel: countLabel,
                isMuted: true
            )
            .accessibilityElement(children: .combine)
        }
    }

    struct RowLayout: View {
        let symbol: String
        let color: Color
        let name: String
        let countLabel: String
        let isMuted: Bool

        var body: some View {
            HStack(spacing: EmmSpacing.s3) {
                Image(systemName: symbol)
                    .resizable()
                    .scaledToFit()
                    .frame(width: EmmSpacing.s4, height: EmmSpacing.s4)
                    .foregroundStyle(EmmColors.textSecondary)
                    .frame(width: EmmSpacing.s8, height: EmmSpacing.s8)
                    .background(EmmColors.surface2, in: EmmRadii.rXS)
                    .accessibilityHidden(true)
                HStack(alignment: .firstTextBaseline, spacing: EmmSpacing.s2) {
                    Circle()
                        .fill(color)
                        .frame(width: EmmSpacing.s2, height: EmmSpacing.s2)
                        .alignmentGuide(.firstTextBaseline) { dimensions in dimensions[.bottom] }
                        .accessibilityHidden(true)
                    Text(name)
                        .emmTextStyle(EmmType.titleM)
                        .foregroundStyle(isMuted ? EmmColors.textTertiary : EmmColors.textPrimary)
                        .multilineTextAlignment(.leading)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                Text(countLabel)
                    .emmTextStyle(EmmType.labelM)
                    .foregroundStyle(isMuted ? EmmColors.textDisabled : EmmColors.textTertiary)
                    .fixedSize()
                Image(systemName: "chevron.right")
                    .resizable()
                    .scaledToFit()
                    .frame(width: EmmSpacing.s3, height: EmmSpacing.s3)
                    .foregroundStyle(isMuted ? EmmColors.textDisabled : EmmColors.textTertiary)
                    .accessibilityHidden(true)
            }
            .padding(.horizontal, EmmSpacing.s5)
            .padding(.vertical, EmmSpacing.s3)
            .contentShape(Rectangle())
        }
    }

    struct EmptyState: View {
        let onCreate: () -> Void

        var body: some View {
            VStack(spacing: EmmSpacing.s0) {
                Image(systemName: "square.grid.2x2")
                    .resizable()
                    .scaledToFit()
                    .frame(width: EmmSpacing.s10, height: EmmSpacing.s10)
                    .foregroundStyle(EmmColors.textTertiary)
                    .accessibilityHidden(true)
                Text("Aún sin categorías")
                    .emmTextStyle(EmmType.headlineM)
                    .foregroundStyle(EmmColors.textPrimary)
                    .padding(.top, EmmSpacing.s4)
                Text("Crea una para empezar a clasificar tus movimientos")
                    .emmTextStyle(EmmType.bodyM)
                    .foregroundStyle(EmmColors.textSecondary)
                    .multilineTextAlignment(.center)
                    .padding(.top, EmmSpacing.s2)
                Button(action: onCreate) {
                    Text("Crear categoría")
                        .emmTextStyle(EmmType.labelL)
                        .foregroundStyle(EmmColors.bg)
                        .frame(maxWidth: .infinity, minHeight: EmmSpacing.s12)
                        .background(EmmColors.textPrimary, in: EmmRadii.rL)
                }
                .padding(.top, EmmSpacing.s6)
            }
            .padding(.horizontal, EmmSpacing.s5)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
    }
}
