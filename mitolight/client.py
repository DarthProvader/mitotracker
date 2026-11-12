"""Async client for a Mito Light panel over Bluetooth LE."""

from __future__ import annotations

import asyncio
import datetime as dt
import logging
from typing import Callable

from bleak import BleakClient, BleakScanner

from .protocol import (
    auth_password,
    CHANNEL_NIR,
    CHANNEL_RED,
    CONTROL_RESET,
    CONTROL_START,
    CONTROL_STOP,
    NOTIFY_UUID,
    SERVICE_UUID,
    WRITE_UUID,
    Command,
    DeviceStatus,
    Frame,
    brightness_payload,
    countdown_payload,
    decode,
    encode,
    system_time_payload,
)

log = logging.getLogger(__name__)

# Room left between writes; the panel drops frames that arrive back to back.
WRITE_GAP = 0.05
RESPONSE_TIMEOUT = 3.0


class MitoLight:
    def __init__(self, address: str, min_brightness: int = 1) -> None:
        self.address = address
        self.min_brightness = min_brightness
        self._client: BleakClient | None = None
        self._buffer = b""
        self._waiters: dict[int, asyncio.Future[Frame]] = {}
        self._listeners: list[Callable[[Frame], None]] = []

    @staticmethod
    async def discover(timeout: float = 15.0) -> list[tuple[str, str]]:
        """Return (address, name) for every panel advertising the vendor service."""
        found = await BleakScanner.discover(timeout=timeout, return_adv=True)
        panels = []
        for address, (device, adv) in found.items():
            uuids = [u.lower() for u in (adv.service_uuids or [])]
            if SERVICE_UUID in uuids:
                panels.append((address, adv.local_name or device.name or "(no name)"))
        return panels

    async def __aenter__(self) -> MitoLight:
        await self.connect()
        return self

    async def __aexit__(self, *_exc) -> None:
        await self.disconnect()

    async def connect(self, authenticate: bool = True) -> None:
        self._client = BleakClient(self.address, timeout=20.0)
        await self._client.connect()
        await self._client.start_notify(NOTIFY_UUID, self._on_notify)
        if authenticate:
            await self.authenticate()

    async def disconnect(self) -> None:
        if self._client and self._client.is_connected:
            await self._client.disconnect()
        self._client = None

    def add_listener(self, callback: Callable[[Frame], None]) -> None:
        self._listeners.append(callback)

    def _on_notify(self, _sender, data: bytearray) -> None:
        self._buffer += bytes(data)
        frames, self._buffer = decode(self._buffer)
        for frame in frames:
            log.debug("<- %s", frame)
            waiter = self._waiters.pop(frame.command, None)
            if waiter and not waiter.done():
                waiter.set_result(frame)
            for listener in self._listeners:
                listener(frame)

    async def _write(self, command: Command, payload: bytes = b"") -> None:
        if self._client is None:
            raise RuntimeError("not connected")
        frame = encode(command, payload)
        log.debug("-> %s %s", command.name, frame.hex(" "))
        await self._client.write_gatt_char(WRITE_UUID, frame, response=True)
        await asyncio.sleep(WRITE_GAP)

    async def request(self, command: Command, payload: bytes = b"", timeout: float = RESPONSE_TIMEOUT) -> Frame:
        """Send a command and wait for the reply carrying the same command byte."""
        loop = asyncio.get_running_loop()
        waiter: asyncio.Future[Frame] = loop.create_future()
        self._waiters[int(command)] = waiter
        try:
            await self._write(command, payload)
            return await asyncio.wait_for(waiter, timeout)
        finally:
            self._waiters.pop(int(command), None)

    async def authenticate(self) -> Frame:
        return await self.request(Command.AUTHENTICATION, auth_password())

    async def protocol_version(self) -> Frame:
        return await self.request(Command.PROTOCOL_VERSION)

    async def device_information(self) -> Frame:
        return await self.request(Command.DEVICE_INFORMATION)

    async def status(self) -> DeviceStatus | None:
        frame = await self.request(Command.DEVICE_STATUS)
        return DeviceStatus.parse(frame.payload)

    async def sync_time(self, when: dt.datetime | None = None) -> None:
        now = when or dt.datetime.now()
        await self._write(Command.SET_SYSTEM_TIME, system_time_payload(now.hour, now.minute, now.second))

    async def set_brightness(self, channel: int, brightness: int) -> None:
        await self._write(
            Command.SET_DEVICE_CHANNEL_BRIGHTNESS,
            brightness_payload(channel, brightness, self.min_brightness),
        )

    async def set_red(self, brightness: int) -> None:
        await self.set_brightness(CHANNEL_RED, brightness)

    async def set_nir(self, brightness: int) -> None:
        await self.set_brightness(CHANNEL_NIR, brightness)

    async def set_countdown(self, minutes: int) -> None:
        await self._write(Command.SET_DEVICE_COUNTDOWN, countdown_payload(minutes * 60))

    async def start(self) -> None:
        await self._write(Command.CONTROL, CONTROL_START)

    async def stop(self) -> None:
        await self._write(Command.CONTROL, CONTROL_STOP)

    async def reset(self) -> None:
        await self._write(Command.CONTROL, CONTROL_RESET)

    async def run(self, red: int, nir: int, minutes: int) -> None:
        """Configure both channels and a countdown, then start the session."""
        await self.set_red(red)
        await self.set_nir(nir)
        await self.set_countdown(minutes)
        await self.start()
