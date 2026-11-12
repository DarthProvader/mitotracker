package mitolight.protocol

/**
 * Command bytes under the names the vendor app gives them, so this list can be read against
 * `docs/protocol.md` without translation.
 */
enum class Command(val code: Int) {
    AUTHENTICATION(0x00),
    PROTOCOL_VERSION(0x01),
    GET_SYSTEM_TIME(0x02),
    SET_SYSTEM_TIME(0x03),
    DEVICE_INFORMATION(0x08),
    DEVICE_STATUS(0x10),
    CONTROL(0x20),
    QUICK_START(0x21),
    GET_DEVICE_COUNTDOWN(0x30),
    SET_DEVICE_COUNTDOWN(0x31),
    GET_DEVICE_CHANNEL_PARAMETERS(0x40),
    SET_DEVICE_CHANNEL_BRIGHTNESS(0x41),
    SET_DEVICE_CHANNEL_PULSE(0x42),
    SET_LIGHT_MODE(0x50),
    GET_LIGHT_MODE(0x51),
    GET_CHANNELS_FOR_MODE(0x52),
    GET_BRIGHTNESS_AND_PULSE_FOR_MODE(0x53),
    SET_BRIGHTNESS_AND_PULSE_FOR_MODE(0x54),
    GET_ALARMS(0x60),
    ADD_ALARM(0x61),
    DELETE_ALARM_BY_TIME(0x62),
    DELETE_ALARM_BY_ID(0x63),
    MODIFY_ALARM_BY_ID(0x64),
    TURN_ON_OFF_ALARM_BY_ID(0x65),
    ;

    companion object {
        private val byCode = entries.associateBy(Command::code)

        fun of(code: Int): Command? = byCode[code]
    }
}

/** The payload of [Command.CONTROL]. */
enum class Control(val code: Int) {
    STOP(0x00),
    START(0x01),
    RESET(0x02),
}
