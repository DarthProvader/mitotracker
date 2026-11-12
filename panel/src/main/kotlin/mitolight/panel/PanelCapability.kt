package mitolight.panel

/**
 * How many channels a panel exposes and how dim it will go, decided from its Bluetooth name the
 * same way the vendor app decides it. `ML5 BULB` is the awkward case: it carries the 5.0 prefix
 * but has only two channels and ignores anything under 3 %.
 *
 * Only [TWO_CHANNEL] has been driven against real hardware (ADR 0004).
 */
enum class PanelCapability(val channelCount: Int, val minimumBrightness: Int) {
    TWO_CHANNEL(channelCount = 2, minimumBrightness = 1),
    SIX_CHANNEL(channelCount = 6, minimumBrightness = 1),
    BULB(channelCount = 2, minimumBrightness = 3),
    ;

    val hasLightModes: Boolean get() = this == SIX_CHANNEL

    companion object {
        fun ofName(name: String?): PanelCapability = when {
            name == null -> TWO_CHANNEL
            name.startsWith("ML5 BULB", ignoreCase = true) -> BULB
            name.startsWith("ML5", ignoreCase = true) -> SIX_CHANNEL
            else -> TWO_CHANNEL
        }
    }
}
