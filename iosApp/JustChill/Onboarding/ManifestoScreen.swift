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

    private var startButton: some View {
        let title: String = isRevisit ? "Volver" : "Empezar"
        return Button(action: onStart) {
            HStack(spacing: EmmSpacing.s2) {
                if isRevisit {
                    arrow(systemName: "arrow.backward")
                }
                Text(title)
                    .emmTextStyle(EmmType.titleM)
                if !isRevisit {
                    arrow(systemName: "arrow.forward")
                }
            }
            .foregroundStyle(EmmColors.textPrimary)
            .frame(maxWidth: .infinity, minHeight: EmmSpacing.s12 + EmmSpacing.s1)
            .overlay(EmmRadii.rL.stroke(EmmColors.border, lineWidth: EmmSpacing.hairline))
            .contentShape(EmmRadii.rL)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(title)
        }
        .buttonStyle(.plain)
    }

    private func arrow(systemName: String) -> some View {
        Image(systemName: systemName)
            .accessibilityHidden(true)
    }
}

#Preview("FirstLaunch") {
    ManifestoScreen(isRevisit: false, onStart: {})
}

#Preview("Revisit") {
    ManifestoScreen(isRevisit: true, onStart: {})
}
