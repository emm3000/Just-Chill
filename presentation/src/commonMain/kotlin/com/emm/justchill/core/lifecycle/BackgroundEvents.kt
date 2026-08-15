package com.emm.justchill.core.lifecycle

import kotlinx.coroutines.flow.Flow

/**
 * Emits Unit on every transition to background; one declaration, platform actuals supply
 * ProcessLifecycleOwner `ON_STOP` (Android) / `UIApplicationDidEnterBackgroundNotification` (iOS).
 *
 * Sibling of [resumeEvents], the opposite edge of the same foreground/background cycle. This is the
 * trigger ADR 009's backup orchestrator uses: the app going to background is when a snapshot is
 * cheap to take, since nothing on screen still needs the CPU or the network at that moment.
 */
expect fun backgroundEvents(): Flow<Unit>
