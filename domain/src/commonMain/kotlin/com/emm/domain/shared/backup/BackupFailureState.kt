package com.emm.domain.shared.backup

/**
 * The current failure streak for one account: how many cycles in a row have failed, and why the last
 * one did.
 *
 * **One value rather than two loose fields, because they are one fact.** A count without its reason
 * says "backups are broken" and cannot say how; a reason without its count says "something failed
 * once, maybe a month ago". Written separately they can also disagree: a count bumped without its
 * reason leaves the previous outage's label on a new one.
 *
 * **The type alone does not enforce that, and this class does not pretend to.** There is no
 * `init { require }` here and there must not be — the one place a mismatched pair could come from is
 * a value read back off disk, and crashing while *reporting* a failure is worse than reporting it
 * imprecisely. What actually removes the torn pair is the persistence layer: `AppPreferences` stores
 * both halves under a **single** key in one write, so a process killed mid-save loses the whole
 * update or none of it, never the count without its reason. An earlier version wrote two keys and
 * carried this KDoc claiming the state was unrepresentable; it was, on disk, and this sentence is
 * the reminder that the guarantee lives in the writer.
 *
 * [lastReason] can legitimately be null with a non-zero [consecutiveFailures]: see
 * [BackupFailureReason.fromNameOrNull], which answers null for a reason this build no longer has a
 * name for. **Read [consecutiveFailures] to decide whether backup is broken, never [lastReason].**
 *
 * [consecutiveFailures] is a streak, not a total: [BackupMetadataStore.clearFailures] resets it on
 * every verified success, so it answers "is backup broken *now*", which is the only question a
 * health indicator is asked.
 */
data class BackupFailureState(val consecutiveFailures: Int, val lastReason: BackupFailureReason?) {
    companion object {
        /** No failure on record: either nothing has failed, or the last cycle succeeded. */
        val None = BackupFailureState(consecutiveFailures = 0, lastReason = null)
    }
}
