package mitolight.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mitolight.app.ui.theme.MitoTheme
import mitolight.app.ui.theme.glow

/**
 * The one thing the app is for.
 *
 * A panel is a switch with a timer on it, and everything else in this app is bookkeeping around
 * that. So the switch is a single round button at the centre of the landing screen, big enough to
 * hit without looking, and it says out loud what it will do — including when what it will do is
 * "find a panel first". While the light is on it becomes the pause control and carries the countdown
 * as a ring; ending the session sits under it as a separate control.
 */
@Composable
fun PowerButton(
    icon: ImageVector,
    caption: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    lit: Boolean = false,
    progress: Float? = null,
    diameter: Dp = 132.dp,
) {
    val colours = MitoTheme.colours
    val shape = MitoTheme.shape.full
    val enabled = onClick != null

    val tint = when {
        !enabled -> colours.onSurfaceMuted
        lit -> colours.primaryBright
        else -> colours.channelRed
    }

    // Breathing, and only while the panel is lit: the halo is standing in for light in the room, and
    // light in the room is not a static thing. Off, it holds still, which is the difference.
    val transition = rememberInfiniteTransition(label = "power")
    val halo by transition.animateFloat(
        initialValue = if (lit) 18f else 24f,
        targetValue = if (lit) 40f else 24f,
        animationSpec = infiniteRepeatable(tween(1_800), RepeatMode.Reverse),
        label = "halo",
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MitoTheme.space.md),
    ) {
        Box(
            modifier = Modifier
                .size(diameter)
                .then(if (enabled) Modifier.glow(tint, radius = halo.dp, shape = shape) else Modifier)
                .clip(shape)
                .background(colours.deep)
                .border(1.dp, if (enabled) tint.copy(alpha = 0.55f) else colours.hairline, shape)
                .ring(colour = tint, progress = progress, enabled = enabled)
                .then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = caption,
                tint = tint,
                modifier = Modifier.size(diameter * 0.4f),
            )
        }

        Text(
            text = caption.uppercase(),
            style = MitoTheme.type.labelSm,
            color = if (enabled) colours.onSurface else colours.onSurfaceMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = MitoTheme.space.lg),
        )
    }
}

/**
 * A wash inside the disc and, when there is a countdown to show, the fraction of it that is left as
 * an arc. Drawn from twelve o'clock and clockwise, which is the only direction anybody reads a dial.
 */
private fun Modifier.ring(colour: Color, progress: Float?, enabled: Boolean): Modifier = drawBehind {
    if (enabled) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(colour.copy(alpha = 0.22f), Color.Transparent),
                center = center,
                radius = size.minDimension * 0.6f,
            ),
        )
    }

    progress ?: return@drawBehind

    val width = 5.dp.toPx()
    val inset = width / 2f

    drawArc(
        color = colour,
        startAngle = -90f,
        sweepAngle = 360f * progress.coerceIn(0f, 1f),
        useCenter = false,
        topLeft = Offset(inset, inset),
        size = Size(size.width - width, size.height - width),
        style = Stroke(width = width, cap = StrokeCap.Round),
    )
}
