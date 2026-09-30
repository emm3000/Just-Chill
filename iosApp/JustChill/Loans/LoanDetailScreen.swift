@preconcurrency import JustChillKit
import SwiftUI

struct LoanDetailScreen: View {
    typealias Store = MviStore<LoanDetailUiState, any LoanDetailIntent, any LoanDetailEffect>
    typealias Send = (any LoanDetailIntent) -> Void

    let loanId: String
    let onClose: () -> Void

    @State private var store: Store?
    @State private var errorMessage: String?
    @State private var isEditPresented: Bool = false

    var body: some View {
        Group {
            if let store {
                Content(state: store.state, send: { store.send($0) }, onClose: onClose)
            } else {
                EmmColors.bg
            }
        }
        .background(EmmColors.bg)
        .navigationDestination(isPresented: $isEditPresented) {
            AddEditLoanScreen(loanId: loanId, onClose: { isEditPresented = false })
                .toolbar(.hidden, for: .navigationBar)
                .toolbar(.hidden, for: .tabBar)
        }
        .task {
            guard store == nil else { return }
            let newStore = Store(resolveLoanDetailHandle(loanId: loanId))
            newStore.onEffect { effect in
                switch onEnum(of: effect) {
                case .navigateToEditLoan: isEditPresented = true
                case .loanDeleted: onClose()
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

extension LoanDetailScreen {
    struct Content: View {
        let state: LoanDetailUiState
        let send: Send
        let onClose: () -> Void

        var body: some View {
            VStack(spacing: EmmSpacing.s0) {
                TopBar(
                    title: state.summary?.personName ?? "",
                    onBack: onClose,
                    trailing: [
                        TopBar.Action(symbol: "pencil", label: "Editar préstamo") {
                            send(LoanDetailIntentOnEditLoanClick.shared)
                        },
                        TopBar.Action(symbol: "trash", label: "Eliminar préstamo", tone: .danger) {
                            send(LoanDetailIntentOnDeleteLoanClick.shared)
                        },
                    ]
                )
                if let summary = state.summary {
                    loaded(summary)
                } else {
                    ProgressView()
                        .tint(EmmColors.textTertiary)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            .background(EmmColors.bg)
            .sheet(isPresented: paymentBinding) { paymentSheet }
            .sheet(isPresented: deleteLoanBinding) { deleteLoanSheet }
            .sheet(isPresented: deletePaymentBinding) { deletePaymentSheet }
        }

        private func loaded(_ summary: LoanSummaryUi) -> some View {
            VStack(spacing: EmmSpacing.s0) {
                ScrollView {
                    VStack(alignment: .leading, spacing: EmmSpacing.s0) {
                        SummaryCard(summary: summary)
                            .padding(EmmSpacing.s4)
                        Eyebrow(text: "ABONOS")
                            .padding(.horizontal, EmmSpacing.s4)
                            .padding(.bottom, EmmSpacing.s2)
                        payments
                    }
                    .padding(.bottom, EmmSpacing.s3)
                }
                SaveButton(
                    label: "Registrar abono",
                    isEnabled: !summary.isSettled,
                    isSaving: false,
                    onSave: { send(LoanDetailIntentPaymentFormIntentOnAddPaymentClick.shared) }
                )
            }
        }

        @ViewBuilder
        private var payments: some View {
            if state.payments.isEmpty {
                EmptyPayments()
            } else {
                LazyVStack(spacing: EmmSpacing.s0) {
                    ForEach(state.payments, id: \.paymentId) { payment in
                        PaymentRow(
                            payment: payment,
                            onEdit: {
                                send(LoanDetailIntentPaymentFormIntentOnEditPaymentClick(paymentId: payment.paymentId))
                            },
                            onDelete: { send(LoanDetailIntentOnDeletePaymentClick(paymentId: payment.paymentId)) }
                        )
                    }
                }
            }
        }

        @ViewBuilder
        private var paymentSheet: some View {
            if let form = state.payment {
                PaymentFormSheet(form: form, send: send)
            }
        }

        @ViewBuilder
        private var deleteLoanSheet: some View {
            if let summary = state.summary {
                DeleteSheet(
                    title: "¿Borrar este préstamo?",
                    message: "Prestado el \(summary.readableLentAt) por \(summary.principal). "
                        + "Se borra junto con sus abonos registrados.",
                    isDeleting: state.isDeletingLoan,
                    onConfirm: { send(LoanDetailIntentOnDeleteLoanConfirm.shared) },
                    onDismiss: { send(LoanDetailIntentOnDeleteLoanDismiss.shared) }
                )
            }
        }

        @ViewBuilder
        private var deletePaymentSheet: some View {
            if let payment = state.pendingDeletePayment {
                DeleteSheet(
                    title: "¿Borrar este abono?",
                    message: "Abono de \(payment.amount) del \(payment.readablePaidAt). "
                        + "El préstamo recupera ese monto como pendiente.",
                    isDeleting: state.isDeletingPayment,
                    onConfirm: { send(LoanDetailIntentOnDeletePaymentConfirm.shared) },
                    onDismiss: { send(LoanDetailIntentOnDeletePaymentDismiss.shared) }
                )
            }
        }

        private var paymentBinding: Binding<Bool> {
            Binding(
                get: { state.payment != nil },
                set: { isPresented in
                    if !isPresented, state.payment != nil {
                        send(LoanDetailIntentPaymentFormIntentOnPaymentDismiss.shared)
                    }
                }
            )
        }

        private var deleteLoanBinding: Binding<Bool> {
            Binding(
                get: { state.pendingDeleteLoan },
                set: { isPresented in
                    if !isPresented, state.pendingDeleteLoan { send(LoanDetailIntentOnDeleteLoanDismiss.shared) }
                }
            )
        }

        private var deletePaymentBinding: Binding<Bool> {
            Binding(
                get: { state.pendingDeletePayment != nil },
                set: { isPresented in
                    if !isPresented, state.pendingDeletePayment != nil {
                        send(LoanDetailIntentOnDeletePaymentDismiss.shared)
                    }
                }
            )
        }
    }
}
