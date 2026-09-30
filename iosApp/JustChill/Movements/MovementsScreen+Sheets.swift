@preconcurrency import JustChillKit
import SwiftUI

extension MovementsScreen {
    struct FilterSheet: View {
        let state: SeeTransactionsUiState
        let send: Send
        @State private var segment: CategoryType
        @State private var search: String = ""

        init(state: SeeTransactionsUiState, send: @escaping Send) {
            self.state = state
            self.send = send
            let activeType: CategoryType? = state.sheetItems.first { $0.id == state.activeCategory?.id }?.type
            _segment = State(initialValue: activeType ?? CategoryType.spend)
        }

        var body: some View {
            VStack(spacing: EmmSpacing.s0) {
                SheetTitleBar(title: "Filtrar movimientos", onClose: dismiss)
                searchField
                amountSection
                segmented
                ScrollView {
                    LazyVStack(spacing: EmmSpacing.s0) {
                        ForEach(visibleItems, id: \.id) { item in
                            CategoryRow(item: item) {
                                send(SeeTransactionsIntentOnCategorySelected(categoryId: item.id))
                                dismiss()
                            }
                        }
                    }
                }
                if state.isCategoryOrAmountFilterActive {
                    OutlinedButton(title: "Limpiar filtro", leadingSymbol: "xmark", symbolSize: EmmSpacing.s3) {
                        send(SeeTransactionsIntentOnClearCategoryFilter.shared)
                        dismiss()
                    }
                    .padding(.horizontal, EmmSpacing.s4)
                    .padding(.top, EmmSpacing.s3)
                    .padding(.bottom, EmmSpacing.s4)
                }
            }
            .background(EmmColors.bg)
            .presentationBackground(EmmColors.bg)
            .presentationDetents([.large])
            .sheet(isPresented: isAmountSheetPresented) {
                AmountSheet(state: state, send: send)
            }
        }

        private var visibleItems: [CategorySheetItem] {
            let needle: String = normalized(search)
            return state.sheetItems.filter { item in
                item.type == segment && (needle.isEmpty || normalized(item.name).contains(needle))
            }
        }

        private func normalized(_ text: String) -> String {
            SpanishSearchKt.stripSpanishAccents(text.trimmingCharacters(in: .whitespaces).lowercased())
        }

        private func dismiss() {
            send(SeeTransactionsIntentScreenChromeIntentOnFilterSheetDismissed.shared)
        }

        private var isAmountSheetPresented: Binding<Bool> {
            Binding(
                get: { state.amountSheetTarget != nil },
                set: { isPresented in
                    if !isPresented { send(SeeTransactionsIntentAmountFilterIntentOnAmountSheetDismissed.shared) }
                }
            )
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
                    text: $search,
                    prompt: Text("Buscar entre \(state.sheetItems.count) categorías")
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
            .padding(.bottom, EmmSpacing.s3)
        }

        private var amountSection: some View {
            VStack(alignment: .leading, spacing: EmmSpacing.s3) {
                Text("MONTO")
                    .emmTextStyle(EmmType.eyebrow)
                    .foregroundStyle(EmmColors.textTertiary)
                AmountBoundRow(label: "Mínimo", amount: state.minAmountCents, target: AmountRangeTarget.min, send: send)
                AmountBoundRow(label: "Máximo", amount: state.maxAmountCents, target: AmountRangeTarget.max, send: send)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, EmmSpacing.s5)
            .padding(.vertical, EmmSpacing.s2)
        }

        private var segmented: some View {
            HStack(spacing: EmmSpacing.s0) {
                segmentCell(label: "Ingresos · \(state.incomeCount)", type: CategoryType.income)
                segmentCell(label: "Gastos · \(state.spendCount)", type: CategoryType.spend)
            }
            .padding(EmmSpacing.s1)
            .frame(height: EmmSpacing.s12)
            .overlay { EmmRadii.rM.stroke(EmmColors.border, lineWidth: EmmSpacing.hairline) }
            .padding(.horizontal, EmmSpacing.s5)
            .padding(.vertical, EmmSpacing.s1)
            .padding(.bottom, EmmSpacing.s2)
        }

        private func segmentCell(label: String, type: CategoryType) -> some View {
            let isSelected: Bool = segment == type
            return Button {
                segment = type
            } label: {
                Text(label)
                    .emmTextStyle(EmmType.labelL)
                    .foregroundStyle(isSelected ? EmmColors.textPrimary : EmmColors.textSecondary)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(isSelected ? EmmColors.surface2 : Color.clear, in: EmmRadii.rS)
            }
            .accessibilityAddTraits(isSelected ? .isSelected : [])
        }
    }

    struct AmountBoundRow: View {
        let label: String
        let amount: KotlinLong?
        let target: AmountRangeTarget
        let send: Send

        var body: some View {
            HStack(spacing: EmmSpacing.s0) {
                Button {
                    send(SeeTransactionsIntentAmountFilterIntentOnAmountSheetRequested(target: target))
                } label: {
                    HStack {
                        Text(label)
                            .emmTextStyle(EmmType.bodyM)
                            .foregroundStyle(EmmColors.textSecondary)
                        Spacer()
                        Text(MovementsScreen.boundText(amount) ?? "Sin límite")
                            .emmTextStyle(EmmType.amountS)
                            .foregroundStyle(EmmColors.textPrimary)
                    }
                    .contentShape(Rectangle())
                }
                if amount != nil {
                    Button {
                        send(SeeTransactionsIntentAmountFilterIntentOnAmountBoundCleared(target: target))
                    } label: {
                        Image(systemName: "xmark")
                            .resizable()
                            .scaledToFit()
                            .frame(width: EmmSpacing.s3, height: EmmSpacing.s3)
                            .foregroundStyle(EmmColors.textTertiary)
                            .frame(width: EmmSpacing.s12, height: EmmSpacing.s12)
                    }
                    .accessibilityLabel("Quitar " + label)
                }
            }
            .padding(.leading, EmmSpacing.s4)
            .padding(.trailing, amount == nil ? EmmSpacing.s4 : EmmSpacing.s0)
            .frame(height: EmmSpacing.s12)
            .background(EmmColors.surface1, in: EmmRadii.rM)
            .overlay { EmmRadii.rM.stroke(EmmColors.border, lineWidth: EmmSpacing.hairline) }
        }
    }

    struct CategoryRow: View {
        let item: CategorySheetItem
        let onSelect: () -> Void

        var body: some View {
            Button(action: onSelect) {
                HStack(spacing: EmmSpacing.s4) {
                    Image(systemName: EmmCategory.resolvedSymbol(item.iconId))
                        .resizable()
                        .scaledToFit()
                        .frame(width: EmmSpacing.s3, height: EmmSpacing.s3)
                        .foregroundStyle(EmmColors.textSecondary)
                        .frame(width: EmmSpacing.s6, height: EmmSpacing.s6)
                        .overlay { EmmRadii.rXS.stroke(EmmColors.border, lineWidth: EmmSpacing.hairline) }
                        .accessibilityHidden(true)
                    Text(item.name)
                        .emmTextStyle(EmmType.titleM)
                        .foregroundStyle(EmmColors.textPrimary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    if item.isActive {
                        Image(systemName: "checkmark")
                            .resizable()
                            .scaledToFit()
                            .frame(width: EmmSpacing.s3, height: EmmSpacing.s3)
                            .foregroundStyle(EmmColors.textPrimary)
                            .frame(width: EmmSpacing.s6, height: EmmSpacing.s6)
                            .background(EmmColors.surface3, in: Circle())
                    }
                }
                .padding(.horizontal, EmmSpacing.s6)
                .padding(.vertical, EmmSpacing.s3)
                .background(item.isActive ? EmmColors.surface1 : Color.clear)
            }
            .accessibilityAddTraits(item.isActive ? .isSelected : [])
        }
    }

    struct AmountSheet: View {
        let state: SeeTransactionsUiState
        let send: Send
        @State private var digits: String

        init(state: SeeTransactionsUiState, send: @escaping Send) {
            self.state = state
            self.send = send
            let currentCents: KotlinLong? =
                state.amountSheetTarget == AmountRangeTarget.min ? state.minAmountCents : state.maxAmountCents
            let seed: String = currentCents.map { CentsFormatterKt.moneyCentsString(money: $0.int64Value) } ?? ""
            _digits = State(initialValue: CentsFormatterKt.sanitizeCentsInput(raw: seed))
        }

        var body: some View {
            VStack(spacing: EmmSpacing.s4) {
                SheetTitleBar(title: title, onClose: dismiss)
                HStack(alignment: .firstTextBaseline, spacing: EmmSpacing.s2) {
                    Text("S/")
                        .emmTextStyle(EmmType.amountLead)
                        .foregroundStyle(EmmColors.textPrimary)
                    Text(digits.isEmpty ? "0.00" : formattedDigits)
                        .emmTextStyle(EmmType.amountL)
                        .foregroundStyle(EmmColors.textPrimary)
                }
                .accessibilityElement(children: .combine)
                Numpad(digits: $digits)
                    .padding(.horizontal, EmmSpacing.s4)
                Button {
                    send(SeeTransactionsIntentAmountFilterIntentOnAmountConfirmed(digits: digits))
                    dismiss()
                } label: {
                    Text("Listo · S/ " + formattedDigits)
                        .emmTextStyle(EmmType.titleM)
                        .foregroundStyle(isConfirmEnabled ? EmmColors.bg : EmmColors.textTertiary)
                        .frame(maxWidth: .infinity, minHeight: EmmSpacing.s12 + EmmSpacing.s1)
                        .background(isConfirmEnabled ? EmmColors.textPrimary : EmmColors.surface1, in: EmmRadii.rL)
                }
                .disabled(!isConfirmEnabled)
                .padding(.horizontal, EmmSpacing.s4)
                .padding(.bottom, EmmSpacing.s4)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            .background(EmmColors.bg)
            .presentationBackground(EmmColors.bg)
            .presentationDetents([.large])
        }

        private var title: String {
            state.amountSheetTarget == AmountRangeTarget.min ? "Monto mínimo" : "Monto máximo"
        }

        private var formattedDigits: String {
            CentsFormatterKt.formatCentsForDisplay(digits: digits)
        }

        private var isConfirmEnabled: Bool {
            CentsFormatterKt.isSavableAmount(digits)
        }

        private func dismiss() {
            send(SeeTransactionsIntentAmountFilterIntentOnAmountSheetDismissed.shared)
        }
    }
}
