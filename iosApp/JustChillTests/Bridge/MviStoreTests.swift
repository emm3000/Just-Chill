@preconcurrency import JustChillKit
import Testing

@testable import JustChill

@MainActor
struct MviStoreTests {
    @Test("releasing the last reference to a store clears its handle")
    func releasingTheStoreClearsItsHandle() {
        let handle = resolveAccountsHandle()
        var store: MviStore<AccountsUiState, any AccountsIntent, any AccountsEffect>? = MviStore(handle)
        weak var released = store
        #expect(deliversState(handle))

        store = nil

        #expect(released == nil)
        #expect(deliversState(handle) == false)
    }

    private func deliversState(_ handle: MviHandle<AccountsUiState, any AccountsIntent, any AccountsEffect>) -> Bool {
        var isDelivered = false
        handle.collectState { _ in isDelivered = true }
        return isDelivered
    }
}
