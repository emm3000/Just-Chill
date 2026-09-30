@preconcurrency import JustChillKit
import SwiftUI

struct PersonLoansScreen: View {
    typealias Store = MviStore<PersonLoansUiState, any PersonLoansIntent, any PersonLoansEffect>

    let personKey: String
    let onBack: () -> Void

    @State private var store: Store?
    @State private var errorMessage: String?
    @State private var openedLoanId: String?

    var body: some View {
        Group {
            if let store {
                Content(state: store.state, onBack: onBack, send: { store.send($0) })
            } else {
                EmmColors.bg
            }
        }
        .background(EmmColors.bg)
        .navigationDestination(item: $openedLoanId) { loanId in
            LoanDetailScreen(loanId: loanId, onClose: { openedLoanId = nil })
                .toolbar(.hidden, for: .navigationBar)
                .toolbar(.hidden, for: .tabBar)
        }
        .task {
            guard store == nil else { return }
            let newStore = Store(resolvePersonLoansHandle(personKey: personKey))
            newStore.onEffect { effect in
                switch onEnum(of: effect) {
                case .navigateToLoanDetail(let detail): openedLoanId = detail.loanId
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

extension PersonLoansScreen {
    struct Content: View {
        let state: PersonLoansUiState
        let onBack: () -> Void
        let send: (any PersonLoansIntent) -> Void

        var body: some View {
            VStack(spacing: EmmSpacing.s0) {
                TopBar(title: state.personName, onBack: onBack)
                if state.loans.isEmpty {
                    EmptyState(personName: state.personName)
                } else {
                    ScrollView {
                        LazyVStack(spacing: EmmSpacing.s0) {
                            ForEach(state.loans, id: \.loanId) { loan in
                                LoanRow(loan: loan) { send(PersonLoansIntentOnLoanClick(loanId: loan.loanId)) }
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
