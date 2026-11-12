// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package top.yukonga.miuix.kmp.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

/**
 * Drag-to-deform press feedback for dialogs, bottom sheets and popup menus.
 *
 * The deformation maths and spring specs follow Kyant0/AndroidLiquidGlass's InteractiveHighlight
 * and its drag-scale layer: press grows the panel, the drag is tanh-damped into a translation, and
 * the panel stretches along the drag direction. Panels get no touch highlight; only buttons do.
 *
 * The gesture only watches the [PointerEventPass.Final] pass and never consumes, so taps, buttons,
 * text fields and scrolling keep working. It engages after the touch slop, and not at all for
 * movement a child has already consumed (scroll, sliders, sheet handle).
 */
@Stable
class GlassDragDeform(private val scope: CoroutineScope) {

    private val pressSpec = spring<Float>(0.5f, 300f, 0.001f)
    private val positionSpec = spring(0.5f, 300f, Offset.VisibilityThreshold)
    private val press = Animatable(0f, 0.001f)
    private val position = Animatable(Offset.Zero, Offset.VectorConverter, Offset.VisibilityThreshold)
    private var start = Offset.Zero

    /** Gesture observer plus the deforming layer. Apply before the surface modifiers. */
    val layer: Modifier = Modifier

    private fun begin(down: Offset) {
        start = down
        scope.launch {
            launch { press.animateTo(1f, pressSpec) }
            launch { position.snapTo(down) }
        }
    }

    private fun end() {
        scope.launch {
            launch { press.animateTo(0f, pressSpec) }
            launch { position.animateTo(start, positionSpec) }
        }
    }

    private companion object {
        const val GROW_DP = 8f
    }
}

@Composable
fun rememberGlassDragDeform(): GlassDragDeform {
    val scope = rememberCoroutineScope()
    return remember(scope) { GlassDragDeform(scope) }
}
