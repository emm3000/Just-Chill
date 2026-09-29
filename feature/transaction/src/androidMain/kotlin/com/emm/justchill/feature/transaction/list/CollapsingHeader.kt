package com.emm.justchill.feature.transaction.list

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.util.lerp
import kotlin.math.roundToInt

@Stable
internal class CollapsingHeaderState {
    private var fullHeightPx: Float by mutableFloatStateOf(0f)

    var heightOffsetPx: Float by mutableFloatStateOf(0f)
        private set

    val collapsedFraction: Float
        get() = if (fullHeightPx > 0f) (-heightOffsetPx / fullHeightPx).coerceIn(0f, 1f) else 0f

    fun onMeasured(heightPx: Int) {
        fullHeightPx = heightPx.toFloat()
        heightOffsetPx = heightOffsetPx.coerceIn(-fullHeightPx, 0f)
    }

    fun shiftBy(delta: Float): Float {
        val previous: Float = heightOffsetPx
        heightOffsetPx = (previous + delta).coerceIn(-fullHeightPx, 0f)
        return heightOffsetPx - previous
    }

    fun open() {
        heightOffsetPx = 0f
    }

    suspend fun settle() {
        val fraction: Float = collapsedFraction
        if (fraction == 0f || fraction == 1f) return
        val target: Float = if (fraction < SETTLE_THRESHOLD) 0f else -fullHeightPx
        animate(
            initialValue = heightOffsetPx,
            targetValue = target,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        ) { value: Float, _: Float -> heightOffsetPx = value }
    }
}

private class CollapsingHeaderConnection(
    private val state: CollapsingHeaderState,
    private val isHeaderShown: () -> Boolean,
    private val canListScroll: () -> Boolean,
) : NestedScrollConnection {

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (available.y >= 0f || !isHeaderShown() || !canListScroll()) return Offset.Zero
        return Offset(0f, state.shiftBy(available.y))
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (available.y <= 0f || !isHeaderShown()) return Offset.Zero
        return Offset(0f, state.shiftBy(available.y))
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        state.settle()
        return Velocity.Zero
    }
}

@Composable
internal fun rememberCollapsingHeaderConnection(
    state: CollapsingHeaderState,
    listState: LazyListState,
    isHeaderShown: Boolean,
): NestedScrollConnection {
    val isHeaderShownNow: State<Boolean> = rememberUpdatedState(isHeaderShown)
    return remember(state, listState) {
        CollapsingHeaderConnection(
            state = state,
            isHeaderShown = { isHeaderShownNow.value },
            canListScroll = { listState.canScrollForward || listState.canScrollBackward },
        )
    }
}

internal fun Modifier.collapsingHeader(state: CollapsingHeaderState, scrollableState: ScrollableState): Modifier = this
    .scrollable(state = scrollableState, orientation = Orientation.Vertical, reverseDirection = true)
    .clipToBounds()
    .layout { measurable, constraints ->
        val placeable: Placeable = measurable.measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
        val offsetPx: Float = state.heightOffsetPx
        val visibleHeight: Int = (placeable.height + offsetPx).roundToInt().coerceIn(0, placeable.height)
        layout(placeable.width, visibleHeight) {
            placeable.placeRelative(0, (offsetPx * PARALLAX).roundToInt())
        }
    }
    .onSizeChanged { state.onMeasured(it.height) }
    .graphicsLayer {
        val fraction: Float = state.collapsedFraction
        alpha = (1f - fraction * FADE_SPEED).coerceIn(0f, 1f)
        scaleX = lerp(1f, COLLAPSED_SCALE, fraction)
        scaleY = lerp(1f, COLLAPSED_SCALE, fraction)
        transformOrigin = TransformOrigin(0f, 0f)
    }

private const val SETTLE_THRESHOLD: Float = 0.5f
private const val PARALLAX: Float = 0.5f
private const val FADE_SPEED: Float = 1.4f
private const val COLLAPSED_SCALE: Float = 0.92f
