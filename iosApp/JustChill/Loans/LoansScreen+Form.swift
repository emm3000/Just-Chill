@preconcurrency import JustChillKit
import SwiftUI

extension LoansScreen {
    struct FormSection<Content: View>: View {
        let eyebrow: String
        @ViewBuilder let content: Content

        var body: some View {
            VStack(alignment: .leading, spacing: EmmSpacing.s2) {
                Eyebrow(text: eyebrow)
                content
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    struct AmountCard: View {
        let amountDigits: String
        let onOpen: () -> Void

        var body: some View {
            Button(action: onOpen) {
                HStack(alignment: .firstTextBaseline, spacing: EmmSpacing.s2) {
                    Text("S/")
                        .emmTextStyle(EmmType.amountLead)
                        .foregroundStyle(EmmColors.textTertiary)
                    Text(CentsFormatterKt.formatCentsForDisplay(digits: amountDigits))
                        .emmTextStyle(EmmType.amountCard)
                        .foregroundStyle(amountDigits.isEmpty ? EmmColors.textTertiary : EmmColors.textPrimary)
                        .lineLimit(1)
                        .minimumScaleFactor(0.5)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, EmmSpacing.s5)
                .background(EmmColors.surface1, in: EmmRadii.rM)
                .overlay { EmmRadii.rM.stroke(EmmColors.border, lineWidth: EmmSpacing.hairline) }
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityHint("Cambiar monto")
        }
    }

    struct DateRow: View {
        let label: String
        let onOpen: () -> Void

        var body: some View {
            Button(action: onOpen) {
                HStack(spacing: EmmSpacing.s2) {
                    Image(systemName: "calendar")
                        .foregroundStyle(EmmColors.textTertiary)
                        .accessibilityHidden(true)
                    Text(label)
                        .emmTextStyle(EmmType.labelL)
                        .foregroundStyle(EmmColors.textPrimary)
                        .multilineTextAlignment(.leading)
                }
                .frame(maxWidth: .infinity, minHeight: EmmSpacing.s12, alignment: .leading)
                .padding(.horizontal, EmmSpacing.s4)
                .background(EmmColors.surface1, in: EmmRadii.rM)
                .overlay { EmmRadii.rM.stroke(EmmColors.border, lineWidth: EmmSpacing.hairline) }
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityHint("Cambiar fecha")
        }
    }

    struct TextEntry: View {
        let placeholder: String
        @Binding var text: String
        var keyboard: UIKeyboardType = .default
        var isAutocorrectionDisabled: Bool = false

        var body: some View {
            TextField(
                "", text: $text, prompt: Text(placeholder).foregroundStyle(EmmColors.textTertiary)
            )
            .emmTextStyle(EmmType.bodyM)
            .foregroundStyle(EmmColors.textPrimary)
            .tint(EmmColors.borderFocus)
            .keyboardType(keyboard)
            .autocorrectionDisabled(isAutocorrectionDisabled)
            .frame(minHeight: EmmSpacing.s12)
            .padding(.horizontal, EmmSpacing.s4)
            .background(EmmColors.surface1, in: EmmRadii.rM)
            .overlay { EmmRadii.rM.stroke(EmmColors.border, lineWidth: EmmSpacing.hairline) }
        }
    }

    struct SanitizedEntry: View {
        let placeholder: String
        let text: String
        let current: @MainActor () -> String
        let onChange: (String) -> Void
        var keyboard: UIKeyboardType = .default
        @State private var draft: String

        init(
            placeholder: String, text: String, current: @escaping @MainActor () -> String,
            keyboard: UIKeyboardType = .default, onChange: @escaping (String) -> Void
        ) {
            self.placeholder = placeholder
            self.text = text
            self.current = current
            self.keyboard = keyboard
            self.onChange = onChange
            _draft = State(initialValue: text)
        }

        var body: some View {
            TextEntry(placeholder: placeholder, text: $draft, keyboard: keyboard)
                .onChange(of: text) { _, latest in draft = latest }
                .onChange(of: draft) { _, typed in
                    guard typed != text else { return }
                    onChange(typed)
                    Task { draft = current() }
                }
        }
    }

    struct AmountSheet: View {
        let title: String
        let subtitle: String?
        let onConfirm: (String) -> Void
        let onDismiss: () -> Void
        @State private var draftDigits: String

        init(
            title: String, subtitle: String?, amountDigits: String, onConfirm: @escaping (String) -> Void,
            onDismiss: @escaping () -> Void
        ) {
            self.title = title
            self.subtitle = subtitle
            self.onConfirm = onConfirm
            self.onDismiss = onDismiss
            _draftDigits = State(initialValue: amountDigits)
        }

        var body: some View {
            VStack(spacing: EmmSpacing.s0) {
                SheetTitleBar(title: title, onClose: onDismiss)
                if let subtitle {
                    Text(subtitle)
                        .emmTextStyle(EmmType.caption)
                        .foregroundStyle(EmmColors.textTertiary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal, EmmSpacing.s6)
                }
                hero
                    .frame(maxHeight: .infinity)
                Numpad(digits: $draftDigits)
                    .padding(.horizontal, EmmSpacing.s4)
                    .padding(.bottom, EmmSpacing.s2)
                SaveButton(
                    label: "Listo · " + draftAmount,
                    isEnabled: true,
                    isSaving: false,
                    onSave: {
                        onConfirm(draftDigits)
                        onDismiss()
                    }
                )
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            .background(EmmColors.bg)
            .presentationBackground(EmmColors.bg)
            .presentationDetents([.large])
        }

        private var draftAmount: String {
            CurrencyFormatKt.formatNeutral(value: CentsFormatterKt.formatCentsForDisplay(digits: draftDigits))
        }

        private var hero: some View {
            HStack(alignment: .firstTextBaseline, spacing: EmmSpacing.s2) {
                Text("S/")
                    .emmTextStyle(EmmType.amountLead)
                    .foregroundStyle(EmmColors.textTertiary)
                Text(CentsFormatterKt.formatCentsForDisplay(digits: draftDigits))
                    .emmTextStyle(EmmType.amountHero)
                    .foregroundStyle(draftDigits.isEmpty ? EmmColors.textTertiary : EmmColors.textPrimary)
                    .lineLimit(1)
                    .minimumScaleFactor(0.25)
            }
            .padding(.horizontal, EmmSpacing.s6)
            .frame(maxWidth: .infinity)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(draftAmount)
        }
    }
}
