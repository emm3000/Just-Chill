import SwiftUI

extension MoreScreen {
    struct PrivacyPolicy: View {
        let onBack: () -> Void

        var body: some View {
            VStack(spacing: EmmSpacing.s0) {
                ScrollView {
                    VStack(alignment: .leading, spacing: EmmSpacing.s6) {
                        Text("Tu privacidad")
                            .emmTextStyle(EmmType.headlineL)
                            .foregroundStyle(EmmColors.textPrimary)
                            .accessibilityAddTraits(.isHeader)
                        VStack(alignment: .leading, spacing: EmmSpacing.s4) {
                            ForEach(Self.paragraphs, id: \.self) { paragraph in
                                Text(paragraph)
                                    .emmTextStyle(EmmType.bodyL)
                                    .foregroundStyle(EmmColors.textPrimary)
                                    .fixedSize(horizontal: false, vertical: true)
                            }
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, EmmSpacing.s6)
                    .padding(.vertical, EmmSpacing.s6)
                }
                SaveButton(label: "Volver", isEnabled: true, isSaving: false, onSave: onBack)
            }
            .background(EmmColors.bg)
        }

        private static let paragraphs: [String] = [
            "Tu plata vive en tu celular. Sin cuenta, nada sale de él.",
            "Si creas una cuenta (opcional), guardamos tu correo para "
                + "identificarte. Tu data financiera —montos, categorías, movimientos— "
                + "no se sube a nuestros servidores: hoy tener cuenta no la respalda "
                + "ni la lleva entre tus celulares. No la vendemos ni la compartimos.",
            "Si exportas tu data a un archivo, tú decides qué hacer con él — "
                + "guardarlo, mandarlo o borrarlo.",
            "No usamos analytics ni cookies. La versión de Play Store reporta "
                + "solo crashes (Crashlytics), nunca tu data financiera.",
            "Si cambias de celular sin exportar primero, tu data se pierde — "
                + "pasa igual con cuenta o sin ella, porque tu data financiera no está "
                + "en nuestros servidores. Para recuperarla necesitas un archivo JSON "
                + "que hayas exportado tú.",
            "¿Quieres borrar tu cuenta y tu data del servidor? "
                + "Puedes hacerlo directo desde la app: Perfil → \"Eliminar cuenta\". "
                + "También puedes escribirnos a edgardo.emm20@gmail.com.",
        ]
    }
}
