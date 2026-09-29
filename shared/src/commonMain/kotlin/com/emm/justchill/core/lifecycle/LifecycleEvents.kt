package com.emm.justchill.core.lifecycle

import kotlinx.coroutines.flow.Flow

// ADR 009: the backup orchestrator triggers on this edge because backgrounding is when a snapshot
// is cheap — nothing on screen still needs the CPU or the network at that moment.
expect fun backgroundEvents(): Flow<Unit>

expect fun resumeEvents(): Flow<Unit>
