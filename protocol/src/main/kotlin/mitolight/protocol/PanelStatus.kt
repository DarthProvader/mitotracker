package mitolight.protocol

/** What a panel reports in reply to [Command.DEVICE_STATUS]. */
data class PanelStatus(
    val running: Boolean,
    val countdownSeconds: Int,
    val remainingSeconds: Int,
) {
    override fun toString(): String {
        val state = if (running) "running" else "idle"
        val remaining = "%d:%02d".format(remainingSeconds / 60, remainingSeconds % 60)
        return "$state, countdown ${countdownSeconds / 60} min, remaining $remaining"
    }

    companion object {
        fun parse(payload: ByteArray): PanelStatus? {
            if (payload.size != 5) return null

            return PanelStatus(
                running = payload[0].toInt() == 1,
                countdownSeconds = ((payload[1].toInt() and 0xFF) shl 8) or (payload[2].toInt() and 0xFF),
                remainingSeconds = ((payload[3].toInt() and 0xFF) shl 8) or (payload[4].toInt() and 0xFF),
            )
        }
    }
}
