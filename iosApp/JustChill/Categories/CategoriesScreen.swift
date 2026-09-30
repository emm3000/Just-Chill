@preconcurrency import JustChillKit
import SwiftUI

struct CategoriesScreen: View {
    typealias Store = MviStore<CategoriesUiState, any CategoriesIntent, any CategoriesEffect>
    typealias Send = (any CategoriesIntent) -> Void

    let onBack: () -> Void

    @State private var store: Store?
    @State private var message: String?

    var body: some View {
        Group {
            if let store {
                Content(state: store.state, send: { store.send($0) }, onBack: onBack)
            } else {
                EmmColors.bg
            }
        }
        .background(EmmColors.bg)
        .task {
            guard store == nil else { return }
            let newStore = Store(resolveCategoriesHandle())
            newStore.onEffect { effect in
                switch onEnum(of: effect) {
                case .showMessage(let shown): message = shown.text
                }
            }
            store = newStore
        }
        .alert(message ?? "", isPresented: isMessagePresented) {
            Button("Aceptar") { message = nil }
        }
    }

    private var isMessagePresented: Binding<Bool> {
        Binding(
            get: { message != nil },
            set: { isPresented in
                if !isPresented { message = nil }
            }
        )
    }
}

extension CategoriesScreen {
    struct Content: View {
        let state: CategoriesUiState
        let send: Send
        let onBack: () -> Void

        @State private var isAddPresented: Bool = false

        var body: some View {
            VStack(spacing: EmmSpacing.s0) {
                TopBar(
                    title: "Categorías",
                    onBack: onBack,
                    add: TopBar.Action(label: "Nueva categoría") { isAddPresented = true }
                )
                if state.categories.isEmpty {
                    EmptyState(onCreate: { isAddPresented = true })
                } else {
                    list
                        .alert("Editar categoría", isPresented: isEditPresented) {
                            TextField("ejm. Supermercado", text: editName)
                            Button("Cancelar", role: .cancel) { send(CategoriesIntentOnEditDismiss.shared) }
                            Button("Borrar categoría", role: .destructive, action: deleteEdited)
                            Button("Guardar") { send(CategoriesIntentOnEditConfirm.shared) }
                        }
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            .background(EmmColors.bg)
            .sheet(isPresented: $isAddPresented) {
                AddCategoryScreen(
                    initialType: CategoryType.spend,
                    onClose: { isAddPresented = false },
                    onSaved: { _ in isAddPresented = false }
                )
            }
            .alert("¿Borrar «" + (state.pendingDelete?.name ?? "") + "»?", isPresented: isDeletePresented) {
                Button("Cancelar", role: .cancel) { send(CategoriesIntentOnDeleteDismiss.shared) }
                Button("Borrar", role: .destructive) { send(CategoriesIntentOnDeleteConfirm.shared) }
            } message: {
                Text(state.pendingDeleteMessage ?? "")
            }
        }

        private var list: some View {
            ScrollView {
                LazyVStack(spacing: EmmSpacing.s0) {
                    if !state.incomeRows.isEmpty {
                        SectionHeader(label: "Ingresos", count: state.incomeRows.count)
                        rows(state.incomeRows)
                    }
                    SectionHeader(label: "Gastos", count: Int(state.spendSectionCount))
                    rows(state.spendRows)
                    UncategorizedRow(countLabel: state.uncategorizedCountLabel)
                }
                .padding(.bottom, EmmSpacing.s8)
            }
        }

        private func rows(_ rows: [CategoryRowUi]) -> some View {
            ForEach(rows, id: \.id) { row in
                CategoryRow(row: row) { send(CategoriesIntentOnEditClick(category: row.category)) }
            }
        }

        private func deleteEdited() {
            guard let editing = state.pendingEdit else { return }
            send(CategoriesIntentOnEditDismiss.shared)
            send(CategoriesIntentOnDeleteClick(category: editing))
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
                set: { send(CategoriesIntentOnEditNameChange(value: $0)) }
            )
        }
    }
}
