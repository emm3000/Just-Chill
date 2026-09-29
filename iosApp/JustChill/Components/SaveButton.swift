import SwiftUI

struct SaveButton: View {
    let label: String
    let isEnabled: Bool
    let isSaving: Bool
    let onSave: () -> Void

    var body: some View {
        VStack(spacing: EmmSpacing.s0) {
            Rectangle()
                .fill(EmmColors.border)
                .frame(height: EmmSpacing.hairline)
            Button(action: onSave) {
                HStack(spacing: EmmSpacing.s2) {
                    if isSaving {
                        ProgressView()
                            .tint(EmmColors.textTertiary)
                    }
                    Text(label)
                        .emmTextStyle(EmmType.titleM)
                }
                .foregroundStyle(isEnabled ? EmmColors.bg : EmmColors.textTertiary)
                .frame(maxWidth: .infinity, minHeight: EmmSpacing.s12 + EmmSpacing.s1)
                .background(isEnabled ? EmmColors.textPrimary : EmmColors.surface1, in: EmmRadii.rL)
            }
            .disabled(!isEnabled)
            .padding(.horizontal, EmmSpacing.s4)
            .padding(.top, EmmSpacing.s3)
            .padding(.bottom, EmmSpacing.s4)
        }
    }
}
