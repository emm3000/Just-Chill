package com.emm.justchill.core.ui.navigation

import androidx.navigation3.runtime.NavKey

/**
 * Every implementation must be `@Serializable`, fields included: the back stack re-resolves each
 * entry by class name on process-death restore, so a missing annotation crashes there and nowhere
 * else. A route left out of its feature's route registry is never round-tripped by
 * `RouteSerializationTest`, the only guard.
 */
interface AppRoute : NavKey

/**
 * The movement forms [AppNavigator.popToCapture] returns to, marked by the feature that owns them.
 * A form that offers "+ Nueva categoría" must carry the marker, or the picker's return leg walks
 * past it down to whichever marked form is buried below.
 */
interface CaptureRoute : AppRoute
