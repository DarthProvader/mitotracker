package mitolight.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.EmojiPeople
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import mitolight.app.ui.label
import mitolight.app.ui.theme.MitoTheme
import mitolight.app.ui.theme.glass
import mitolight.app.ui.theme.glow
import mitolight.domain.Area

/**
 * A body part as a tile rather than a word in a list. Twelve areas is too many to read as text at
 * arm's length, and this screen is used at arm's length: the phone is propped up and somebody is
 * standing in front of a lit panel.
 */
@Composable
fun AreaTile(
    area: Area,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colours = MitoTheme.colours
    val shape = MitoTheme.shape.md
    val tint = if (selected) colours.channelRed else colours.onSurfaceMuted

    // Opaque, unlike a card: the glow is a coloured shadow, and a translucent fill lets the part of
    // it that falls under the tile show through as a dark square. Small and lit has to be solid.
    val surface = if (selected) {
        Modifier
            .glow(colours.channelRed, radius = 16.dp, shape = shape)
            .clip(shape)
            .background(colours.surface)
            .border(1.dp, colours.channelRed, shape)
    } else {
        Modifier
            .clip(shape)
            .background(colours.glassTop.copy(alpha = 0.45f))
            .border(1.dp, colours.hairline, shape)
    }

    Column(
        modifier = modifier
            .aspectRatio(1f)
            .then(surface)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = area.icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(26.dp),
        )
        Text(
            text = area.label(),
            style = MitoTheme.type.labelSm,
            color = tint,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, start = 2.dp, end = 2.dp),
        )
    }
}

/**
 * The whole fixed list, laid out by hand so it can live inside a scrolling column. Four to a row
 * rather than three, because twelve areas then fit in three rows: this screen stands between somebody
 * and a lit panel, and scrolling to find a knee is the wrong thing to be doing at that moment.
 */
@Composable
fun AreaGrid(
    chosen: List<Area>,
    onToggle: (Area) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = 4,
) {
    val gutter = MitoTheme.space.sm

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(gutter)) {
        Area.entries.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(gutter)) {
                row.forEach { area ->
                    AreaTile(
                        area = area,
                        selected = area in chosen,
                        onClick = { onToggle(area) },
                        modifier = Modifier.weight(1f),
                    )
                }
                // A short last row keeps the tiles square instead of stretching them.
                repeat(columns - row.size) { Column(modifier = Modifier.weight(1f)) {} }
            }
        }
    }
}

/**
 * Material's pictograms are a loose fit for anatomy — there is no icon for "lower back" — so these
 * are chosen to be told apart at a glance rather than to be literal, and the label always carries
 * the meaning.
 */
val Area.icon: ImageVector
    get() = when (this) {
        Area.SCALP -> Icons.Filled.Psychology
        Area.FACE_AND_NECK -> Icons.Filled.Face
        Area.SHOULDERS -> Icons.Filled.FitnessCenter
        Area.CHEST -> Icons.Filled.MonitorHeart
        Area.ABDOMEN -> Icons.Filled.SelfImprovement
        Area.UPPER_BACK -> Icons.Filled.Accessibility
        Area.LOWER_BACK -> Icons.Filled.AccessibilityNew
        Area.ARMS -> Icons.Filled.PanTool
        Area.HIPS -> Icons.Filled.EmojiPeople
        Area.THIGHS -> Icons.Filled.DirectionsRun
        Area.KNEES -> Icons.Filled.Hiking
        Area.FEET -> Icons.Filled.DirectionsWalk
    }
