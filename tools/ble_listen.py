"""Subscribe to the panel's notify characteristic and optionally send raw frames.

Everything that arrives is timestamped and printed as hex, which is what we need
to correlate our writes with the panel's responses.
"""

import argparse
import asyncio
import os
import time

from bleak import BleakClient

PANEL_ADDRESS = os.environ.get("MITOLIGHT_ADDRESS")
CHAR_WRITE = "0000fff2-0000-1000-8000-00805f9b34fb"
CHAR_NOTIFY = "0000fff1-0000-1000-8000-00805f9b34fb"


def parse_frame(text: str) -> bytes:
    """Accept '7e 00 05' or '7E000503' or '0x7e,0x00'."""
    cleaned = text.replace("0x", "").replace(",", " ").replace("-", " ")
    parts = cleaned.split()
    if len(parts) == 1:
        return bytes.fromhex(parts[0])
    return bytes(int(p, 16) for p in parts)


async def main(address: str, seconds: float, frames: list[str], response: bool) -> None:
    start = time.monotonic()

    def on_notify(_sender, data: bytearray) -> None:
        elapsed = time.monotonic() - start
        printable = "".join(chr(b) if 32 <= b < 127 else "." for b in data)
        print(f"[{elapsed:6.2f}s] <- {data.hex(' ')}   {printable!r}")

    async with BleakClient(address, timeout=20.0) as client:
        print(f"Connected to {address}, mtu={client.mtu_size}")
        await client.start_notify(CHAR_NOTIFY, on_notify)
        print(f"Subscribed to {CHAR_NOTIFY}\n")

        for text in frames:
            frame = parse_frame(text)
            elapsed = time.monotonic() - start
            print(f"[{elapsed:6.2f}s] -> {frame.hex(' ')}")
            await client.write_gatt_char(CHAR_WRITE, frame, response=response)
            await asyncio.sleep(1.0)

        await asyncio.sleep(seconds)
        await client.stop_notify(CHAR_NOTIFY)

    print("\nDisconnected.")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("-a", "--address", default=PANEL_ADDRESS, required=PANEL_ADDRESS is None)
    parser.add_argument("-t", "--time", type=float, default=20.0, help="listen duration after sending")
    parser.add_argument("-s", "--send", action="append", default=[], help="hex frame to write, repeatable")
    parser.add_argument("--with-response", action="store_true", help="use write-with-response")
    args = parser.parse_args()
    asyncio.run(main(args.address, args.time, args.send, args.with_response))
