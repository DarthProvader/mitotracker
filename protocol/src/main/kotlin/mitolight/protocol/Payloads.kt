package mitolight.protocol

/**
 * A two-channel panel drives every red wavelength as channel 0 and every near-infrared one as
 * channel 1. A six-channel panel addresses 630, 660 and 670 nm as channels 0 to 2, and 810,
 * 830 and 850 nm as channels 3 to 5.
 */
const val CHANNEL_RED = 0
const val CHANNEL_NEAR_INFRARED = 1

val CHANNELS_RED_SEPARATE = intArrayOf(0, 1, 2)
val CHANNELS_NEAR_INFRARED_SEPARATE = intArrayOf(3, 4, 5)

/**
 * Zero switches the channel off. Any other value is raised to [minimumBrightness], because the
 * panel treats a level below its own floor as off and would silently ignore the request.
 */
fun brightnessPayload(channel: Int, brightness: Int, minimumBrightness: Int = 1): ByteArray {
    require(brightness in 0..100) { "brightness must be 0..100, was $brightness" }

    val level = if (brightness == 0) 0 else maxOf(brightness, minimumBrightness)
    return byteArrayOf(channel.toByte(), level.toByte())
}

fun countdownPayload(seconds: Int): ByteArray {
    require(seconds in 0..0xFFFF) { "a countdown of $seconds seconds does not fit two bytes" }

    return byteArrayOf((seconds shr 8).toByte(), seconds.toByte())
}

fun controlPayload(control: Control): ByteArray = byteArrayOf(control.code.toByte())

fun systemTimePayload(hour: Int, minute: Int, second: Int): ByteArray =
    byteArrayOf(hour.toByte(), minute.toByte(), second.toByte())
