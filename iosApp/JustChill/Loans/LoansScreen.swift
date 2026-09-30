@preconcurrency import JustChillKit
import SwiftUI

struct LoansScreen: View {
    typealias Store = MviStore<LoansUiState, any LoansIntent, any LoansEffect>

    let onBack: () -> Void
    let onOpenPerson: (String) -> Void

    @State private var store: Store?
    @State private var errorMessage: String?
    @State private var isAddLoanPresented: Bool = false

    var body: some View {
        Group {
            if let store {
                Content(state: store.state, onBack: onBack, send: { store.send($0) })
            } else {
                EmmColors.bg
            }
        }
        .background(EmmColors.bg)
        .navigationDestination(isPresented: $isAddLoanPresented) {
            AddEditLoanScreen(loanId: nil, onClose: { isAddLoanPresented = false })
                .toolbar(.hidden, for: .navigationBar)
                .toolbar(.hidden, for: .tabBar)
        }
        .task {
            guard store == nil else { return }
            let newStore = Store(resolveLoansHandle())
            newStore.onEffect { effect in
                switch onEnum(of: effect) {
                case .navigateToPerson(let person): onOpenPerson(person.personKey)
                case .navigateToAddLoan: isAddLoanPresented = true
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

extension LoansScreen {
    struct Content: View {
        let state: LoansUiState
        let onBack: () -> Void
        let send: (any LoansIntent) -> Void

        var body: some View {
            VStack(spacing: EmmSpacing.s0) {
                TopBar(
                    title: "Préstamos",
                    onBack: onBack,
                    trailing: [
                        TopBar.Action(symbol: "plus", label: "Nuevo préstamo") {
                            send(LoansIntentOnAddLoanClick.shared)
                        }
                    ]
                )
                if state.people.isEmpty {
                    EmptyState()
                } else {
                    ScrollView {
                        LazyVStack(spacing: EmmSpacing.s0) {
                            ForEach(state.people, id: \.personKey) { person in
                                PersonRow(person: person) {
                                    send(LoansIntentOnPersonClick(personKey: person.personKey))
                                }
                            }
                        }
                        .padding(.vertical, EmmSpacing.s1)
                        .padding(.bottom, EmmSpacing.s3)
                    }
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            .background(EmmColors.bg)
        }
    }
}
