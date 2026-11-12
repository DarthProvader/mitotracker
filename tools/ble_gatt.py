"""Connect to a BLE device and dump its full GATT table."""

import argparse
import asyncio

from bleak import BleakClient

# Characteristic properties that matter when looking for a command channel.
WRITABLE = {"write", "write-without-response"}


async def main(address: str, read_values: bool) -> None:
    print(f"Connecting to {address} ...")
    async with BleakClient(address, timeout=20.0) as client:
        print(f"Connected: {client.is_connected}")
        try:
            print(f"MTU: {client.mtu_size}")
        except Exception:
            pass
        print()

        write_targets, notify_targets = [], []

        for service in client.services:
            print(f"[service] {service.uuid}  {service.description}")
            for char in service.characteristics:
                props = ",".join(char.properties)
                print(f"    [char] {char.uuid}  handle={char.handle}  ({props})")
                if char.description and char.description != "Unknown":
                    print(f"           desc: {char.description}")

                if WRITABLE & set(char.properties):
                    write_targets.append(char)
                if "notify" in char.properties or "indicate" in char.properties:
                    notify_targets.append(char)

                if read_values and "read" in char.properties:
                    try:
                        value = await client.read_gatt_char(char)
                        # Console codepages on Windows choke on arbitrary bytes.
                        printable = "".join(chr(b) if 32 <= b < 127 else "." for b in value)
                        print(f"           value: {value.hex(' ')}  ascii={printable!r}")
                    except Exception as exc:
                        print(f"           value: <read failed: {exc}>")

                for descriptor in char.descriptors:
                    print(f"        [desc] {descriptor.uuid}  handle={descriptor.handle}")
            print()

        print("=" * 70)
        print("Candidate command channels (writable):")
        for char in write_targets:
            print(f"  {char.uuid}  handle={char.handle}  ({','.join(char.properties)})")
        print("\nCandidate feedback channels (notify/indicate):")
        for char in notify_targets:
            print(f"  {char.uuid}  handle={char.handle}  ({','.join(char.properties)})")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("address", help="BLE MAC address, e.g. AA:BB:CC:DD:EE:FF")
    parser.add_argument("--no-read", action="store_true", help="skip reading characteristic values")
    args = parser.parse_args()
    asyncio.run(main(args.address, read_values=not args.no_read))
