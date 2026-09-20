package com.emm.justchill.feature.transaction.capture.components

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import com.emm.justchill.core.domain.shared.Money

internal const val SAVE_MOTION_MILLIS: Int = 400

@Stable
internal class SaveMotion(private val animatorDurationScale: Float) {

    private val progress: Animatable<Float, AnimationVector1D> = Animatable(0f)
    private var heldTotal: String? by mutableStateOf(null)
    private var lineCenterY: Float by mutableFloatStateOf(0f)
    private var heroCenterY: Float by mutableFloatStateOf(0f)

    var flyingAmount: Money? by mutableStateOf(null)
        private set

    fun holdTotal(current: String) {
        heldTotal = heldTotal ?: current
    }

    fun releaseTotal() {
        heldTotal = null
    }

    fun displayedTotal(current: String): String = heldTotal ?: current

    suspend fun fly(amount: Money) {
        if (animatorDurationScale > 0f) {
            flyingAmount = amount
            progress.snapTo(0f)
            try {
                progress.animateTo(1f, tween(durationMillis = SAVE_MOTION_MILLIS, easing = FastOutSlowInEasing))
            } finally {
                flyingAmount = null
            }
        }
        heldTotal = null
    }

    fun Modifier.monthLineTarget(): Modifier = onGloballyPositioned { coordinates ->
        lineCenterY = coordinates.boundsInRoot().center.y
    }

    fun Modifier.heroOrigin(): Modifier = onGloballyPositioned { coordinates ->
        heroCenterY = coordinates.boundsInRoot().center.y
    }

    fun Modifier.inFlight(): Modifier = graphicsLayer {
        translationY = (lineCenterY - heroCenterY) * progress.value
        alpha = 1f - progress.value
    }
}

@Composable
internal fun rememberSaveMotion(): SaveMotion {
    val context: Context = LocalContext.current
    return remember(context) {
        SaveMotion(
            animatorDurationScale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ),
        )
    }
}
