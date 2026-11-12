"""Command line control for a Mito Light panel.

    python tools/mito.py scan
    python tools/mito.py info
    python tools/mito.py status
    python tools/mito.py on --red 50 --nir 50 --minutes 10
    python tools/mito.py off
    python tools/mito.py watch
"""

from __future__ import annotations

import argparse
import asyncio
import logging
import os
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from bleak.exc import BleakDeviceNotFoundError

from mitolight import MitoLight
from mitolight.protocol import Command

# Find yours with `python tools/mito.py scan`.
DEFAULT_ADDRESS = os.environ.get("MITOLIGHT_ADDRESS")


async def cmd_scan(args) -> None:
    print(f"Scanning {args.timeout:.0f}s for panels ...")
    for address, name in await MitoLight.discover(args.timeout):
        print(f"  {address}  {name}")


async def cmd_info(args) -> None:
    async with MitoLight(args.address) as panel:
        print("authenticated")
        version = await panel.protocol_version()
        print(f"protocol version : {version.payload.hex(' ')}")
        info = await panel.device_information()
        printable = "".join(chr(b) if 32 <= b < 127 else "." for b in info.payload)
        print(f"device info      : {info.payload.hex(' ')}   {printable!r}")
        print(f"status           : {await panel.status()}")


async def cmd_status(args) -> None:
    async with MitoLight(args.address) as panel:
        print(await panel.status())


async def cmd_on(args) -> None:
    async with MitoLight(args.address) as panel:
        await panel.run(red=args.red, nir=args.nir, minutes=args.minutes)
        await asyncio.sleep(0.5)
        print(await panel.status())


async def cmd_off(args) -> None:
    async with MitoLight(args.address) as panel:
        await panel.stop()
        await asyncio.sleep(0.5)
        print(await panel.status())


async def cmd_watch(args) -> None:
    async with MitoLight(args.address) as panel:
        panel.add_listener(lambda frame: print(f"  {frame}"))
        print(f"Listening {args.seconds:.0f}s; use the panel's touchscreen or the app.")
        await asyncio.sleep(args.seconds)


async def cmd_raw(args) -> None:
    async with MitoLight(args.address) as panel:
        command = Command[args.command.upper()]
        payload = bytes.fromhex(args.payload.replace(" ", "")) if args.payload else b""
        frame = await panel.request(command, payload)
        print(frame)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("-a", "--address", default=DEFAULT_ADDRESS, help="panel address, or set MITOLIGHT_ADDRESS")
    parser.add_argument("-v", "--verbose", action="store_true", help="log every frame")
    sub = parser.add_subparsers(dest="cmd", required=True)

    scan = sub.add_parser("scan", help="find panels in range")
    scan.add_argument("-t", "--timeout", type=float, default=15.0)
    scan.set_defaults(func=cmd_scan)

    sub.add_parser("info", help="protocol version, device info and status").set_defaults(func=cmd_info)
    sub.add_parser("status", help="current session state").set_defaults(func=cmd_status)

    on = sub.add_parser("on", help="set brightness and start a session")
    on.add_argument("--red", type=int, default=50, help="red channel, 0-100 %%")
    on.add_argument("--nir", type=int, default=50, help="near-infrared channel, 0-100 %%")
    on.add_argument("--minutes", type=int, default=10, help="countdown, 0 means until stopped")
    on.set_defaults(func=cmd_on)

    sub.add_parser("off", help="stop the session").set_defaults(func=cmd_off)

    watch = sub.add_parser("watch", help="print frames the panel sends")
    watch.add_argument("-s", "--seconds", type=float, default=60.0)
    watch.set_defaults(func=cmd_watch)

    raw = sub.add_parser("raw", help="send one command by name")
    raw.add_argument("command", help=", ".join(c.name for c in Command))
    raw.add_argument("payload", nargs="?", default="", help="hex payload")
    raw.set_defaults(func=cmd_raw)

    args = parser.parse_args()
    if args.func is not cmd_scan and not args.address:
        parser.error("give the panel address with --address or MITOLIGHT_ADDRESS (find it with `scan`)")
    # Scope verbosity to our own frames; bleak's own debug log drowns everything out.
    logging.basicConfig(level=logging.WARNING, format="%(message)s")
    if args.verbose:
        logging.getLogger("mitolight").setLevel(logging.DEBUG)

    try:
        asyncio.run(args.func(args))
    except BleakDeviceNotFoundError:
        print(
            f"Panel {args.address} is not reachable.\n"
            "It stops advertising while something else holds the connection, so close the\n"
            "vendor app and turn Bluetooth off on the phone, then try again.",
            file=sys.stderr,
        )
        raise SystemExit(1)


if __name__ == "__main__":
    main()
