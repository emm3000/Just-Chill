package com.emm.justchill.core.platform

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference

/**
 * Tracks the currently foregrounded Activity via [Application.ActivityLifecycleCallbacks].
 *
 * Register an instance via [Application.registerActivityLifecycleCallbacks] in [Application.onCreate].
 * The same instance must be provided as a Koin singleton so it can be injected where needed.
 */
class CurrentActivityHolder : Application.ActivityLifecycleCallbacks {

    private var activityRef: WeakReference<Activity>? = null

    val current: Activity?
        get() = activityRef?.get()

    override fun onActivityResumed(activity: Activity) {
        activityRef = WeakReference(activity)
    }

    override fun onActivityPaused(activity: Activity) {
        // Only clear if this is the same activity we are holding — avoids race where a new
        // activity resumes before the old one fires onPaused.
        if (activityRef?.get() === activity) {
            activityRef = null
        }
    }

    // Unused lifecycle callbacks — required by the interface.
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
