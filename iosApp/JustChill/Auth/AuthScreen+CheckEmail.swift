@preconcurrency import JustChillKit
import SwiftUI

extension AuthScreen {
    struct CheckEmailStep: View {
        let check: AuthUiStateCheckEmail
        let send: Send

        var body: some View {
            ScrollView {
                VStack(spacing: EmmSpacing.s0) {
                    Image(systemName: "envelope")
                        .resizable()
                        .scaledToFit()
                        .frame(width: EmmSpacing.s6, height: EmmSpacing.s6)
                        .foregroundStyle(EmmColors.textSecondary)
                        .frame(width: EmmSpacing.s12, height: EmmSpacing.s12)
                        .background(EmmColors.surface2, in: EmmRadii.rM)
                        .accessibilityHidden(true)
                        .padding(.top, EmmSpacing.s12)
                    Text("Revisa tu correo")
                        .emmTextStyle(EmmType.headlineM)
                        .foregroundStyle(EmmColors.textPrimary)
                        .accessibilityAddTraits(.isHeader)
                        .padding(.top, EmmSpacing.s5)
                    sentTo
                        .padding(.top, EmmSpacing.s3)
                    VStack(spacing: EmmSpacing.s3) {
                        StepButton(label: "Abrir mi correo") {
                            send(AuthIntentOpenEmailApp.shared)
                        }
                        OutlinedButton(title: "Volver a iniciar sesión") {
                            send(AuthIntentBackToSignIn.shared)
                        }
                    }
                    .padding(.top, EmmSpacing.s6)
                    resend
                        .padding(.top, EmmSpacing.s5)
                }
                .frame(maxWidth: .infinity)
                .padding(.horizontal, EmmSpacing.s4)
                .padding(.bottom, EmmSpacing.s4)
            }
        }

        private var sentTo: some View {
            VStack(spacing: EmmSpacing.s0) {
                Text("Te mandamos un enlace a")
                    .emmTextStyle(EmmType.bodyM)
                    .foregroundStyle(EmmColors.textSecondary)
                Text(check.email)
                    .emmTextStyle(EmmType.amountS)
                    .foregroundStyle(EmmColors.textPrimary)
                Text("Ábrelo para confirmar tu cuenta y vuelve acá.")
                    .emmTextStyle(EmmType.bodyM)
                    .foregroundStyle(EmmColors.textSecondary)
            }
            .multilineTextAlignment(.center)
            .fixedSize(horizontal: false, vertical: true)
        }

        private var resend: some View {
            ViewThatFits {
                HStack(spacing: EmmSpacing.s1) { resendParts }
                VStack(spacing: EmmSpacing.s0) { resendParts }
            }
        }

        @ViewBuilder
        private var resendParts: some View {
            Text("¿No te llegó?")
                .emmTextStyle(EmmType.bodyM)
                .foregroundStyle(EmmColors.textTertiary)
            Button(action: { send(AuthIntentResendEmail.shared) }) {
                Text(check.resendLabel)
                    .emmTextStyle(EmmType.bodyM)
                    .foregroundStyle(check.isResendEnabled ? EmmColors.textPrimary : EmmColors.textTertiary)
                    .frame(minHeight: EmmSpacing.s12)
                    .contentShape(Rectangle())
            }
            .disabled(!check.isResendEnabled)
        }
    }

    struct StepButton: View {
        let label: String
        let action: () -> Void

        var body: some View {
            Button(action: action) {
                Text(label)
                    .emmTextStyle(EmmType.titleM)
                    .foregroundStyle(EmmColors.bg)
                    .frame(maxWidth: .infinity, minHeight: EmmSpacing.s12 + EmmSpacing.s1)
                    .background(EmmColors.textPrimary, in: EmmRadii.rL)
                    .contentShape(Rectangle())
            }
        }
    }
}
