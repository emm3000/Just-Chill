package com.emm.justchill.core

data class KitConfig(
    val supabaseUrl: String,
    val supabaseAnonKey: String,
    val googleServerClientId: String,
    val appVersion: String,
    val isSnapshotBackupEnabled: Boolean,
)
