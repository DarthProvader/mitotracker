package mitolight.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import mitolight.app.R
import mitolight.app.ui.theme.MitoTheme
import mitolight.app.ui.theme.glow

/**
 * A channel as a column of light rather than a line with a dot on it.
 *
 * A two-channel panel has exactly two of these. They sit as a centred pair at half the previous
 * column width and a taller fill, so the level still reads from across a room without dominating
 * the Output screen. The buttons above and below are there because a small correction is not a
 * drag gesture. The step is five per cent — finer than anybody can see, coarse enough to reach an
 * end in a few taps.
 */
@Composable
fun VerticalFader(
    label: String,
    percent: Int,
    tint: Color,
    onSet: (Int) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 300.dp,
) {
    var value by remember { mutableFloatStateOf(percent.toFloat()) }
    var dragging by remember { mutableStateOf(false) }
    var columnPx by remember { mutableFloatStateOf(1f) }

    // Follows the panel, except while a thumb is on it: a status arrives every two seconds and would
    // otherwise snatch the fader back mid-drag.
    LaunchedEffect(percent) {
        if (!dragging) value = percent.toFloat()
    }

    fun set(next: Float) {
        value = next.coerceIn(0f, 100f)
    }

    fun step(by: Int) {
        set((value.roundToInt() + by).toFloat())
        onSet(value.roundToInt())
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MitoTheme.space.md),
    ) {
        StepButton(
            icon = Icons.Outlined.Add,
            description = stringResource(R.string.a11y_more, label),
        ) { step(STEP) }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .glow(tint.copy(alpha = (value / 100f).coerceIn(0f, 1f)), shape = MitoTheme.shape.md)
                .clip(MitoTheme.shape.md)
                .background(MitoTheme.colours.deep)
                .border(1.dp, MitoTheme.colours.hairline, MitoTheme.shape.md)
                .onSizeChanged { columnPx = it.height.toFloat().coerceAtLeast(1f) }
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { dragging = true },
                        onDragEnd = {
                            dragging = false
                            onSet(value.roundToInt())
                        },
                        onDragCancel = {
                            dragging = false
                            onSet(value.roundToInt())
                        },
                    ) { change, _ ->
                        set((1f - change.position.y / columnPx) * 100f)
                    }
                },
            contentAlignment = Alignment.BottomCenter,
        ) {
            // The lit part, with a bright lip along its top edge so the level reads even where the
            // fill is dark. Both are clipped by the column, which is why a fill of nearly nothing
            // does not leave a white bar hanging off the bottom. It fades downwards rather than
            // being one flat colour: a flat block reads as a paint swatch, a fade as light.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight((value / 100f).coerceIn(0f, 1f))
                    .background(
                        Brush.verticalGradient(
                            listOf(tint, tint.copy(alpha = 0.55f), tint.copy(alpha = 0.28f)),
                        ),
                    ),
                contentAlignment = Alignment.TopCenter,
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(5.dp)
                        .glow(tint, radius = 10.dp, shape = MitoTheme.shape.full)
                        .clip(MitoTheme.shape.full)
                        .background(MitoTheme.colours.onPrimary),
                )
            }

            Column(
                modifier = Modifier.padding(bottom = MitoTheme.space.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "${value.roundToInt()}%",
                    style = MitoTheme.type.headlineSm,
                    color = MitoTheme.colours.onPrimary,
                )
                Text(
                    text = label.uppercase(),
                    style = MitoTheme.type.labelSm,
                    color = MitoTheme.colours.onPrimary,
                )
            }
        }

        StepButton(
            icon = Icons.Outlined.Remove,
            description = stringResource(R.string.a11y_less, label),
        ) { step(-STEP) }
    }
}

@Composable
private fun StepButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(56.dp)
            .height(40.dp)
            .clip(MitoTheme.shape.md)
            .background(MitoTheme.colours.hairline)
            .border(1.dp, MitoTheme.colours.hairlineBright, MitoTheme.shape.md)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = MitoTheme.colours.onSurface,
            modifier = Modifier.size(20.dp),
        )
    }
}

private const val STEP = 5
