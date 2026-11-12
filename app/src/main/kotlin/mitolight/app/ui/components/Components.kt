@file:OptIn(ExperimentalMaterial3Api::class)

package mitolight.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import mitolight.app.R
import mitolight.app.ui.theme.MitoTheme
import mitolight.app.ui.theme.actionGradient
import mitolight.app.ui.theme.glass
import mitolight.app.ui.theme.glow

/** A section heading. Uppercase and widely tracked, which is how the instrument voice is set. */
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MitoTheme.type.labelSm,
        color = MitoTheme.colours.onSurfaceMuted,
        modifier = modifier,
    )
}

/**
 * A card. Translucent gradient, hairline edge, soft shadow — see `Surfaces.kt` for why depth came
 * back. [glow] adds the atmospheric wash a hero card gets; [accent] draws the lit edge that marks
 * what a row is about.
 */
@Composable
fun MitoCard(
    modifier: Modifier = Modifier,
    raised: Boolean = false,
    onClick: (() -> Unit)? = null,
    glow: Color? = null,
    accent: Color? = null,
    opaque: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colours = MitoTheme.colours
    val shape = MitoTheme.shape.xl

    val base = modifier
        .fillMaxWidth()
        .glass(
            shape = shape,
            glow = glow,
            hairline = if (raised) colours.hairlineBright else colours.hairline,
            opaque = opaque,
        )

    // A lit edge rather than a plain bar: the wash beside it is the glow, drawn here because a
    // coloured shadow cannot be cast by something whose height is only known at measure time.
    val edge = if (accent == null) {
        Modifier
    } else {
        Modifier.drawBehind {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(accent.copy(alpha = 0.28f), Color.Transparent),
                    startX = 0f,
                    endX = size.width * 0.3f,
                ),
            )
            drawRect(color = accent, size = Size(width = 6.dp.toPx(), height = size.height))
        }
    }

    Box(modifier = (if (onClick != null) base.clickable(onClick = onClick) else base).then(edge)) {
        Column(
            modifier = Modifier.padding(MitoTheme.space.cardPadding),
            content = content,
        )
    }
}

/**
 * One per screen. If two things are brand red, neither is the action — so this deliberately takes no
 * colour parameter. The gradient and the glow are the vendor's red doing what the hardware does.
 */
@Composable
fun PrimaryAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val colours = MitoTheme.colours
    val shape = MitoTheme.shape.lg

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .then(if (enabled) Modifier.glow(colours.primary, 22.dp, shape) else Modifier)
            .clip(shape)
            .background(if (enabled) actionGradient() else Brush.horizontalGradient(listOf(colours.surfaceHigh, colours.surfaceHigh)))
            .border(1.dp, if (enabled) colours.hairlineBright else colours.hairline, shape)
            .clickable(enabled = enabled, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = if (enabled) colours.onPrimary else colours.onSurfaceMuted,
                modifier = Modifier.size(26.dp).padding(end = MitoTheme.space.sm),
            )
        }
        Text(
            text = label.uppercase(),
            style = MitoTheme.type.labelLg,
            textAlign = TextAlign.Center,
            color = if (enabled) colours.onPrimary else colours.onSurfaceMuted,
        )
    }
}

@Composable
fun SecondaryAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val colours = MitoTheme.colours
    val shape = MitoTheme.shape.lg
    val tint = if (enabled) colours.onSurface else colours.onSurfaceMuted

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(shape)
            .background(colours.hairline)
            .border(1.dp, colours.hairlineBright, shape)
            .clickable(enabled = enabled, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp).padding(end = MitoTheme.space.sm),
            )
        }
        Text(text = label.uppercase(), style = MitoTheme.type.headlineSm, color = tint)
    }
}

/** Quiet, and never in `primary`, because its label is too small to pass AA in it. */
@Composable
fun TextAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    Row(
        modifier = modifier
            .clip(MitoTheme.shape.full)
            .clickable(onClick = onClick)
            .padding(horizontal = MitoTheme.space.md, vertical = MitoTheme.space.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MitoTheme.space.sm),
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = MitoTheme.colours.primaryBright,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(text = label, style = MitoTheme.type.titleSm, color = MitoTheme.colours.primaryBright)
    }
}

/**
 * A quiet, wide button for the thing a card leads to — Stitch's "adjust dose limits" treatment: a
 * step down from [SecondaryAction], and the only wide button allowed to sit inside a card.
 */
@Composable
fun CardAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val colours = MitoTheme.colours
    val shape = MitoTheme.shape.md

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(shape)
            .background(colours.hairline)
            .border(1.dp, colours.hairline, shape)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = colours.primaryBright,
                modifier = Modifier.size(18.dp).padding(end = MitoTheme.space.xs),
            )
        }
        Text(text = label.uppercase(), style = MitoTheme.type.labelSm, color = colours.primaryBright)
    }
}

@Composable
fun Chip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colours = MitoTheme.colours
    val shape = MitoTheme.shape.full

    Box(
        modifier = modifier
            .clip(shape)
            .background(if (selected) colours.surfaceHigh else colours.hairline)
            .border(1.dp, if (selected) colours.hairlineBright else colours.hairline, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = MitoTheme.space.lg, vertical = MitoTheme.space.sm),
    ) {
        Text(
            text = label,
            style = MitoTheme.type.titleSm,
            // Selection is never colour alone: the chosen chip is the only filled one.
            color = if (selected) colours.onSurface else colours.onSurfaceMuted,
        )
    }
}

/** A small tinted badge — a state worth naming, not a control. */
@Composable
fun Pill(
    label: String,
    tint: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val shape = MitoTheme.shape.full

    Row(
        modifier = modifier
            .clip(shape)
            .background(tint.copy(alpha = 0.12f))
            .border(1.dp, tint.copy(alpha = 0.22f), shape)
            .padding(horizontal = MitoTheme.space.md, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        icon?.let {
            Icon(imageVector = it, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
        }
        Text(text = label.uppercase(), style = MitoTheme.type.labelSm, color = tint)
    }
}

/** The same badge, but for the panel's own state, where the dot carries the colour and the word stays white. */
@Composable
fun StatusPill(label: String, dot: Color, modifier: Modifier = Modifier) {
    val colours = MitoTheme.colours
    val shape = MitoTheme.shape.full

    Row(
        modifier = modifier
            .clip(shape)
            .background(colours.deep.copy(alpha = 0.5f))
            .border(1.dp, colours.hairlineBright, shape)
            .padding(horizontal = MitoTheme.space.lg, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MitoTheme.space.sm),
    ) {
        GlowDot(dot)
        Text(text = label.uppercase(), style = MitoTheme.type.labelSm, color = colours.onSurface)
    }
}

/** A lit indicator, not a bullet: it has a halo, because everything in this app that means "on" does. */
@Composable
fun GlowDot(colour: Color, modifier: Modifier = Modifier, size: Dp = 6.dp) {
    Box(
        modifier = modifier
            .size(size)
            .glow(colour, radius = 8.dp, shape = MitoTheme.shape.full)
            .background(colour, MitoTheme.shape.full),
    )
}

@Composable
fun StatusDot(colour: Color, label: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MitoTheme.space.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlowDot(colour)
        Text(text = label, style = MitoTheme.type.bodySm, color = colour)
    }
}

/** A circular well with an icon in it. Recessed rather than raised: the highlight is on the rim. */
@Composable
fun IconBubble(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    fill: Color? = null,
    contentDescription: String? = null,
) {
    val shape = MitoTheme.shape.full

    Box(
        modifier = modifier
            .size(size)
            .background(fill ?: MitoTheme.colours.hairlineBright, shape)
            .border(1.dp, tint.copy(alpha = 0.22f), shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(size * 0.55f),
        )
    }
}

/**
 * A channel, laid out as an instrument reads: what it is on the left, what it is doing on the right,
 * and the rail underneath. Brightness is committed on release, because every drag step would
 * otherwise be a Bluetooth write.
 */
@Composable
fun ChannelFader(
    label: String,
    tint: Color,
    icon: ImageVector,
    percent: Int,
    onSet: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var value by remember { mutableFloatStateOf(percent.toFloat()) }
    var dragging by remember { mutableStateOf(false) }

    // Follows the panel, except while a thumb is on it: a status arrives every two seconds and would
    // otherwise snatch the fader back mid-drag.
    LaunchedEffect(percent) {
        if (!dragging) value = percent.toFloat()
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MitoTheme.space.md),
            ) {
                IconBubble(icon = icon, tint = tint, fill = tint.copy(alpha = 0.10f))
                Text(text = label, style = MitoTheme.type.titleSm, color = MitoTheme.colours.onSurface)
            }
            Text(
                text = "${value.roundToInt()}%",
                style = MitoTheme.type.metricMd,
                color = MitoTheme.colours.onSurface,
            )
        }
        Rail(
            value = value,
            range = 0f..100f,
            colour = tint,
            onValueChange = {
                dragging = true
                value = it
            },
            onCommit = {
                dragging = false
                onSet(value.roundToInt())
            },
        )
    }
}

/**
 * The countdown, in minutes rather than in a percentage of nothing. Zero means the panel stays lit
 * until it is stopped, which is a real setting and so has its own words.
 */
@Composable
fun MinuteFader(
    minutes: Int,
    onSet: (Int) -> Unit,
    modifier: Modifier = Modifier,
    maximum: Int = 20,
    icon: ImageVector? = null,
) {
    var value by remember { mutableFloatStateOf(minutes.toFloat()) }
    var dragging by remember { mutableStateOf(false) }

    LaunchedEffect(minutes) {
        if (!dragging) value = minutes.toFloat()
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MitoTheme.space.md),
            ) {
                icon?.let {
                    IconBubble(
                        icon = it,
                        tint = MitoTheme.colours.onSurfaceMuted,
                        fill = MitoTheme.colours.hairline,
                    )
                }
                Text(
                    text = stringResource(R.string.label_countdown),
                    style = MitoTheme.type.titleSm,
                    color = MitoTheme.colours.onSurface,
                )
            }
            Text(
                text = if (value.roundToInt() == 0) {
                    stringResource(R.string.word_hold)
                } else {
                    stringResource(R.string.format_min, value.roundToInt())
                },
                style = MitoTheme.type.metricMd,
                color = MitoTheme.colours.onSurface,
            )
        }
        Rail(
            value = value,
            range = 0f..maximum.toFloat(),
            colour = MitoTheme.colours.onSurfaceMuted,
            steps = maximum - 1,
            onValueChange = {
                dragging = true
                value = it
            },
            onCommit = {
                dragging = false
                onSet(value.roundToInt())
            },
        )
    }
}

/**
 * A 6dp rail with a lit knob on it, drawn by hand: Material's own track stipples tick marks along a
 * stepped slider and parks a dot at the far end, and both are noise on an instrument.
 */
@Composable
private fun Rail(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    colour: Color,
    onValueChange: (Float) -> Unit,
    onCommit: () -> Unit,
    steps: Int = 0,
) {
    val fraction = ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)

    Slider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onCommit,
        valueRange = range,
        steps = steps,
        thumb = { Knob(colour) },
        track = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(MitoTheme.shape.full)
                    .background(MitoTheme.colours.hairlineBright),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .clip(MitoTheme.shape.full)
                        .background(colour),
                )
            }
        },
    )
}

/** Lit, ringed in white, and 24dp because a thumb has to be findable without looking. */
@Composable
private fun Knob(colour: Color) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .glow(colour, radius = 12.dp, shape = MitoTheme.shape.full)
            .background(colour, MitoTheme.shape.full)
            .border(2.dp, MitoTheme.colours.onPrimary, MitoTheme.shape.full),
    )
}

/**
 * Minutes used against the day's allowance. Always labelled with real numbers, because a bar alone
 * cannot say how close is too close. The bright tip is where the fill "arrives", which is what makes
 * a small difference visible at a glance.
 */
@Composable
fun DoseBar(
    fraction: Float,
    over: Boolean,
    modifier: Modifier = Modifier,
) {
    val colours = MitoTheme.colours
    val colour = if (over) colours.warning else colours.primary
    val shape = MitoTheme.shape.full

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(shape)
            .background(colours.hairlineBright)
            .border(1.dp, if (over) colour.copy(alpha = 0.25f) else colours.hairline, shape),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(shape)
                .background(colour),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(20.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, colours.onPrimary.copy(alpha = 0.35f)),
                        ),
                    ),
            )
        }
    }
}

/** Two mutually exclusive views of the same screen, as a recessed segmented control. */
@Composable
fun TabPair(
    first: String,
    second: String,
    firstSelected: Boolean,
    onSelect: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colours = MitoTheme.colours
    val outer = MitoTheme.shape.md
    val inner = MitoTheme.shape.sm

    Row(
        modifier = modifier
            .clip(outer)
            .background(colours.deep.copy(alpha = 0.6f))
            .border(1.dp, colours.hairline, outer)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOf(first to true, second to false).forEach { (label, isFirst) ->
            val selected = firstSelected == isFirst

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(inner)
                    .then(
                        if (selected) {
                            Modifier
                                .background(
                                    Brush.verticalGradient(
                                        listOf(colours.surfaceHigh, colours.surface),
                                    ),
                                )
                                .border(1.dp, colours.hairlineBright, inner)
                        } else {
                            Modifier
                        },
                    )
                    .clickable { onSelect(isFirst) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MitoTheme.type.titleSm,
                    textAlign = TextAlign.Center,
                    color = if (selected) colours.onSurface else colours.onSurfaceMuted,
                )
            }
        }
    }
}

/** A hairline rule, for splitting a card's footer off from its body. */
@Composable
fun Divider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MitoTheme.colours.hairline),
    )
}

/** A screen's own title, under the app bar: a headline and the sentence that says what it is for. */
@Composable
fun ScreenTitle(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = title, style = MitoTheme.type.headlineLg, color = MitoTheme.colours.onSurface)
        subtitle?.let {
            Text(text = it, style = MitoTheme.type.bodyMd, color = MitoTheme.colours.onSurfaceMuted)
        }
    }
}

/** The circular icon button in the app bar and at the end of a row. */
@Composable
fun RoundIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    size: Dp = 44.dp,
    contentDescription: String? = null,
) {
    val shape = MitoTheme.shape.full

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(MitoTheme.colours.hairline)
            .border(1.dp, MitoTheme.colours.hairline, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint ?: MitoTheme.colours.onSurfaceMuted,
            modifier = Modifier.size(size * 0.5f),
        )
    }
}

/** A screen that fills its space with one centred thing — waiting, or nothing to show yet. */
@Composable
fun Centred(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier.fillMaxSize().padding(MitoTheme.space.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = content,
    )
}
