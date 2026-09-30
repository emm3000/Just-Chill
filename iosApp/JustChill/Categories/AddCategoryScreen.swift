@preconcurrency import JustChillKit
import SwiftUI

struct AddCategoryScreen: View {
    typealias Store = MviStore<AddCategoryUiState, any AddCategoryIntent, any AddCategoryEffect>
    typealias Send = (any AddCategoryIntent) -> Void

    let initialType: CategoryType
    let onClose: () -> Void
    let onSaved: (JustChillKit.Category) -> Void

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
            let newStore = Store(resolveAddCategoryHandle(initialType: initialType.name, initialName: ""))
            newStore.onEffect { effect in
                switch onEnum(of: effect) {
                case .categorySaved(let saved): onSaved(saved.created)
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

extension AddCategoryScreen {
    struct Content: View {
        let state: AddCategoryUiState
        let send: Send
        let onClose: () -> Void

        @FocusState private var isNameFocused: Bool

        var body: some View {
            VStack(spacing: EmmSpacing.s0) {
                SheetTitleBar(title: "Nueva categoría", onClose: onClose)
                ScrollView {
                    VStack(alignment: .leading, spacing: EmmSpacing.s5) {
                        PreviewChip(state: state)
                            .frame(maxWidth: .infinity)
                        section(eyebrow: "Nombre") { nameField }
                        section(eyebrow: "Tipo") {
                            TypeSegmented(selected: state.categoryType) {
                                send(AddCategoryIntentOnCategoryTypeChange(value: $0))
                            }
                        }
                        section(eyebrow: "Ícono") {
                            IconPicker(selectedId: state.iconId) { send(AddCategoryIntentOnIconChange(value: $0)) }
                        }
                        section(eyebrow: "Color") {
                            ColorRow(selectedId: state.colorId) { send(AddCategoryIntentOnColorChange(value: $0)) }
                        }
                    }
                    .padding(.horizontal, EmmSpacing.s4)
                    .padding(.vertical, EmmSpacing.s4)
                }
                .scrollDismissesKeyboard(.interactively)
                SaveButton(
                    label: state.saveLabel,
                    isEnabled: state.isAllFieldValidated,
                    isSaving: false,
                    onSave: save
                )
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            .background(EmmColors.bg)
            .onAppear {
                if state.isPreviewPlaceholder { isNameFocused = true }
            }
        }

        private var nameField: some View {
            VStack(spacing: EmmSpacing.s2) {
                TextField(
                    "",
                    text: nameBinding,
                    prompt: Text("ej. Comida").foregroundStyle(EmmColors.textTertiary)
                )
                .emmTextStyle(EmmType.titleL)
                .foregroundStyle(EmmColors.textPrimary)
                .tint(EmmColors.borderFocus)
                .focused($isNameFocused)
                .submitLabel(.done)
                .onSubmit(save)
                .frame(minHeight: EmmSpacing.s12)
                .accessibilityLabel("Nombre")
                Hairline(isFocused: isNameFocused)
            }
        }

        private var nameBinding: Binding<String> {
            Binding(
                get: { state.name },
                set: { send(AddCategoryIntentOnNameChange(value: $0)) }
            )
        }

        private func save() {
            guard state.isAllFieldValidated else { return }
            isNameFocused = false
            send(AddCategoryIntentOnSave.shared)
        }

        private func section<Body: View>(eyebrow: String, @ViewBuilder content: () -> Body) -> some View {
            VStack(alignment: .leading, spacing: EmmSpacing.s2) {
                Eyebrow(text: eyebrow)
                content()
            }
        }
    }
}
