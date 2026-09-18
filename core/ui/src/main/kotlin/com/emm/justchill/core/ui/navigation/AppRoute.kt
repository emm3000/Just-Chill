package com.emm.justchill.core.ui.navigation

import androidx.navigation3.runtime.NavKey

/**
 * Every implementation must be `@Serializable`, fields included: the back stack re-resolves each
 * entry by class name on process-death restore, so a missing annotation crashes there and nowhere
 * else. A route left out of its feature's route registry is never round-tripped by
 * `RouteSerializationTest`, the only guard.
 */
interface AppRoute : NavKey

interface BottomBarRoute : AppRoute

/** The capture screens [AppNavigator.popToCapture] returns to, marked by the feature that owns them. */
interface CaptureRoute : AppRoute
