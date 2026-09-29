@preconcurrency import JustChillKit
import SwiftUI

extension AddCategoryScreen {
    struct PreviewChip: View {
        let state: AddCategoryUiState

        var body: some View {
            HStack(spacing: EmmSpacing.s2) {
                Image(systemName: EmmCategory.resolvedSymbol(state.iconId))
                    .resizable()
                    .scaledToFit()
                    .frame(width: EmmSpacing.s4, height: EmmSpacing.s4)
                    .foregroundStyle(EmmColors.textSecondary)
                    .frame(width: EmmSpacing.s8, height: EmmSpacing.s8)
                    .background(EmmColors.surface3, in: EmmRadii.rS)
                Circle()
                    .fill(EmmCategory.resolvedColor(state.colorId))
                    .frame(width: EmmSpacing.s2, height: EmmSpacing.s2)
                Text(state.previewName)
                    .emmTextStyle(EmmType.titleM)
                    .foregroundStyle(state.isPreviewPlaceholder ? EmmColors.textTertiary : EmmColors.textPrimary)
                    .lineLimit(1)
                Text(state.categoryType == CategoryType.income ? "Ingreso" : "Gasto")
                    .emmTextStyle(EmmType.caption)
                    .foregroundStyle(
                        state.categoryType == CategoryType.income ? EmmColors.success : EmmColors.textSecondary
                    )
                    .padding(.horizontal, EmmSpacing.s2)
                    .padding(.vertical, EmmSpacing.s1)
                    .background(EmmColors.surface2, in: EmmRadii.rFull)
            }
            .padding(.leading, EmmSpacing.s2)
            .padding(.trailing, EmmSpacing.s3)
            .padding(.vertical, EmmSpacing.s2)
            .background(EmmColors.surface1, in: EmmRadii.rFull)
            .overlay { EmmRadii.rFull.stroke(EmmColors.border, lineWidth: EmmSpacing.hairline) }
            .accessibilityElement(children: .combine)
        }
    }

    struct TypeSegmented: View {
        let selected: CategoryType
        let onSelect: (CategoryType) -> Void

        var body: some View {
            HStack(spacing: EmmSpacing.s1) {
                cell(label: "Ingreso", type: CategoryType.income)
                cell(label: "Gasto", type: CategoryType.spend)
            }
            .padding(EmmSpacing.s1)
            .background(EmmColors.surface1, in: EmmRadii.rM)
            .overlay { EmmRadii.rM.stroke(EmmColors.border, lineWidth: EmmSpacing.hairline) }
        }

        private func cell(label: String, type: CategoryType) -> some View {
            let isSelected: Bool = selected == type
            return Button {
                onSelect(type)
            } label: {
                Text(label)
                    .emmTextStyle(EmmType.labelM)
                    .foregroundStyle(isSelected ? EmmColors.textPrimary : EmmColors.textSecondary)
                    .frame(maxWidth: .infinity, minHeight: EmmSpacing.s10)
                    .background(isSelected ? EmmColors.surface3 : Color.clear, in: EmmRadii.rXS)
                    .contentShape(Rectangle())
            }
            .accessibilityAddTraits(isSelected ? .isSelected : [])
        }
    }

    struct IconPicker: View {
        let selectedId: String
        let onSelect: (String) -> Void

        private static let rowCount: Int = 2
        private static let gridHeight: CGFloat = EmmSpacing.s12 + EmmSpacing.s2 + EmmSpacing.s12

        @State private var query: String = ""

        var body: some View {
            VStack(alignment: .leading, spacing: EmmSpacing.s3) {
                searchField
                if icons.isEmpty {
                    Text("Sin resultados para «" + query + "»")
                        .emmTextStyle(EmmType.bodyM)
                        .foregroundStyle(EmmColors.textTertiary)
                        .frame(maxWidth: .infinity, minHeight: Self.gridHeight)
                } else {
                    grid
                }
            }
        }

        private var icons: [IconCatalog] {
            AppIconCatalog.shared.search(query: query)
        }

        private var grid: some View {
            ScrollView(.horizontal, showsIndicators: false) {
                LazyHGrid(rows: rows, spacing: EmmSpacing.s2) {
                    ForEach(icons, id: \.id) { icon in
                        cell(icon)
                    }
                }
            }
            .frame(height: Self.gridHeight)
        }

        private var rows: [GridItem] {
            Array(repeating: GridItem(.fixed(EmmSpacing.s12), spacing: EmmSpacing.s2), count: Self.rowCount)
        }

        private func cell(_ icon: IconCatalog) -> some View {
            let isSelected: Bool = icon.id == selectedId
            return Button {
                onSelect(icon.id)
            } label: {
                Image(systemName: EmmCategory.resolvedSymbol(icon.id))
                    .resizable()
                    .scaledToFit()
                    .frame(width: EmmSpacing.s5, height: EmmSpacing.s5)
                    .foregroundStyle(isSelected ? EmmColors.textPrimary : EmmColors.textSecondary)
                    .frame(width: EmmSpacing.s12, height: EmmSpacing.s12)
                    .overlay {
                        EmmRadii.rM.stroke(
                            isSelected ? EmmColors.borderFocus : EmmColors.border, lineWidth: EmmSpacing.hairline)
                    }
                    .contentShape(Rectangle())
            }
            .accessibilityLabel(icon.label)
            .accessibilityAddTraits(isSelected ? .isSelected : [])
        }

        private var searchField: some View {
            HStack(spacing: EmmSpacing.s3) {
                Image(systemName: "magnifyingglass")
                    .resizable()
                    .scaledToFit()
                    .frame(width: EmmSpacing.s4, height: EmmSpacing.s4)
                    .foregroundStyle(EmmColors.textTertiary)
                    .accessibilityHidden(true)
                TextField("", text: $query, prompt: Text("Buscar").foregroundStyle(EmmColors.textTertiary))
                    .emmTextStyle(EmmType.bodyM)
                    .foregroundStyle(EmmColors.textPrimary)
                    .tint(EmmColors.borderFocus)
                    .autocorrectionDisabled()
                    .textInputAutocapitalization(.never)
            }
            .padding(EmmSpacing.s3)
            .background(EmmColors.surface1, in: EmmRadii.rM)
            .overlay { EmmRadii.rM.stroke(EmmColors.border, lineWidth: EmmSpacing.hairline) }
        }
    }

    struct ColorRow: View {
        let selectedId: String
        let onSelect: (String) -> Void

        var body: some View {
            HStack(spacing: EmmSpacing.s0) {
                ForEach(CategoryColorsKt.selectableColorIds, id: \.self) { colorId in
                    swatch(colorId)
                        .frame(maxWidth: .infinity)
                }
            }
        }

        private func swatch(_ colorId: String) -> some View {
            let isSelected: Bool = colorId == selectedId
            return Button {
                onSelect(colorId)
            } label: {
                Circle()
                    .fill(EmmCategory.resolvedColor(colorId))
                    .frame(width: EmmSpacing.s6, height: EmmSpacing.s6)
                    .frame(width: EmmSpacing.s12, height: EmmSpacing.s12)
                    .overlay {
                        Circle().stroke(
                            isSelected ? EmmColors.borderFocus : Color.clear, lineWidth: EmmSpacing.hairline)
                    }
                    .contentShape(Circle())
            }
            .accessibilityLabel(CategoryColorsKt.colorLabel(colorId: colorId))
            .accessibilityAddTraits(isSelected ? .isSelected : [])
        }
    }
}
