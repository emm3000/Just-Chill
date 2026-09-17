package com.emm.justchill.core.platform

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference

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

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
