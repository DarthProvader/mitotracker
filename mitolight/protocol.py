"""Wire protocol of the Mito Light panel.

Frames are a small ASCII-delimited envelope around a command byte and payload:

    3A FF <cmd> <len_hi> <len_lo> <payload...> <checksum> 0A
    :                                                     \n

The checksum is the low byte of the sum of everything between the header and
the checksum itself. Responses use 2A ('*') as the header and are otherwise
identical.
"""

from __future__ import annotations

import os
from dataclasses import dataclass
from enum import IntEnum

HEADER_WRITE = 0x3A
HEADER_READ = 0x2A
FOOTER = 0x0A
ADDRESS_BROADCAST = 0xFF
OVERHEAD = 7

SERVICE_UUID = "0000fff0-0000-1000-8000-00805f9b34fb"
WRITE_UUID = "0000fff2-0000-1000-8000-00805f9b34fb"
NOTIFY_UUID = "0000fff1-0000-1000-8000-00805f9b34fb"

# The panel expects an AUTHENTICATION frame before anything else. Its value is not
# part of this repository: supply it through the environment.
AUTH_PASSWORD_ENV = "MITOLIGHT_AUTH_PASSWORD"


def auth_password() -> bytes:
    value = os.environ.get(AUTH_PASSWORD_ENV)
    if not value:
        raise RuntimeError(f"Set {AUTH_PASSWORD_ENV} to the panel's authentication value.")
    return value.encode("ascii")


class Command(IntEnum):
    AUTHENTICATION = 0x00
    PROTOCOL_VERSION = 0x01
    GET_SYSTEM_TIME = 0x02
    SET_SYSTEM_TIME = 0x03
    DEVICE_INFORMATION = 0x08
    DEVICE_STATUS = 0x10
    CONTROL = 0x20
    QUICK_START = 0x21
    GET_DEVICE_COUNTDOWN = 0x30
    SET_DEVICE_COUNTDOWN = 0x31
    GET_DEVICE_CHANNEL_PARAMETERS = 0x40
    SET_DEVICE_CHANNEL_BRIGHTNESS = 0x41
    SET_DEVICE_CHANNEL_PULSE = 0x42
    SET_LIGHT_MODE = 0x50
    GET_LIGHT_MODE = 0x51
    GET_CHANNELS_FOR_MODE = 0x52
    GET_BRIGHTNESS_AND_PULSE_FOR_MODE = 0x53
    SET_BRIGHTNESS_AND_PULSE_FOR_MODE = 0x54
    GET_ALARMS = 0x60
    ADD_ALARM = 0x61
    DELETE_ALARM_BY_TIME = 0x62
    DELETE_ALARM_BY_ID = 0x63
    MODIFY_ALARM_BY_ID = 0x64
    TURN_ON_OFF_ALARM_BY_ID = 0x65


CONTROL_STOP = b"\x00"
CONTROL_START = b"\x01"
CONTROL_RESET = b"\x02"

# On a two-channel panel (ML4) all red wavelengths share channel 0 and all
# near-infrared ones share channel 1. Six-channel panels (ML5) address
# 630/660/670 nm as 0..2 and 810/830/850 nm as 3..5.
CHANNEL_RED = 0
CHANNEL_NIR = 1
CHANNELS_RED_SEPARATE = (0, 1, 2)
CHANNELS_NIR_SEPARATE = (3, 4, 5)


def checksum(body: bytes) -> int:
    return sum(body) & 0xFF


def encode(command: Command | int, payload: bytes = b"") -> bytes:
    """Build a frame ready to be written to the command characteristic."""
    if len(payload) > 0xFFFF:
        raise ValueError("payload too long")
    body = bytes([ADDRESS_BROADCAST, int(command), (len(payload) >> 8) & 0xFF, len(payload) & 0xFF]) + payload
    return bytes([HEADER_WRITE]) + body + bytes([checksum(body), FOOTER])


@dataclass(frozen=True)
class Frame:
    header: int
    address: int
    command: int
    payload: bytes

    @property
    def command_name(self) -> str:
        try:
            return Command(self.command).name
        except ValueError:
            return f"UNKNOWN_0x{self.command:02X}"

    def __str__(self) -> str:
        return f"{self.command_name} {self.payload.hex(' ')}"


def decode(buffer: bytes) -> tuple[list[Frame], bytes]:
    """Pull complete frames out of a stream, returning them and the leftover bytes.

    Notifications can split or coalesce frames, so the caller keeps the remainder
    and feeds it back in with the next chunk.
    """
    frames: list[Frame] = []
    offset = 0

    while len(buffer) - offset >= OVERHEAD:
        if buffer[offset] not in (HEADER_READ, HEADER_WRITE):
            offset += 1
            continue

        length = (buffer[offset + 3] << 8) | buffer[offset + 4]
        end = offset + OVERHEAD + length
        if end > len(buffer):
            break

        frame = buffer[offset:end]
        if frame[-1] != FOOTER or frame[-2] != checksum(frame[1:-2]):
            offset += 1
            continue

        frames.append(Frame(frame[0], frame[1], frame[2], bytes(frame[5:-2])))
        offset = end

    return frames, bytes(buffer[offset:])


@dataclass(frozen=True)
class DeviceStatus:
    running: bool
    countdown_seconds: int
    remaining_seconds: int

    @classmethod
    def parse(cls, payload: bytes) -> DeviceStatus | None:
        if len(payload) != 5:
            return None
        return cls(
            running=payload[0] == 1,
            countdown_seconds=(payload[1] << 8) | payload[2],
            remaining_seconds=(payload[3] << 8) | payload[4],
        )

    def __str__(self) -> str:
        state = "running" if self.running else "idle"
        return (
            f"{state}, countdown {self.countdown_seconds // 60} min, "
            f"remaining {self.remaining_seconds // 60}:{self.remaining_seconds % 60:02d}"
        )


def brightness_payload(channel: int, brightness: int, min_brightness: int = 1) -> bytes:
    """Zero means off; anything else is clamped up to the panel's minimum."""
    if not 0 <= brightness <= 100:
        raise ValueError("brightness must be 0..100")
    level = 0 if brightness == 0 else max(brightness, min_brightness)
    return bytes([channel, level])


def countdown_payload(seconds: int) -> bytes:
    if not 0 <= seconds <= 0xFFFF:
        raise ValueError("countdown out of range")
    return bytes([(seconds >> 8) & 0xFF, seconds & 0xFF])


def system_time_payload(hour: int, minute: int, second: int) -> bytes:
    return bytes([hour, minute, second])
