package com.pradhul.forge.habits.ui

import android.provider.Settings
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset

/**
 * Pixel-like motion for Forge Habits. Animator duration scale 0
 * ("Remove animations") disables every animation.
 */
object HabitMotion {
    const val FAST_MS = 180
    const val MEDIUM_MS = 240
    const val SLOW_MS = 280
    const val STAGGER_MS = 16
    const val PRESS_SCALE = 0.96f

    fun enabled(animatorDurationScale: Float): Boolean = animatorDurationScale > 0f

    fun millis(base: Int, enabled: Boolean): Int = if (enabled) base else 0

    fun staggerDelayMs(index: Int, enabled: Boolean): Int {
        if (!enabled) return 0
        return (index * STAGGER_MS).coerceAtMost(220)
    }

    fun springFloat(enabled: Boolean): AnimationSpec<Float> =
        if (enabled) spring(dampingRatio = 0.78f, stiffness = 800f) else snap()

    fun popFloat(enabled: Boolean): AnimationSpec<Float> =
        if (enabled) spring(dampingRatio = 0.55f, stiffness = 900f) else snap()

    fun thumbSpring(enabled: Boolean): AnimationSpec<Float> =
        if (enabled) spring(dampingRatio = 0.72f, stiffness = 900f) else snap()

    fun fadeSpec(enabled: Boolean): FiniteAnimationSpec<Float> =
        if (enabled) tween(FAST_MS) else snap()

    fun placementSpec(enabled: Boolean): FiniteAnimationSpec<IntOffset> =
        if (enabled) {
            spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
        } else {
            snap()
        }

    fun <T> tweenMs(base: Int, enabled: Boolean): FiniteAnimationSpec<T> =
        tween(millis(base, enabled))

    @Composable
    fun enabled(): Boolean {
        val context = LocalContext.current
        return remember(context) {
            val scale = runCatching {
                Settings.Global.getFloat(
                    context.contentResolver,
                    Settings.Global.ANIMATOR_DURATION_SCALE,
                    1f
                )
            }.getOrDefault(1f)
            enabled(scale)
        }
    }
}

@Composable
internal fun Modifier.habitClickable(
    enabled: Boolean = true,
    role: Role? = null,
    onClick: () -> Unit
): Modifier {
    val motion = HabitMotion.enabled()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (pressed && enabled && motion) HabitMotion.PRESS_SCALE else 1f,
        animationSpec = HabitMotion.springFloat(motion),
        label = "pressScale"
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            enabled = enabled,
            role = role,
            interactionSource = interaction,
            indication = null,
            onClick = onClick
        )
}
