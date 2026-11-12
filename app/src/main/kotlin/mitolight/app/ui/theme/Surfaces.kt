package mitolight.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Depth, as the Stitch drawings do it. `DESIGN.md` called for flat tonal steps, which is the safe
 * answer for a dark interface and also the reason the first build looked like a settings screen. What
 * replaces it is not decoration for its own sake: a panel is several hundred red LEDs pointed at a
 * wall, so the interface sits in a room with a maroon cast, cards are translucent enough to show it,
 * and anything that emits light in the app glows the way the hardware does.
 */

/** Near-black with the panel's own light spilling down from behind the top of the screen. */
@Composable
fun Modifier.roomLight(): Modifier {
    val colours = MitoTheme.colours

    return background(colours.background).drawBehind {
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(colours.vignette, Color.Transparent),
                center = Offset(size.width / 2f, -size.height * 0.2f),
                radius = size.height * 0.75f,
            ),
        )
    }
}

/**
 * A card: a 135° translucent gradient, a hairline of white at 5 % to catch the edge, and a wide soft
 * shadow underneath. The hairline is what stops the whole screen dissolving into one dark mass.
 *
 * [opaque] lays the background colour underneath the gradient first, for a card that has to be read
 * against another card — a modal over a lit control screen, where letting the screen behind show
 * through is not atmosphere, it is two interfaces on top of each other.
 */
@Composable
fun Modifier.glass(
    shape: Shape = MitoTheme.shape.xl,
    glow: Color? = null,
    hairline: Color? = null,
    opaque: Boolean = false,
): Modifier {
    val colours = MitoTheme.colours

    return this
        .shadow(elevation = 10.dp, shape = shape, clip = false)
        .then(if (opaque) Modifier.background(colours.background, shape) else Modifier)
        .background(
            brush = Brush.linearGradient(listOf(colours.glassTop, colours.glassBottom)),
            shape = shape,
        )
        .clip(shape)
        .then(if (glow == null) Modifier else Modifier.innerGlow(glow))
        .border(1.dp, hairline ?: colours.hairline, shape)
}

/** The atmospheric wash inside a card, centred and fading out well before the edges. */
fun Modifier.innerGlow(colour: Color): Modifier = drawBehind {
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(colour, Color.Transparent),
            center = Offset(size.width / 2f, size.height / 2f),
            radius = maxOf(size.width, size.height) * 0.7f,
        ),
    )
}

/**
 * Light coming off something, drawn as a coloured shadow. Android renders those from API 28, and
 * below that it falls back to a plain dark shadow, which is a downgrade rather than a defect.
 */
@Composable
fun Modifier.glow(colour: Color, radius: Dp = 20.dp, shape: Shape = MitoTheme.shape.lg): Modifier =
    shadow(elevation = radius, shape = shape, clip = false, ambientColor = colour, spotColor = colour)

/** The primary action's fill: brand red across to the pressed red, left to right. */
@Composable
fun actionGradient(): Brush =
    Brush.horizontalGradient(listOf(MitoTheme.colours.primary, MitoTheme.colours.primaryPressed))
