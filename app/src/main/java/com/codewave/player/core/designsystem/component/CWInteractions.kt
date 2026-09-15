package com.codewave.player.core.designsystem.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role

/**
 * Emil Kowalski design engineering principle:
 * Interactive elements must provide immediate tactile feedback on press.
 * Subtle 0.96 scale with punchy 120ms ease-out.
 */
fun Modifier.tactilePress(
    targetScale: Float = 0.96f,
    interactionSource: MutableInteractionSource? = null
): Modifier = composed {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isPressed by source.collectIsPressedAsState()
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) targetScale else 1f,
        animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
        label = "tactileScale"
    )

    this.graphicsLayer {
        scaleX = animatedScale
        scaleY = animatedScale
    }
}

/**
 * Clickable modifier with integrated tactile press scale and zero sluggish delay.
 */
fun Modifier.tactileClickable(
    enabled: Boolean = true,
    role: Role? = null,
    targetScale: Float = 0.96f,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed && enabled) targetScale else 1f,
        animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
        label = "tactileClickScale"
    )

    this
        .graphicsLayer {
            scaleX = animatedScale
            scaleY = animatedScale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            role = role,
            onClick = onClick
        )
}
