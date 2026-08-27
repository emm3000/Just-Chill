package com.emm.justchill.hh.profile

internal class SnapshotBackupActions(
    val onBackUpNow: () -> Unit,
    val onVerify: () -> Unit,
    val onAcknowledgeDestination: () -> Unit,
)
