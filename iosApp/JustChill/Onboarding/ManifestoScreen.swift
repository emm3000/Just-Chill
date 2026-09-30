import SwiftUI

struct ManifestoScreen: View {
    let isRevisit: Bool
    let onStart: () -> Void

    var body: some View {
        VStack(spacing: EmmSpacing.s0) {
            ScrollView {
                VStack(alignment: .leading, spacing: EmmSpacing.s8) {
                    Text("Tu plata no necesita\nun dashboard.")
                        .emmTextStyle(EmmType.headlineL)
                        .foregroundStyle(EmmColors.textPrimary)
                    Text("Necesita tu atención,\n30 segundos al día.")
                        .emmTextStyle(EmmType.headlineM)
                        .foregroundStyle(EmmColors.textPrimary)
                    Text("Sin cuenta obligatoria.\nSin notificaciones.\nSin que te vendamos nada.")
                        .emmTextStyle(EmmType.bodyL)
                        .foregroundStyle(EmmColors.textPrimary)
                    Text("Solo tú, tu plata,\ny la verdad.")
                        .emmTextStyle(EmmType.bodyL)
                        .foregroundStyle(EmmColors.textTertiary)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, EmmSpacing.s6)
                .padding(.top, EmmSpacing.s10)
                .padding(.bottom, EmmSpacing.s4)
            }
            startButton
                .padding(.horizontal, EmmSpacing.s6)
                .padding(.top, EmmSpacing.s4)
                .padding(.bottom, EmmSpacing.s6)
        }
        .background(EmmColors.bg)
    }

    @ViewBuilder
    private var startButton: some View {
        if isRevisit {
            OutlinedButton(title: "Volver", leadingSymbol: "arrow.backward", action: onStart)
        } else {
            OutlinedButton(title: "Empezar", trailingSymbol: "arrow.forward", action: onStart)
        }
    }
}

#Preview("FirstLaunch") {
    ManifestoScreen(isRevisit: false, onStart: {})
}

#Preview("Revisit") {
    ManifestoScreen(isRevisit: true, onStart: {})
}
