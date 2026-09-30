@preconcurrency import JustChillKit
import SwiftUI

extension MoreScreen {
    struct AccountSection: View {
        let state: ProfileUiState
        let send: Send

        var body: some View {
            VStack(alignment: .leading, spacing: EmmSpacing.s0) {
                SectionHeader(text: "Cuenta")
                Row(symbol: "person.crop.circle", label: state.accountLabel)
                Row(
                    symbol: "rectangle.portrait.and.arrow.right",
                    label: "Cerrar sesión",
                    meta: state.signOutMeta,
                    isEnabled: state.op == ProfileOp.none,
                    isBusy: state.op == ProfileOp.signingOut,
                    action: { send(ProfileIntentSignOut.shared) }
                )
                Row(
                    symbol: "trash",
                    label: "Eliminar cuenta",
                    meta: state.deleteAccountMeta,
                    isEnabled: state.op == ProfileOp.none,
                    isBusy: state.op == ProfileOp.deletingAccount,
                    action: { send(ProfileIntentDeleteAccountClicked.shared) }
                )
            }
            .alert("¿Eliminar tu cuenta?", isPresented: isDeleteDialogPresented) {
                Button("Cancelar", role: .cancel) { send(ProfileIntentDialogDismissed.shared) }
                Button("Eliminar cuenta", role: .destructive) { send(ProfileIntentDeleteAccountConfirmed.shared) }
            } message: {
                Text(
                    "Se borra tu cuenta y todos tus datos en la nube. "
                        + "Tu plata sigue acá, en este teléfono — eso no se toca."
                )
            }
        }

        private var isDeleteDialogPresented: Binding<Bool> {
            Binding(get: { state.dialog == .deleteAccount }, set: { _ in })
        }
    }
}
