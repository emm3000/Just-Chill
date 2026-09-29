@preconcurrency import JustChillKit
import SwiftUI

extension CaptureScreen {
    struct AccountSheet: View {
        let accounts: [Account]
        let selected: Account?
        let onSelect: (Account) -> Void
        let onDismiss: () -> Void

        var body: some View {
            VStack(spacing: EmmSpacing.s0) {
                SheetTitleBar(title: "Selecciona cuenta", onClose: onDismiss)
                if accounts.isEmpty {
                    SheetEmptyText(text: "Sin cuentas. Cierra y crea una primero.")
                } else {
                    ScrollView {
                        LazyVStack(spacing: EmmSpacing.s0) {
                            ForEach(accounts, id: \.self) { account in
                                AccountRow(account: account, isActive: account == selected) {
                                    onSelect(account)
                                    onDismiss()
                                }
                            }
                        }
                        .padding(.bottom, EmmSpacing.s4)
                    }
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            .background(EmmColors.bg)
            .presentationBackground(EmmColors.bg)
            .presentationDetents([.medium, .large])
        }
    }

    struct AccountRow: View {
        let account: Account
        let isActive: Bool
        let onSelect: () -> Void

        var body: some View {
            Button(action: onSelect) {
                HStack(spacing: EmmSpacing.s4) {
                    Image(systemName: symbol)
                        .resizable()
                        .scaledToFit()
                        .frame(width: EmmSpacing.s5, height: EmmSpacing.s5)
                        .foregroundStyle(EmmColors.textSecondary)
                        .frame(width: EmmSpacing.s10, height: EmmSpacing.s10)
                        .overlay { EmmRadii.rM.stroke(EmmColors.border, lineWidth: EmmSpacing.hairline) }
                        .accessibilityHidden(true)
                    VStack(alignment: .leading, spacing: EmmSpacing.s0) {
                        Text(account.name)
                            .emmTextStyle(EmmType.titleM)
                            .foregroundStyle(EmmColors.textPrimary)
                        Text(typeLabel)
                            .emmTextStyle(EmmType.caption)
                            .foregroundStyle(EmmColors.textTertiary)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    if isActive {
                        SelectedMark()
                    }
                }
                .padding(.horizontal, EmmSpacing.s6)
                .padding(.vertical, EmmSpacing.s3)
                .background(isActive ? EmmColors.surface1 : Color.clear)
                .contentShape(Rectangle())
            }
            .accessibilityElement(children: .combine)
            .accessibilityAddTraits(isActive ? .isSelected : [])
        }

        private var symbol: String {
            switch account.type {
            case .bank, .investment: "building.columns"
            case .cash: "dollarsign"
            case .creditCard: "creditcard"
            case .wallet: "wallet.bifold"
            }
        }

        private var typeLabel: String {
            switch account.type {
            case .bank: "Banco"
            case .cash: "Efectivo"
            case .creditCard: "Crédito"
            case .investment: "Inversión"
            case .wallet: "Billetera"
            }
        }
    }

    struct CategorySheet: View {
        let categories: [SelectableCategory]
        let frequent: [SelectableCategory]
        let other: [SelectableCategory]
        let selected: SelectableCategory?
        let search: (String) -> [SelectableCategory]
        let onSelect: (SelectableCategory) -> Void
        let onDismiss: () -> Void
        @State private var query: String = ""

        var body: some View {
            VStack(spacing: EmmSpacing.s0) {
                SheetTitleBar(title: "Selecciona categoría", onClose: onDismiss)
                searchField
                if categories.isEmpty {
                    SheetEmptyText(text: "Sin categorías. Crea una primero.")
                } else {
                    ScrollView {
                        LazyVStack(spacing: EmmSpacing.s0) { rows }
                            .padding(.bottom, EmmSpacing.s4)
                    }
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            .background(EmmColors.bg)
            .presentationBackground(EmmColors.bg)
            .presentationDetents([.large])
        }

        @ViewBuilder
        private var rows: some View {
            if isSearching || frequent.isEmpty {
                categoryRows(isSearching ? search(trimmedQuery) : categories)
            } else {
                SectionEyebrow(text: "Frecuentes")
                categoryRows(frequent)
                SectionEyebrow(text: "Todas")
                categoryRows(other)
            }
        }

        private func categoryRows(_ categories: [SelectableCategory]) -> some View {
            ForEach(categories, id: \.self) { category in
                CategoryRow(category: category, isActive: category == selected) {
                    onSelect(category)
                    onDismiss()
                }
            }
        }

        private var trimmedQuery: String {
            query.trimmingCharacters(in: .whitespaces)
        }

        private var isSearching: Bool {
            !trimmedQuery.isEmpty
        }

        private var searchField: some View {
            HStack(spacing: EmmSpacing.s3) {
                Image(systemName: "magnifyingglass")
                    .resizable()
                    .scaledToFit()
                    .frame(width: EmmSpacing.s4, height: EmmSpacing.s4)
                    .foregroundStyle(EmmColors.textTertiary)
                    .accessibilityHidden(true)
                TextField(
                    "",
                    text: $query,
                    prompt: Text("Buscar entre \(categories.count) categorías")
                        .foregroundStyle(EmmColors.textTertiary)
                )
                .emmTextStyle(EmmType.bodyM)
                .foregroundStyle(EmmColors.textPrimary)
                .tint(EmmColors.borderFocus)
                .autocorrectionDisabled()
            }
            .padding(EmmSpacing.s3)
            .background(EmmColors.surface1, in: EmmRadii.rM)
            .overlay { EmmRadii.rM.stroke(EmmColors.border, lineWidth: EmmSpacing.hairline) }
            .padding(.horizontal, EmmSpacing.s5)
            .padding(.bottom, EmmSpacing.s4)
        }
    }

    struct CategoryRow: View {
        let category: SelectableCategory
        let isActive: Bool
        let onSelect: () -> Void

        var body: some View {
            Button(action: onSelect) {
                HStack(spacing: EmmSpacing.s4) {
                    Image(systemName: EmmCategory.resolvedSymbol(category.iconId))
                        .resizable()
                        .scaledToFit()
                        .frame(width: EmmSpacing.s3, height: EmmSpacing.s3)
                        .foregroundStyle(EmmColors.textSecondary)
                        .frame(width: EmmSpacing.s6, height: EmmSpacing.s6)
                        .overlay { EmmRadii.rXS.stroke(EmmColors.border, lineWidth: EmmSpacing.hairline) }
                        .accessibilityHidden(true)
                    HStack(alignment: .firstTextBaseline, spacing: EmmSpacing.s2) {
                        Circle()
                            .fill(EmmCategory.resolvedColor(category.colorId))
                            .frame(width: EmmSpacing.s2, height: EmmSpacing.s2)
                            .alignmentGuide(.firstTextBaseline) { dimensions in dimensions[.bottom] }
                        Text(category.name)
                            .emmTextStyle(EmmType.titleM)
                            .foregroundStyle(EmmColors.textPrimary)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    if isActive {
                        SelectedMark()
                    }
                }
                .padding(.horizontal, EmmSpacing.s6)
                .padding(.vertical, EmmSpacing.s3)
                .background(isActive ? EmmColors.surface1 : Color.clear)
                .contentShape(Rectangle())
            }
            .accessibilityElement(children: .combine)
            .accessibilityAddTraits(isActive ? .isSelected : [])
        }
    }

    struct SectionEyebrow: View {
        let text: String

        var body: some View {
            Text(text.uppercased())
                .emmTextStyle(EmmType.eyebrow)
                .foregroundStyle(EmmColors.textTertiary)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, EmmSpacing.s6)
                .padding(.top, EmmSpacing.s3)
                .padding(.bottom, EmmSpacing.s2)
                .accessibilityAddTraits(.isHeader)
        }
    }

    struct SelectedMark: View {
        var body: some View {
            Image(systemName: "checkmark")
                .resizable()
                .scaledToFit()
                .frame(width: EmmSpacing.s3, height: EmmSpacing.s3)
                .foregroundStyle(EmmColors.textPrimary)
                .frame(width: EmmSpacing.s6, height: EmmSpacing.s6)
                .background(EmmColors.surface3, in: Circle())
                .accessibilityHidden(true)
        }
    }

    struct SheetEmptyText: View {
        let text: String

        var body: some View {
            Text(text)
                .emmTextStyle(EmmType.bodyM)
                .foregroundStyle(EmmColors.textTertiary)
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
                .padding(EmmSpacing.s6)
        }
    }
}
