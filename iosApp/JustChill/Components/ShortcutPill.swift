import SwiftUI

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
