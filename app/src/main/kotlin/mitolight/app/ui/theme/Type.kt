@file:OptIn(ExperimentalTextApi::class)

package mitolight.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import mitolight.app.R

/**
 * Mito Light's real display face is Aktiv Grotesk Extended, licensed from Adobe. Archivo is the
 * closest free neo-grotesque, and its variable width axis is used to lean towards the extended
 * proportions of the original rather than faking them with letter-spacing.
 */
private val archivo = FontFamily(
    Font(
        R.font.archivo_variable,
        FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500), FontVariation.width(WIDER)),
    ),
    Font(
        R.font.archivo_variable,
        FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700), FontVariation.width(WIDER)),
    ),
)

/** Body text, at 15sp, exactly as Mito Light's own app sets it. */
private val poppins = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
)

/**
 * Numbers only. A proportional digit changes width as it changes value, so a running countdown
 * visibly twitches; monospaced digits hold still.
 */
private val robotoMono = FontFamily(
    // Light, because a 56sp countdown at book weight is a wall of ink. Thin large numerals are how
    // instruments are read at a glance, and the size carries the emphasis instead of the weight.
    Font(
        R.font.roboto_mono_variable,
        FontWeight.Light,
        variationSettings = FontVariation.Settings(FontVariation.weight(300)),
    ),
    Font(
        R.font.roboto_mono_variable,
        FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400)),
    ),
    Font(
        R.font.roboto_mono_variable,
        FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500)),
    ),
)

/** Archivo's width axis runs 62–125; a little over normal echoes the extended brand face. */
private const val WIDER = 105f

@Immutable
data class MitoType(
    val headlineLg: TextStyle = TextStyle(
        fontFamily = archivo,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 34.5.sp,
        letterSpacing = 0.01.em,
    ),
    val headlineMd: TextStyle = TextStyle(
        fontFamily = archivo,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 31.sp,
        letterSpacing = 0.01.em,
    ),
    val headlineSm: TextStyle = TextStyle(
        fontFamily = archivo,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.01.em,
    ),
    val titleMd: TextStyle = TextStyle(
        fontFamily = poppins,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    val titleSm: TextStyle = TextStyle(
        fontFamily = poppins,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.007.em,
    ),
    val bodyLg: TextStyle = TextStyle(
        fontFamily = poppins,
        fontSize = 16.sp,
        lineHeight = 26.sp,
    ),
    val bodyMd: TextStyle = TextStyle(
        fontFamily = poppins,
        fontSize = 15.sp,
        lineHeight = 24.sp,
    ),
    val bodySm: TextStyle = TextStyle(
        fontFamily = poppins,
        fontSize = 13.sp,
        lineHeight = 19.5.sp,
    ),
    /**
     * 20sp rather than the vendor app's 16, and the extra pixels are load bearing: white on
     * `#F50537` is 4.23:1, which passes AA only at large-text sizes, and bold type counts as large
     * from 18.66sp up. So the primary action can stay brand red without an asterisk.
     */
    val labelLg: TextStyle = TextStyle(
        fontFamily = archivo,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.02.em,
    ),
    val labelMd: TextStyle = TextStyle(
        fontFamily = poppins,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.03.em,
    ),
    /** Section eyebrows, always uppercase. Nothing else in the app is. */
    val labelSm: TextStyle = TextStyle(
        fontFamily = poppins,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.15.em,
    ),
    val metricLg: TextStyle = TextStyle(
        fontFamily = robotoMono,
        fontWeight = FontWeight.Light,
        fontSize = 56.sp,
        lineHeight = 56.sp,
        letterSpacing = (-0.04).em,
    ),
    val metricMd: TextStyle = TextStyle(
        fontFamily = robotoMono,
        fontWeight = FontWeight.Light,
        fontSize = 24.sp,
        lineHeight = 29.sp,
    ),
    val metricSm: TextStyle = TextStyle(
        fontFamily = robotoMono,
        fontWeight = FontWeight.Light,
        fontSize = 13.sp,
        lineHeight = 16.sp,
    ),
)
