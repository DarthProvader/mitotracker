package mitolight.protocol

/**
 * A frame is a small ASCII-delimited envelope around a command byte and its payload:
 *
 *     3A FF <cmd> <len hi> <len lo> <payload...> <checksum> 0A
 *     ':'                                                  '\n'
 *
 * The checksum is the low byte of the sum of every byte between the header and the checksum
 * itself, which means the header is not covered by it. Replies are identical except for a
 * `'*'` header.
 */

const val HEADER_COMMAND = 0x3A
const val HEADER_REPLY = 0x2A
const val FOOTER = 0x0A
const val ADDRESS_BROADCAST = 0xFF

/** Header, address, command, two length bytes, checksum and footer. */
const val FRAME_OVERHEAD = 7

const val SERVICE_UUID = "0000fff0-0000-1000-8000-00805f9b34fb"
const val WRITE_UUID = "0000fff2-0000-1000-8000-00805f9b34fb"
const val NOTIFY_UUID = "0000fff1-0000-1000-8000-00805f9b34fb"

private val NO_PAYLOAD = ByteArray(0)

class Frame(
    val header: Int,
    val address: Int,
    val command: Int,
    val payload: ByteArray,
) {
    val commandName: String
        get() = Command.of(command)?.name ?: "UNKNOWN_0x%02X".format(command)

    override fun toString(): String =
        if (payload.isEmpty()) commandName else "$commandName ${payload.toHex()}"
}

class Decoded(val frames: List<Frame>, val remainder: ByteArray)

fun encode(command: Command, payload: ByteArray = NO_PAYLOAD): ByteArray = encode(command.code, payload)

fun encode(command: Int, payload: ByteArray = NO_PAYLOAD): ByteArray {
    require(payload.size <= 0xFFFF) { "a payload of ${payload.size} bytes does not fit the length field" }

    val frame = ByteArray(FRAME_OVERHEAD + payload.size)
    frame[0] = HEADER_COMMAND.toByte()
    frame[1] = ADDRESS_BROADCAST.toByte()
    frame[2] = command.toByte()
    frame[3] = (payload.size shr 8).toByte()
    frame[4] = payload.size.toByte()
    payload.copyInto(frame, 5)
    frame[frame.lastIndex - 1] = checksum(frame, 1, frame.lastIndex - 1).toByte()
    frame[frame.lastIndex] = FOOTER.toByte()
    return frame
}

/**
 * Pulls whole frames out of a stream and returns them with whatever is left over.
 *
 * Notifications split and coalesce frames freely, so the caller keeps the remainder and feeds
 * it back in with the next chunk. A frame that fails its checksum is skipped one byte at a
 * time rather than dropping the rest of the buffer, since the real frame may start inside it.
 */
fun decode(buffer: ByteArray): Decoded {
    val frames = mutableListOf<Frame>()
    var offset = 0

    while (buffer.size - offset >= FRAME_OVERHEAD) {
        val header = buffer[offset].toInt() and 0xFF
        if (header != HEADER_COMMAND && header != HEADER_REPLY) {
            offset++
            continue
        }

        val length = ((buffer[offset + 3].toInt() and 0xFF) shl 8) or (buffer[offset + 4].toInt() and 0xFF)
        val end = offset + FRAME_OVERHEAD + length
        if (end > buffer.size) break

        val statedChecksum = buffer[end - 2].toInt() and 0xFF
        val footer = buffer[end - 1].toInt() and 0xFF
        if (footer != FOOTER || statedChecksum != checksum(buffer, offset + 1, end - 2)) {
            offset++
            continue
        }

        frames += Frame(
            header = header,
            address = buffer[offset + 1].toInt() and 0xFF,
            command = buffer[offset + 2].toInt() and 0xFF,
            payload = buffer.copyOfRange(offset + 5, end - 2),
        )
        offset = end
    }

    return Decoded(frames, buffer.copyOfRange(offset, buffer.size))
}

internal fun checksum(bytes: ByteArray, fromIndex: Int, toIndex: Int): Int {
    var sum = 0
    for (index in fromIndex until toIndex) sum += bytes[index].toInt() and 0xFF
    return sum and 0xFF
}

fun ByteArray.toHex(): String = joinToString(" ") { "%02x".format(it.toInt() and 0xFF) }
