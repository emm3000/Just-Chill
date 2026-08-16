package com.emm.domain.shared.backup

/**
 * The current failure streak for one account: how many cycles in a row have failed, and why the last
 * one did.
 *
 * **One value rather than two loose fields, because they are one fact.** A count without its reason
 * says "backups are broken" and cannot say how; a reason without its count says "something failed
 * once, maybe a month ago". Worse, written separately they can disagree — a count bumped without its
 * reason leaves the previous outage's label attached to a new one, and clearing one without the
 * other leaves either a streak of zero with a reason or a reason-less streak. Neither is
 * representable here.
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
