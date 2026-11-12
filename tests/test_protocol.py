r"""Check frame encoding against vectors derived from the vendor app's source.

Run directly: .\.venv\Scripts\python.exe tests\test_protocol.py
"""

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from mitolight.protocol import (
    Command,
    DeviceStatus,
    brightness_payload,
    countdown_payload,
    decode,
    encode,
)

VECTORS = [
    ("authenticate (placeholder value)", Command.AUTHENTICATION, b"test", "3a ff 00 00 04 74 65 73 74 c3 0a"),
    ("start", Command.CONTROL, b"\x01", "3a ff 20 00 01 01 21 0a"),
    ("stop", Command.CONTROL, b"\x00", "3a ff 20 00 01 00 20 0a"),
    ("status query", Command.DEVICE_STATUS, b"", "3a ff 10 00 00 0f 0a"),
    ("red 50%", Command.SET_DEVICE_CHANNEL_BRIGHTNESS, brightness_payload(0, 50), "3a ff 41 00 02 00 32 74 0a"),
    ("nir 100%", Command.SET_DEVICE_CHANNEL_BRIGHTNESS, brightness_payload(1, 100), "3a ff 41 00 02 01 64 a7 0a"),
    ("countdown 10 min", Command.SET_DEVICE_COUNTDOWN, countdown_payload(600), "3a ff 31 00 02 02 58 8c 0a"),
]


def check(name: str, condition: bool) -> bool:
    print(f"  {'PASS' if condition else 'FAIL'}  {name}")
    return condition


def main() -> int:
    results = []

    for name, command, payload, expected in VECTORS:
        actual = encode(command, payload).hex(" ")
        ok = actual == expected
        results.append(check(f"encode {name}", ok))
        if not ok:
            print(f"        expected {expected}")
            print(f"        actual   {actual}")

    # A response uses the same layout with '*' as the header.
    response = bytes.fromhex("2aff100005010258023a") + bytes([0]) + b"\n"
    body = response[1:-2]
    response = response[:-2] + bytes([sum(body) & 0xFF, 0x0A])
    frames, rest = decode(response)
    results.append(check("decode a single response frame", len(frames) == 1 and rest == b""))

    if frames:
        status = DeviceStatus.parse(frames[0].payload)
        results.append(check("parse device status", status is not None and status.running))
        results.append(
            check(
                "status countdown 10 min, remaining 9:14",
                status is not None and status.countdown_seconds == 600 and status.remaining_seconds == 570,
            )
        )

    # Two frames arriving in one notification, plus a truncated third.
    stream = encode(Command.CONTROL, b"\x01") + encode(Command.DEVICE_STATUS) + b"\x3a\xff\x10"
    frames, rest = decode(stream)
    results.append(check("split a coalesced stream and keep the remainder", len(frames) == 2 and len(rest) == 3))

    corrupted = bytearray(encode(Command.CONTROL, b"\x01"))
    corrupted[-2] ^= 0xFF
    frames, _ = decode(bytes(corrupted))
    results.append(check("reject a bad checksum", frames == []))

    return 0 if all(results) else 1


if __name__ == "__main__":
    raise SystemExit(main())
