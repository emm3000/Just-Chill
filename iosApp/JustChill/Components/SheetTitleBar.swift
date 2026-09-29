import SwiftUI

struct SheetTitleBar: View {
    let title: String
    let onClose: () -> Void

    var body: some View {
        HStack {
            Text(title)
                .emmTextStyle(EmmType.titleM)
                .foregroundStyle(EmmColors.textPrimary)
                .accessibilityAddTraits(.isHeader)
            Spacer()
            Button(action: onClose) {
                Image(systemName: "xmark")
                    .resizable()
                    .scaledToFit()
                    .frame(width: EmmSpacing.s3, height: EmmSpacing.s3)
                    .foregroundStyle(EmmColors.textSecondary)
                    .frame(width: EmmSpacing.s8, height: EmmSpacing.s8)
                    .background(EmmColors.surface1, in: Circle())
                    .overlay { Circle().stroke(EmmColors.border, lineWidth: EmmSpacing.hairline) }
                    .frame(width: EmmSpacing.s12, height: EmmSpacing.s12)
            }
            .accessibilityLabel("Cerrar")
        }
        .padding(.leading, EmmSpacing.s6)
        .padding(.trailing, EmmSpacing.s4)
        .padding(.top, EmmSpacing.s4)
        .padding(.bottom, EmmSpacing.s2)
    }
}
