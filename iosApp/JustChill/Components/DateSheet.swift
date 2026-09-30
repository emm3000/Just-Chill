@preconcurrency import JustChillKit
import SwiftUI

struct DateSheet: View {
    let today: Kotlinx_datetimeLocalDate
    let dateShortcuts: [DateShortcut]
    let onSelect: (Kotlinx_datetimeLocalDate) -> Void
    let onDismiss: () -> Void
    @State private var selection: Date

    private static let spanish: Locale = Locale(identifier: "es_ES")
    private static let mondayFirst: Calendar = {
        var calendar: Calendar = Calendar(identifier: .gregorian)
        calendar.locale = spanish
        calendar.firstWeekday = 2
        return calendar
    }()

    init(
        pickedDay: Kotlinx_datetimeLocalDate,
        today: Kotlinx_datetimeLocalDate,
        shortcuts: [DateShortcut],
        onSelect: @escaping (Kotlinx_datetimeLocalDate) -> Void,
        onDismiss: @escaping () -> Void
    ) {
        self.today = today
        self.dateShortcuts = shortcuts
        self.onSelect = onSelect
        self.onDismiss = onDismiss
        _selection = State(initialValue: pickedDay.calendarDate)
    }

    var body: some View {
        VStack(spacing: EmmSpacing.s0) {
            SheetTitleBar(title: "Selecciona fecha", onClose: onDismiss)
            ScrollView {
                VStack(spacing: EmmSpacing.s0) {
                    shortcuts
                    DatePicker(
                        "", selection: $selection, in: ...today.calendarDate, displayedComponents: .date
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
                ForEach(dateShortcuts, id: \.kind) { shortcut in
                    ShortcutPill(label: shortcut.label, isActive: shortcut.isActiveFor(day: selectedDay)) {
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

    private func confirm(_ day: Kotlinx_datetimeLocalDate) {
        onSelect(day)
        onDismiss()
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
