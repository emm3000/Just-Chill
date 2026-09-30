@preconcurrency import JustChillKit
import SwiftUI

struct AuthScreen: View {
    typealias Store = MviStore<AuthUiState, any AuthIntent, any AuthEffect>
    typealias Send = (any AuthIntent) -> Void

    let onBack: () -> Void

    @State private var store: Store?
    @State private var notice: String?
    @Environment(\.openURL) private var openURL: OpenURLAction

    var body: some View {
        Group {
            if let store {
                Content(state: store.state, send: { store.send($0) })
            } else {
                EmmColors.bg
            }
        }
        .background(EmmColors.bg)
        .task {
            guard store == nil else { return }
            let newStore = Store(resolveAuthHandle())
            newStore.onEffect(handle)
            store = newStore
        }
        .alert(notice ?? "", isPresented: isNoticePresented) {
            Button("Aceptar") { notice = nil }
        }
    }

    private func handle(_ effect: any AuthEffect) {
        switch onEnum(of: effect) {
        case .navigateBack: onBack()
        case .openEmailApp: openEmailApp()
        case .showError(let failure): notice = failure.error.toUserMessage()
        case .notify(let notify): notice = notify.message.text
        }
    }

    private func openEmailApp() {
        guard let mail = URL(string: "message:") else { return }
        openURL(mail)
    }

    private var isNoticePresented: Binding<Bool> {
        Binding(
            get: { notice != nil },
            set: { isPresented in
                if !isPresented { notice = nil }
            }
        )
    }
}

extension AuthScreen {
    struct Content: View {
        let state: AuthUiState
        let send: Send

        var body: some View {
            VStack(spacing: EmmSpacing.s0) {
                TopBar(title: state.title, onBack: { send(AuthIntentBack.shared) })
                switch onEnum(of: state) {
                case .form(let form): FormStep(form: form, send: send)
                case .checkEmail(let check): CheckEmailStep(check: check, send: send)
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            .background(EmmColors.bg)
        }
    }

    struct TopBar: View {
        let title: String
        let onBack: () -> Void

        var body: some View {
            VStack(spacing: EmmSpacing.s0) {
                HStack(spacing: EmmSpacing.s2) {
                    Button(action: onBack) {
                        Image(systemName: "chevron.left")
                            .resizable()
                            .scaledToFit()
                            .frame(width: EmmSpacing.s5, height: EmmSpacing.s5)
                            .foregroundStyle(EmmColors.textPrimary)
                            .frame(width: EmmSpacing.s12, height: EmmSpacing.s12)
                            .contentShape(Rectangle())
                    }
                    .accessibilityLabel("Volver")
                    Text(title)
                        .emmTextStyle(EmmType.titleL)
                        .foregroundStyle(EmmColors.textPrimary)
                        .lineLimit(1)
                        .minimumScaleFactor(0.5)
                        .accessibilityAddTraits(.isHeader)
                        .frame(maxWidth: .infinity, minHeight: EmmSpacing.s16, alignment: .leading)
                }
                .padding(.horizontal, EmmSpacing.s2)
                Hairline()
            }
        }
    }
}
