@preconcurrency import JustChillKit
import SwiftUI

struct AccountsScreen: View {
    typealias Store = MviStore<AccountsUiState, any AccountsIntent, any AccountsEffect>
    typealias Send = (any AccountsIntent) -> Void

    enum Destination: Hashable {
        case loans(LoansRoute)
    }

    @State private var store: Store?
    @State private var errorMessage: String?
    @State private var path: [Destination] = []

    var body: some View {
        NavigationStack(path: $path) {
            root
                .toolbar(.hidden, for: .navigationBar)
                .navigationDestination(for: Destination.self, destination: destination)
        }
    }

    private var root: some View {
        Group {
            if let store {
                Content(state: store.state, send: { store.send($0) }, onOpenLoans: { path.append(.loans(.people)) })
            } else {
                EmmColors.bg
            }
        }
        .background(EmmColors.bg)
        .task {
            guard store == nil else { return }
            let newStore = Store(resolveAccountsHandle())
            newStore.onEffect { effect in
                switch onEnum(of: effect) {
                case .showMessage(let message): errorMessage = message.text
                }
            }
            store = newStore
        }
        .alert(errorMessage ?? "", isPresented: isErrorPresented) {
            Button("Aceptar") { errorMessage = nil }
        }
    }

    @ViewBuilder
    private func destination(_ destination: Destination) -> some View {
        switch destination {
        case .loans(let route):
            LoansFlow(
                route: route,
                onBack: { path.removeLast() },
                onOpenPerson: { path.append(.loans(.personLoans($0))) }
            )
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

extension AccountsScreen {
    struct Content: View {
        let state: AccountsUiState
        let send: Send
        let onOpenLoans: () -> Void

        @State private var isAddPresented: Bool = false

        var body: some View {
            VStack(spacing: EmmSpacing.s0) {
                Header(state: state, onAdd: { isAddPresented = true })
                ScrollView {
                    LazyVStack(spacing: EmmSpacing.s0) {
                        if state.accounts.isEmpty {
                            EmptyState(onCreate: { isAddPresented = true })
                        } else {
                            ForEach(state.accounts, id: \.id) { row in
                                Row(row: row, send: send)
                            }
                        }
                        LoansRow(state: state, onOpen: onOpenLoans)
                    }
                    .padding(.bottom, EmmSpacing.s4)
                }
                .alert("Editar cuenta", isPresented: isEditPresented) {
                    TextField("ejm. Gasto diario", text: editName)
                    Button("Cancelar", role: .cancel) { send(AccountsIntentOnEditDismiss.shared) }
                    Button("Guardar") { send(AccountsIntentOnEditConfirm.shared) }
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            .background(EmmColors.bg)
            .sheet(isPresented: $isAddPresented) {
                AddAccountScreen(onClose: { isAddPresented = false })
            }
            .alert("¿Borrar «\(state.pendingDelete?.name ?? "")»?", isPresented: isDeletePresented) {
                Button("Cancelar", role: .cancel) { send(AccountsIntentOnDeleteDismiss.shared) }
                Button("Borrar", role: .destructive) { send(AccountsIntentOnDeleteConfirm.shared) }
            } message: {
                Text("Si tiene movimientos asociados, no se puede borrar.")
            }
        }

        private var isEditPresented: Binding<Bool> {
            Binding(get: { state.pendingEdit != nil }, set: { _ in })
        }

        private var isDeletePresented: Binding<Bool> {
            Binding(get: { state.pendingDelete != nil }, set: { _ in })
        }

        private var editName: Binding<String> {
            Binding(
                get: { state.editName },
                set: { send(AccountsIntentOnEditNameChange(value: $0)) }
            )
        }
    }
}
