"""Scan for BLE advertisements and flag devices that look like an LED controller."""

import argparse
import asyncio

from bleak import BleakScanner

# Advertised service UUIDs that identify a known BLE stack, which tells us
# up front how much protocol work to expect.
KNOWN_STACKS = {
    "0000fee7": "Telink mesh (AES, often default keys telink_mesh1/123)",
    "0000fee0": "Telink / Anhui Huami",
    "0000a201": "Tuya BLE (AES-128, session key from cloud)",
    "0000fd50": "Tuya Smart",
    "6e400001": "Nordic UART Service (usually plaintext framing)",
    "0000ffe0": "HM-10 / JDY-08 transparent serial (plaintext)",
    "0000ffd5": "Triones / Happy Lighting LED (plaintext)",
    "0000fff0": "Generic vendor serial (often plaintext)",
    "00001523": "nRF LED Button Service",
    "0000ff00": "Generic vendor service",
}

NAME_HINTS = ("mito", "light", "led", "lamp", "rgb", "panel", "bt", "ble", "elk", "triones")


def classify(name: str, uuids: list[str]) -> list[str]:
    hits = []
    for uuid in uuids:
        prefix = uuid.lower()[:8]
        if prefix in KNOWN_STACKS:
            hits.append(f"{uuid} -> {KNOWN_STACKS[prefix]}")
    lowered = (name or "").lower()
    if any(hint in lowered for hint in NAME_HINTS):
        hits.append(f"name hint: {name}")
    return hits


async def main(seconds: float) -> None:
    print(f"Scanning for {seconds:.0f}s ...\n")
    found = await BleakScanner.discover(timeout=seconds, return_adv=True)

    interesting = []
    for address, (device, adv) in sorted(found.items(), key=lambda kv: -(kv[1][1].rssi or -999)):
        name = adv.local_name or device.name or "(no name)"
        print(f"{address}  rssi={adv.rssi:>4} dBm  {name}")

        if adv.service_uuids:
            for uuid in adv.service_uuids:
                print(f"    service uuid : {uuid}")
        for uuid, data in (adv.service_data or {}).items():
            print(f"    service data : {uuid} = {data.hex(' ')}")
        for company, data in (adv.manufacturer_data or {}).items():
            print(f"    mfr data     : 0x{company:04X} = {data.hex(' ')}")
        if adv.tx_power is not None:
            print(f"    tx power     : {adv.tx_power}")

        hits = classify(name, list(adv.service_uuids or []))
        if hits:
            interesting.append((address, name, adv.rssi, hits))
        print()

    print("=" * 70)
    if interesting:
        print("Likely candidates:\n")
        for address, name, rssi, hits in interesting:
            print(f"  {address}  {name}  ({rssi} dBm)")
            for hit in hits:
                print(f"      {hit}")
    else:
        print("No obvious candidate. The panel may advertise with a generic name;")
        print("look for the strongest RSSI, or move the PC next to the panel and rescan.")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("-t", "--time", type=float, default=12.0, help="scan duration in seconds")
    asyncio.run(main(parser.parse_args().time))
