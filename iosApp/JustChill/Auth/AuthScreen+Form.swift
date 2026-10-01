@preconcurrency import JustChillKit
import SwiftUI

extension AuthScreen {
    struct FormStep: View {
        let form: AuthUiStateForm
        let send: Send

        var body: some View {
            VStack(spacing: EmmSpacing.s0) {
                ScrollView {
                    VStack(alignment: .leading, spacing: EmmSpacing.s0) {
                        Text(form.heading)
                            .emmTextStyle(EmmType.headlineL)
                            .foregroundStyle(EmmColors.textPrimary)
                            .accessibilityAddTraits(.isHeader)
                            .padding(.top, EmmSpacing.s6)
                        Text(
                            "Tu data vive en tu celular. Con una cuenta, que es opcional, guardas una copia de respaldo "
                                + "y tú decides cuándo restaurarla."
                        )
                        .emmTextStyle(EmmType.bodyM)
                        .foregroundStyle(EmmColors.textSecondary)
                        .fixedSize(horizontal: false, vertical: true)
                        .padding(.top, EmmSpacing.s2)
                        VStack(alignment: .leading, spacing: EmmSpacing.s5) {
                            EmailField(email: emailBinding, error: form.emailError)
                            PasswordField(password: passwordBinding, error: form.passwordError)
                        }
                        .padding(.top, EmmSpacing.s6)
                        toggle
                            .padding(.top, EmmSpacing.s8)
                    }
                    .padding(.horizontal, EmmSpacing.s4)
                    .padding(.bottom, EmmSpacing.s4)
                }
                SaveButton(
                    label: form.submitLabel,
                    isEnabled: form.isIdle,
                    isSaving: form.isSubmittingEmail,
                    onSave: { send(AuthIntentSubmit.shared) }
                )
            }
        }

        private var toggle: some View {
            Button(action: { send(AuthIntentToggleMode.shared) }) {
                Text(form.toggleLabel)
                    .emmTextStyle(EmmType.bodyM)
                    .foregroundStyle(EmmColors.textPrimary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, EmmSpacing.s4)
                    .frame(minHeight: EmmSpacing.s12)
                    .contentShape(Rectangle())
            }
            .frame(maxWidth: .infinity)
        }

        private var emailBinding: Binding<String> {
            Binding(get: { form.email }, set: { send(AuthIntentEmailChanged(value: $0)) })
        }

        private var passwordBinding: Binding<String> {
            Binding(get: { form.password }, set: { send(AuthIntentPasswordChanged(value: $0)) })
        }
    }

    struct EmailField: View {
        @Binding var email: String
        let error: String?
        @FocusState private var isFocused: Bool

        var body: some View {
            FieldSection(eyebrow: "Correo", isFocused: isFocused, error: error) {
                TextField(
                    "",
                    text: $email,
                    prompt: Text("hola@ejemplo.com").foregroundStyle(EmmColors.textTertiary)
                )
                .emmTextStyle(EmmType.bodyL)
                .foregroundStyle(EmmColors.textPrimary)
                .keyboardType(.emailAddress)
                .textContentType(.username)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .focused($isFocused)
                .accessibilityLabel("Correo")
            }
        }
    }

    struct PasswordField: View {
        @Binding var password: String
        let error: String?
        @State private var isVisible: Bool = false
        @FocusState private var isFocused: Bool

        var body: some View {
            FieldSection(eyebrow: "Contraseña", isFocused: isFocused, error: error) {
                HStack(spacing: EmmSpacing.s2) {
                    field
                        .emmTextStyle(EmmType.bodyL)
                        .foregroundStyle(EmmColors.textPrimary)
                        .textContentType(.password)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .focused($isFocused)
                        .accessibilityLabel("Contraseña")
                    Button(action: { isVisible.toggle() }) {
                        Image(systemName: isVisible ? "eye" : "eye.slash")
                            .resizable()
                            .scaledToFit()
                            .frame(width: EmmSpacing.s5, height: EmmSpacing.s5)
                            .foregroundStyle(EmmColors.textTertiary)
                            .frame(width: EmmSpacing.s12, height: EmmSpacing.s12)
                            .contentShape(Rectangle())
                    }
                    .accessibilityLabel(isVisible ? "Ocultar contraseña" : "Mostrar contraseña")
                }
            }
        }

        @ViewBuilder
        private var field: some View {
            let prompt = Text("Mínimo 8 caracteres").foregroundStyle(EmmColors.textTertiary)
            if isVisible {
                TextField("", text: $password, prompt: prompt)
            } else {
                SecureField("", text: $password, prompt: prompt)
            }
        }
    }

    struct FieldSection<Field: View>: View {
        let eyebrow: String
        let isFocused: Bool
        let error: String?
        @ViewBuilder let field: () -> Field

        var body: some View {
            VStack(alignment: .leading, spacing: EmmSpacing.s3) {
                Eyebrow(text: eyebrow)
                VStack(spacing: EmmSpacing.s2) {
                    field()
                        .frame(minHeight: EmmSpacing.s12)
                    Hairline(isFocused: isFocused, isError: error != nil)
                    FieldError(message: error)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
            }
        }
    }
}
