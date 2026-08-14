package com.emm.domain.shared.backup

/**
 * Puts one snapshot into remote storage and proves it arrived intact before saying it worked.
 *
 * ADR 009: a backup is a snapshot, not row replication — [BackupRepository.exportToJson] produces
 * the whole ledger as one versioned JSON document and this is where that document goes. Nothing
 * here schedules, retries or prunes; the trigger, the flag and the retention policy are ADR 009
 * Phase 2c's and this port has no opinion about any of them.
 *
 * **Returning normally means the stored bytes were read back and matched**, not that a request
 * succeeded. That is the whole reason this is a port of its own rather than a call into a storage
 * SDK at the call site: "the upload returned 200" is exactly the claim the 2026-08-12 outage was
 * made of, and an implementation that cannot re-read what it wrote cannot honour this contract.
 *
 * **Every failure throws a `DomainException` whose message names WHICH step failed** — hard
 * constraint 4, no silent failure anywhere in this pipeline. A caller may report the message; it
 * must never treat an absent exception as anything other than a verified snapshot.
 */
interface BackupUploader {

    /**
     * Stores [payload] under [fileName], reads it back, and returns only if the two match.
     *
     * [payload] is the export document as [BackupRepository.exportToJson] returned it — a `String`
     * and not a `ByteArray` because the encoding has to happen exactly once, on the far side of this
     * boundary, where the same array is both hashed and sent. Handing bytes across here would let a
     * caller hash one encoding and an implementation send another: same characters, different bytes,
     * a mismatch indistinguishable from a corrupt upload.
     *
     * [fileName] is a name **relative to a prefix the caller never sees, and it must end in
     * `.json`**. A relative path is allowed — ADR 009 Phase 2c pins snapshots under a `pinned/`
     * segment the retention prune never scans, and that segment arrives through this parameter. What
     * a caller cannot do is choose the ROOT: the storage layer owns the per-user prefix its access
     * rules are keyed on, so a snapshot cannot be aimed at somebody else's by construction.
     *
     * The `.json` requirement is not cosmetic and not the storage layer's private business either:
     * the bucket accepts one content type and the implementation derives it from the extension, so a
     * name that ends any other way is a server-side refusal. Naming is ADR 009 Phase 2c's decision;
     * the extension is not up for grabs.
     */
    suspend fun upload(fileName: String, payload: String)
}
