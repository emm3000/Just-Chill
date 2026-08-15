package com.emm.justchill.core.lifecycle

import kotlinx.coroutines.flow.Flow

/**
 * Emits Unit on every foreground resume; one declaration, platform actuals supply
 * ProcessLifecycleOwner (Android) / UIApplicationDidBecomeActive (iOS).
 */
expect fun resumeEvents(): Flow<Unit>
