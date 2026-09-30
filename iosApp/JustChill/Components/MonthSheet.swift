@preconcurrency import JustChillKit
import SwiftUI

struct MonthSheet: View {
    let current: YearMonth
    let onSelect: (YearMonth) -> Void
    let onDismiss: () -> Void
    @State private var displayYear: Int32

    init(current: YearMonth, onSelect: @escaping (YearMonth) -> Void, onDismiss: @escaping () -> Void) {
        self.current = current
        self.onSelect = onSelect
        self.onDismiss = onDismiss
        _displayYear = State(initialValue: current.year)
    }

    var body: some View {
        VStack(spacing: EmmSpacing.s2) {
            SheetTitleBar(title: "Selecciona mes", onClose: onDismiss)
            HStack {
                chevron(symbol: "chevron.left", label: "Año anterior", step: -1)
                Spacer()
                Text(String(displayYear))
                    .emmTextStyle(EmmType.labelL)
                    .foregroundStyle(EmmColors.textPrimary)
                Spacer()
                chevron(symbol: "chevron.right", label: "Año siguiente", step: 1)
            }
            .padding(.horizontal, EmmSpacing.s6)
            LazyVGrid(columns: columns, spacing: EmmSpacing.s2) {
                ForEach(Kotlinx_datetimeMonth.allCases, id: \.self) { month in
                    monthCell(month)
                }
            }
            .padding(.horizontal, EmmSpacing.s4)
            .padding(.bottom, EmmSpacing.s4)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .background(EmmColors.bg)
        .presentationBackground(EmmColors.bg)
        .presentationDetents([.medium])
    }

    private var columns: [GridItem] {
        Array(repeating: GridItem(.flexible(), spacing: EmmSpacing.s2), count: 3)
    }

    private func chevron(symbol: String, label: String, step: Int32) -> some View {
        Button {
            displayYear += step
        } label: {
            Image(systemName: symbol)
                .resizable()
                .scaledToFit()
                .frame(width: EmmSpacing.s4, height: EmmSpacing.s4)
                .foregroundStyle(EmmColors.textSecondary)
                .frame(width: EmmSpacing.s12, height: EmmSpacing.s12)
        }
        .accessibilityLabel(label)
    }

    private func monthCell(_ month: Kotlinx_datetimeMonth) -> some View {
        let target = YearMonth(year: displayYear, month: month)
        let isActive: Bool = target == current
        return Button {
            onSelect(target)
            onDismiss()
        } label: {
            Text(target.monthAbbrevLabel())
                .emmTextStyle(EmmType.labelL)
                .foregroundStyle(isActive ? EmmColors.textPrimary : EmmColors.textSecondary)
                .frame(maxWidth: .infinity, minHeight: EmmSpacing.s12)
                .overlay {
                    EmmRadii.rM.stroke(
                        isActive ? EmmColors.borderFocus : EmmColors.border, lineWidth: EmmSpacing.hairline)
                }
        }
        .accessibilityAddTraits(isActive ? .isSelected : [])
    }
}
