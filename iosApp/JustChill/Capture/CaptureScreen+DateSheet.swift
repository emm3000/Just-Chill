@preconcurrency import JustChillKit
import SwiftUI

extension CaptureScreen {
    struct DateSheet: View {
        let state: AddTransactionUiState
        let send: Send
        @State private var selection: Date

        private static let spanish: Locale = Locale(identifier: "es_ES")
        private static let mondayFirst: Calendar = {
            var calendar: Calendar = Calendar(identifier: .gregorian)
            calendar.locale = spanish
            calendar.firstWeekday = 2
            return calendar
        }()

        init(state: AddTransactionUiState, send: @escaping Send) {
            self.state = state
            self.send = send
            _selection = State(initialValue: state.pickerDate.calendarDate)
        }

        var body: some View {
            VStack(spacing: EmmSpacing.s0) {
                SheetTitleBar(title: "Selecciona fecha", onClose: dismiss)
                ScrollView {
                    VStack(spacing: EmmSpacing.s0) {
                        shortcuts
                        DatePicker(
                            "", selection: $selection, in: ...state.today.calendarDate, displayedComponents: .date
                        )
                        .datePickerStyle(.graphical)
                        .labelsHidden()
                        .tint(EmmColors.textSecondary)
                        .environment(\.locale, Self.spanish)
                        .environment(\.calendar, Self.mondayFirst)
                        .environment(\.colorScheme, .dark)
                        .padding(.horizontal, EmmSpacing.s4)
                    }
                }
                confirmButton
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            .background(EmmColors.bg)
            .presentationBackground(EmmColors.bg)
            .presentationDetents([.large])
        }

        private var selectedDay: Kotlinx_datetimeLocalDate {
            Kotlinx_datetimeLocalDate.day(containing: selection)
        }

        private var shortcuts: some View {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: EmmSpacing.s2) {
                    ForEach(state.dateShortcuts, id: \.self) { shortcut in
                        ShortcutPill(
                            label: label(of: shortcut.kind),
                            isActive: shortcut.kind == DateShortcutKind.today && selectedDay == state.today
                        ) {
                            confirm(shortcut.date)
                        }
                    }
                }
                .padding(.horizontal, EmmSpacing.s5)
            }
            .padding(.bottom, EmmSpacing.s2)
        }

        private var confirmButton: some View {
            Button {
                confirm(selectedDay)
            } label: {
                Text("Confirmar · " + SpanishDateFormat.shared.dayFullMonth(date: selectedDay))
                    .emmTextStyle(EmmType.titleM)
                    .foregroundStyle(EmmColors.bg)
                    .frame(maxWidth: .infinity, minHeight: EmmSpacing.s12 + EmmSpacing.s1)
                    .background(EmmColors.textPrimary, in: EmmRadii.rL)
            }
            .padding(.horizontal, EmmSpacing.s4)
            .padding(.top, EmmSpacing.s2)
            .padding(.bottom, EmmSpacing.s4)
        }

        private func label(of kind: DateShortcutKind) -> String {
            switch kind {
            case .today: "Hoy"
            case .yesterday: "Ayer"
            case .thisWeek: "Esta semana"
            case .thisMonth: "Este mes"
            }
        }

        private func confirm(_ day: Kotlinx_datetimeLocalDate) {
            send(AddTransactionIntentOnDateSelected(value: day))
            dismiss()
        }

        private func dismiss() {
            send(AddTransactionIntentOnSheetDismissed.shared)
        }
    }

    struct ShortcutPill: View {
        let label: String
        let isActive: Bool
        let action: () -> Void

        var body: some View {
            Button(action: action) {
                Text(label)
                    .emmTextStyle(EmmType.labelM)
                    .foregroundStyle(isActive ? EmmColors.bg : EmmColors.textSecondary)
                    .padding(.horizontal, EmmSpacing.s4)
                    .padding(.vertical, EmmSpacing.s2)
                    .background(isActive ? EmmColors.textPrimary : Color.clear, in: EmmRadii.rFull)
                    .overlay {
                        EmmRadii.rFull.stroke(
                            isActive ? EmmColors.textPrimary : EmmColors.border, lineWidth: EmmSpacing.hairline)
                    }
                    .frame(minHeight: EmmSpacing.s12)
                    .contentShape(Rectangle())
            }
            .accessibilityAddTraits(isActive ? .isSelected : [])
        }
    }
}

private extension Kotlinx_datetimeLocalDate {
    static let calendar: Calendar = Calendar(identifier: .gregorian)
    static let epoch: Date = calendar.date(from: DateComponents(year: 1970, month: 1, day: 1)) ?? Date()

    static func day(containing date: Date) -> Kotlinx_datetimeLocalDate {
        let start: Date = calendar.startOfDay(for: date)
        let days: Int = calendar.dateComponents([.day], from: epoch, to: start).day ?? 0
        return companion.fromEpochDays(epochDays: Int64(days))
    }

    var calendarDate: Date {
        Self.calendar.date(byAdding: .day, value: Int(toEpochDays()), to: Self.epoch) ?? Self.epoch
    }
}
