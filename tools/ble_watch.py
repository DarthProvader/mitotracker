"""Continuously watch BLE advertisements and report devices as they first appear.

Useful for catching a device that only advertises in a short window, e.g. right
after a power cycle or after the phone app releases its connection.
"""

import argparse
import asyncio
import time

from bleak import BleakScanner

# Addresses already accounted for, so new arrivals stand out.
IGNORED = set()


async def main(seconds: float, min_rssi: int) -> None:
    seen: dict[str, int] = {}
    start = time.monotonic()

    def on_detect(device, adv) -> None:
        if device.address in IGNORED:
            return
        if adv.rssi < min_rssi:
            return

        elapsed = time.monotonic() - start
        previous = seen.get(device.address)
        if previous is None:
            name = adv.local_name or device.name or "(no name)"
            print(f"[{elapsed:6.1f}s] NEW  {device.address}  {adv.rssi:>4} dBm  {name}")
            for uuid in adv.service_uuids or []:
                print(f"                 service uuid : {uuid}")
            for uuid, data in (adv.service_data or {}).items():
                print(f"                 service data : {uuid} = {data.hex(' ')}")
            for company, data in (adv.manufacturer_data or {}).items():
                print(f"                 mfr data     : 0x{company:04X} = {data.hex(' ')}")
        seen[device.address] = adv.rssi

    print(f"Watching for {seconds:.0f}s (rssi >= {min_rssi} dBm). Power-cycle the panel now.\n")
    async with BleakScanner(detection_callback=on_detect):
        await asyncio.sleep(seconds)

    print(f"\nDone. {len(seen)} device(s) seen.")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("-t", "--time", type=float, default=60.0, help="watch duration in seconds")
    parser.add_argument("--min-rssi", type=int, default=-70, help="ignore weaker devices")
    parser.add_argument("--ignore", nargs="*", default=[], help="addresses to skip")
    args = parser.parse_args()
    IGNORED.update(a.upper() for a in args.ignore)
    asyncio.run(main(args.time, args.min_rssi))
