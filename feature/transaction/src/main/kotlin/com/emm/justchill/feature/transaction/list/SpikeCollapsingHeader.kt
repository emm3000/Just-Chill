// SPIKE: throwaway, see branch spike/collapsing-month-summary
package com.emm.justchill.feature.transaction.list

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.emm.justchill.core.ui.atoms.SegmentOption
import com.emm.justchill.core.ui.atoms.Segmented
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import kotlin.math.roundToInt

internal enum class SpikeCollapseVariant(val label: String) {
    SummaryCollapses("A · Resumen"),
    SummaryFades("B · Fundido"),
    EverythingCollapses("C · Todo"),
}

@Stable
internal class SpikeCollapseState {
    var fullHeightPx: Float by mutableFloatStateOf(0f)
    var heightOffsetPx: Float by mutableFloatStateOf(0f)

    val fraction: Float
        get() = if (fullHeightPx > 0f) (-heightOffsetPx / fullHeightPx).coerceIn(0f, 1f) else 0f

    fun shiftBy(delta: Float): Float {
        val previous: Float = heightOffsetPx
        heightOffsetPx = (previous + delta).coerceIn(-fullHeightPx, 0f)
        return heightOffsetPx - previous
    }

    fun reset() {
        heightOffsetPx = 0f
    }

    suspend fun expand() {
        animateTo(0f)
    }

    suspend fun settle() {
        val current: Float = fraction
        if (fullHeightPx == 0f || current == 0f || current == 1f) return
        val target: Float = if (current < SETTLE_THRESHOLD) 0f else -fullHeightPx
        animateTo(target)
    }

    private suspend fun animateTo(target: Float) {
        animate(
            initialValue = heightOffsetPx,
            targetValue = target,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        ) { value: Float, _: Float -> heightOffsetPx = value }
    }
}

internal class SpikeCollapseConnection(
    private val state: SpikeCollapseState,
    private val consumesCollapse: Boolean,
    private val canCollapse: () -> Boolean,
) : NestedScrollConnection {

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (available.y >= 0f || !canCollapse()) return Offset.Zero
        val consumedY: Float = state.shiftBy(available.y)
        return if (consumesCollapse) Offset(0f, consumedY) else Offset.Zero
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (available.y <= 0f || state.fullHeightPx == 0f) return Offset.Zero
        return Offset(0f, state.shiftBy(available.y))
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        state.settle()
        return Velocity.Zero
    }
}

internal fun Modifier.spikeCollapsing(state: SpikeCollapseState): Modifier = this
    .clipToBounds()
    .layout { measurable, constraints ->
        val placeable: Placeable = measurable.measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
        val visibleHeight: Int = (placeable.height + state.heightOffsetPx).roundToInt().coerceIn(0, placeable.height)
        layout(placeable.width, visibleHeight) {
            placeable.placeRelative(0, (state.heightOffsetPx * PARALLAX).roundToInt())
        }
    }
    .spikeFading(state)

internal fun Modifier.spikeFading(state: SpikeCollapseState): Modifier = this
    .onSizeChanged { state.fullHeightPx = it.height.toFloat() }
    .graphicsLayer {
        val collapsed: Float = state.fraction
        alpha = (1f - collapsed * FADE_SPEED).coerceIn(0f, 1f)
        scaleX = lerp(1f, COLLAPSED_SCALE, collapsed)
        scaleY = lerp(1f, COLLAPSED_SCALE, collapsed)
        transformOrigin = TransformOrigin(0f, 0f)
    }

@Composable
internal fun SpikeVariantSwitcher(selected: SpikeCollapseVariant, onSelect: (SpikeCollapseVariant) -> Unit) {
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val options: List<SegmentOption<SpikeCollapseVariant>> = SpikeCollapseVariant.entries.map {
        SegmentOption(value = it, label = it.label)
    }
    Segmented(
        options = options,
        selected = selected,
        onSelect = onSelect,
        modifier = Modifier.padding(horizontal = spacing.s6),
    )
}

private const val SETTLE_THRESHOLD = 0.5f
private const val PARALLAX = 0.5f
private const val FADE_SPEED = 1.4f
private const val COLLAPSED_SCALE = 0.92f
