@preconcurrency import JustChillKit
import SwiftUI

struct AddAccountScreen: View {
    typealias Store = MviStore<AddAccountUiState, any AddAccountIntent, any AddAccountEffect>
    typealias Send = (any AddAccountIntent) -> Void

    let onClose: () -> Void

    @State private var store: Store?
    @State private var errorMessage: String?

    var body: some View {
        Group {
            if let store {
                Content(state: store.state, send: { store.send($0) }, onClose: onClose)
            } else {
                EmmColors.bg
            }
        }
        .background(EmmColors.bg)
        .presentationBackground(EmmColors.bg)
        .presentationDetents([.large])
        .task {
            guard store == nil else { return }
            let newStore = Store(resolveAddAccountHandle())
            newStore.onEffect { effect in
                switch onEnum(of: effect) {
                case .accountSaved: onClose()
                case .showError(let failure): errorMessage = failure.message
                }
            }
            store = newStore
        }
        .alert(errorMessage ?? "", isPresented: isErrorPresented) {
            Button("Aceptar") { errorMessage = nil }
        }
    }

    private var isErrorPresented: Binding<Bool> {
        Binding(
            get: { errorMessage != nil },
            set: { isPresented in
                if !isPresented { errorMessage = nil }
            }
        )
    }
}

extension AddAccountScreen {
    struct Content: View {
        let state: AddAccountUiState
        let send: Send
        let onClose: () -> Void

        var body: some View {
            VStack(spacing: EmmSpacing.s0) {
                SheetTitleBar(title: "Nueva cuenta", onClose: onClose)
                ScrollView {
                    VStack(alignment: .leading, spacing: EmmSpacing.s5) {
                        section(eyebrow: "Nombre") { NameField(name: nameBinding) }
                        section(eyebrow: "Tipo") { typeGrid }
                    }
                    .padding(.horizontal, EmmSpacing.s4)
                    .padding(.vertical, EmmSpacing.s4)
                }
                SaveButton(
                    label: "Crear cuenta",
                    isEnabled: state.isEnabled,
                    isSaving: false,
                    onSave: { send(AddAccountIntentOnSave.shared) }
                )
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            .background(EmmColors.bg)
        }

        private var nameBinding: Binding<String> {
            Binding(
                get: { state.name },
                set: { send(AddAccountIntentOnNameChange(value: $0)) }
            )
        }

        private var typeGrid: some View {
            LazyVGrid(columns: columns, spacing: EmmSpacing.s2) {
                ForEach(AccountType.allCases, id: \.self) { type in
                    TypeCell(type: type, isSelected: type == state.selectedType) {
                        send(AddAccountIntentOnTypeChange(value: type))
                    }
                }
            }
        }

        private var columns: [GridItem] {
            Array(repeating: GridItem(.flexible(), spacing: EmmSpacing.s2), count: 2)
        }

        private func section<Body: View>(eyebrow: String, @ViewBuilder content: () -> Body) -> some View {
            VStack(alignment: .leading, spacing: EmmSpacing.s3) {
                AccountsScreen.Eyebrow(text: eyebrow)
                content()
            }
        }
    }

    struct NameField: View {
        @Binding var name: String
        @FocusState private var isFocused: Bool

        var body: some View {
            VStack(spacing: EmmSpacing.s2) {
                TextField(
                    "",
                    text: $name,
                    prompt: Text("ejm. Yape").foregroundStyle(EmmColors.textTertiary)
                )
                .emmTextStyle(EmmType.bodyL)
                .foregroundStyle(EmmColors.textPrimary)
                .focused($isFocused)
                .frame(minHeight: EmmSpacing.s12)
                .accessibilityLabel("Nombre")
                Rectangle()
                    .fill(isFocused ? EmmColors.borderFocus : EmmColors.border)
                    .frame(height: EmmSpacing.hairline)
                    .accessibilityHidden(true)
            }
        }
    }

    struct TypeCell: View {
        let type: AccountType
        let isSelected: Bool
        let onSelect: () -> Void

        var body: some View {
            Button(action: onSelect) {
                HStack(spacing: EmmSpacing.s3) {
                    Image(systemName: type.symbolName)
                        .resizable()
                        .scaledToFit()
                        .frame(width: EmmSpacing.s5, height: EmmSpacing.s5)
                        .foregroundStyle(EmmColors.textSecondary)
                        .accessibilityHidden(true)
                    Text(type.label)
                        .emmTextStyle(EmmType.titleM)
                        .multilineTextAlignment(.leading)
                        .foregroundStyle(isSelected ? EmmColors.textPrimary : EmmColors.textSecondary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
                .padding(.horizontal, EmmSpacing.s4)
                .frame(maxWidth: .infinity, minHeight: EmmSpacing.s12 + EmmSpacing.s1)
                .background(isSelected ? EmmColors.surface3 : EmmColors.surface1, in: EmmRadii.rM)
                .overlay {
                    EmmRadii.rM.stroke(
                        isSelected ? EmmColors.borderFocus : EmmColors.border, lineWidth: EmmSpacing.hairline)
                }
                .contentShape(Rectangle())
            }
            .accessibilityAddTraits(isSelected ? .isSelected : [])
        }
    }
}
