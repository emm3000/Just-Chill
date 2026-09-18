package com.emm.justchill.feature.profile

internal class SnapshotBackupActions(
    val onBackUpNow: () -> Unit,
    val onVerify: () -> Unit,
    val onAcknowledgeDestination: () -> Unit,
)
