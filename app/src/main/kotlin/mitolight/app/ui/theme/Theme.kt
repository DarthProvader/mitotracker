package mitolight.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The 4px scale from the vendor app's own `Dimens`, named the way `DESIGN.md` names it. */
@Immutable
data class MitoSpace(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val xxxl: Dp = 48.dp,
    val screenMargin: Dp = 20.dp,
    val cardPadding: Dp = 20.dp,
    val gutter: Dp = 12.dp,
)

/** If it is a pill, it does something. That is the strongest shape signal on the screen. */
@Immutable
data class MitoShape(
    /** Only for things too small to carry an 8dp radius, like a day in the calendar. */
    val xs: RoundedCornerShape = RoundedCornerShape(4.dp),
    val sm: RoundedCornerShape = RoundedCornerShape(8.dp),
    val md: RoundedCornerShape = RoundedCornerShape(12.dp),
    val lg: RoundedCornerShape = RoundedCornerShape(16.dp),
    val xl: RoundedCornerShape = RoundedCornerShape(24.dp),
    val full: RoundedCornerShape = RoundedCornerShape(percent = 50),
)

private val LocalMitoColours = staticCompositionLocalOf { MitoColours() }
private val LocalMitoType = staticCompositionLocalOf { MitoType() }
private val LocalMitoSpace = staticCompositionLocalOf { MitoSpace() }
private val LocalMitoShape = staticCompositionLocalOf { MitoShape() }

object MitoTheme {
    val colours: MitoColours
        @Composable @ReadOnlyComposable get() = LocalMitoColours.current

    val type: MitoType
        @Composable @ReadOnlyComposable get() = LocalMitoType.current

    val space: MitoSpace
        @Composable @ReadOnlyComposable get() = LocalMitoSpace.current

    val shape: MitoShape
        @Composable @ReadOnlyComposable get() = LocalMitoShape.current
}

/**
 * Dark only, which is a use-context decision rather than a fashion: a panel is used in a dim room,
 * often in the evening, with the phone propped up a metre away while several hundred red LEDs are
 * pointed at somebody. A white screen in that room is a second, unwanted light source.
 *
 * Material's scheme is filled in as well as our own tokens, so stock components land in the right
 * place instead of falling back to purple.
 */
@Composable
fun MitoTheme(content: @Composable () -> Unit) {
    val colours = MitoColours()
    val type = MitoType()

    CompositionLocalProvider(
        LocalMitoColours provides colours,
        LocalMitoType provides type,
        LocalMitoSpace provides MitoSpace(),
        LocalMitoShape provides MitoShape(),
    ) {
        MaterialTheme(
            colorScheme = darkColorScheme(
                primary = colours.primary,
                onPrimary = colours.onPrimary,
                primaryContainer = colours.primaryPressed,
                onPrimaryContainer = colours.onPrimary,
                secondary = colours.channelNir,
                onSecondary = colours.onPrimary,
                background = colours.background,
                onBackground = colours.onSurface,
                surface = colours.surface,
                onSurface = colours.onSurface,
                surfaceVariant = colours.surfaceHigh,
                onSurfaceVariant = colours.onSurfaceMuted,
                surfaceContainer = colours.surface,
                surfaceContainerHigh = colours.surfaceHigh,
                outline = colours.outline,
                outlineVariant = colours.outline,
                error = colours.error,
                onError = colours.onPrimaryDark,
                scrim = colours.scrim,
            ),
            typography = Typography(
                headlineLarge = type.headlineLg,
                headlineMedium = type.headlineMd,
                headlineSmall = type.headlineSm,
                titleLarge = type.headlineSm,
                titleMedium = type.titleMd,
                titleSmall = type.titleSm,
                bodyLarge = type.bodyLg,
                bodyMedium = type.bodyMd,
                bodySmall = type.bodySm,
                labelLarge = type.labelLg,
                labelMedium = type.labelMd,
                labelSmall = type.labelSm,
            ),
            shapes = Shapes(
                extraSmall = RoundedCornerShape(8.dp),
                small = RoundedCornerShape(8.dp),
                medium = RoundedCornerShape(12.dp),
                large = RoundedCornerShape(16.dp),
                extraLarge = RoundedCornerShape(24.dp),
            ),
            content = content,
        )
    }
}
