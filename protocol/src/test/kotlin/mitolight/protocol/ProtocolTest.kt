package mitolight.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The byte vectors come from the vendor app's source and have already been driven into the real
 * panel by the Python driver in `tools/mito.py`, so a failure here means this port drifted —
 * not that the hardware changed its mind.
 */
class ProtocolTest {

    @Test
    fun `authentication carries its value as ascii`() {
        // A placeholder: the real value is supplied at build time, not kept in the repository.
        assertEquals(
            "3a ff 00 00 04 74 65 73 74 c3 0a",
            encode(Command.AUTHENTICATION, "test".toByteArray(Charsets.US_ASCII)).toHex(),
        )
    }

    @Test
    fun `starting and stopping the light differ by one byte`() {
        assertEquals("3a ff 20 00 01 01 21 0a", encode(Command.CONTROL, controlPayload(Control.START)).toHex())
        assertEquals("3a ff 20 00 01 00 20 0a", encode(Command.CONTROL, controlPayload(Control.STOP)).toHex())
    }

    @Test
    fun `a query has an empty payload and a zero length`() {
        assertEquals("3a ff 10 00 00 0f 0a", encode(Command.DEVICE_STATUS).toHex())
    }

    @Test
    fun `brightness is addressed per channel`() {
        assertEquals(
            "3a ff 41 00 02 00 32 74 0a",
            encode(Command.SET_DEVICE_CHANNEL_BRIGHTNESS, brightnessPayload(CHANNEL_RED, 50)).toHex(),
        )
        assertEquals(
            "3a ff 41 00 02 01 64 a7 0a",
            encode(Command.SET_DEVICE_CHANNEL_BRIGHTNESS, brightnessPayload(CHANNEL_NEAR_INFRARED, 100)).toHex(),
        )
    }

    @Test
    fun `a countdown is two bytes of seconds`() {
        assertEquals("3a ff 31 00 02 02 58 8c 0a", encode(Command.SET_DEVICE_COUNTDOWN, countdownPayload(600)).toHex())
    }

    @Test
    fun `a dim channel is raised to the panel's floor but zero still means off`() {
        assertEquals(5, brightnessPayload(CHANNEL_RED, 3, minimumBrightness = 5)[1].toInt())
        assertEquals(0, brightnessPayload(CHANNEL_RED, 0, minimumBrightness = 5)[1].toInt())
    }

    @Test
    fun `a reply decodes into a panel status`() {
        val decoded = decode(reply(Command.DEVICE_STATUS, byteArrayOf(1, 0x02, 0x58, 0x02, 0x3A)))

        assertEquals(1, decoded.frames.size)
        assertEquals(0, decoded.remainder.size)

        val status = PanelStatus.parse(decoded.frames.single().payload)
        assertNotNull(status)
        assertTrue(status.running)
        assertEquals(600, status.countdownSeconds)
        assertEquals(570, status.remainingSeconds)
    }

    @Test
    fun `frames coalesced into one chunk are split and a partial one is kept back`() {
        val chunk = encode(Command.CONTROL, controlPayload(Control.START)) +
            encode(Command.DEVICE_STATUS) +
            byteArrayOf(HEADER_COMMAND.toByte(), ADDRESS_BROADCAST.toByte(), 0x10)

        val decoded = decode(chunk)

        assertEquals(2, decoded.frames.size)
        assertEquals(3, decoded.remainder.size)
    }

    @Test
    fun `a frame whose checksum does not add up is discarded`() {
        val frame = encode(Command.CONTROL, controlPayload(Control.START))
        frame[frame.lastIndex - 1] = (frame[frame.lastIndex - 1].toInt() xor 0xFF).toByte()

        assertTrue(decode(frame).frames.isEmpty())
    }

    /** The header sits outside the checksum, so a reply is a command frame with a `'*'` in front. */
    private fun reply(command: Command, payload: ByteArray): ByteArray =
        encode(command, payload).also { it[0] = HEADER_REPLY.toByte() }
}
