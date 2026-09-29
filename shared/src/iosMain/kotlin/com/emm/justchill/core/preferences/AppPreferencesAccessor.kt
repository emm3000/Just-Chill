package com.emm.justchill.core.preferences

import org.koin.mp.KoinPlatform

fun resolveAppPreferences(): AppPreferences = KoinPlatform.getKoin().get()
