# Mito Light BLE protocol

The command set and framing used by Mito Light panels over Bluetooth LE, documented for
interoperability with independent software. The two-channel (ML4) path has been verified
against hardware; the six-channel commands have not.

## Transport

| | |
|---|---|
| Service | `0000fff0-0000-1000-8000-00805f9b34fb` |
| Write characteristic | `0000fff2-...` — write **with response** |
| Notify characteristic | `0000fff1-...` — subscribe for replies |

The panel advertises only while nothing is connected to it, and it accepts a single
central at a time. Close the official app and turn off the phone's Bluetooth before
connecting from anywhere else.

## Frame

```
3A FF <cmd> <len_hi> <len_lo> <payload...> <checksum> 0A
:                                                     \n
```

- `3A` (`:`) marks a frame going to the panel, `2A` (`*`) one coming back.
- `FF` is the address; the app always broadcasts.
- Length is big-endian and counts payload bytes only. Total frame length is payload + 7.
- Checksum is the low byte of the sum of everything from the address through the last
  payload byte, i.e. `sum(frame[1:-2]) & 0xFF`.
- `0A` (`\n`) terminates the frame.

Replies use the same layout and echo the command byte, which is what lets a caller
match a response to its request. Notifications may split one frame across packets or
pack several into one, so parse from a running buffer rather than per packet.

## Commands

| Byte | Name | Payload |
|---|---|---|
| `0x00` | AUTHENTICATION | short ASCII string, see below |
| `0x01` | PROTOCOL_VERSION | none |
| `0x02` | GET_SYSTEM_TIME | none |
| `0x03` | SET_SYSTEM_TIME | `hour, minute, second` |
| `0x08` | DEVICE_INFORMATION | none |
| `0x10` | DEVICE_STATUS | none |
| `0x20` | CONTROL | `00` stop, `01` start, `02` reset |
| `0x21` | QUICK_START | unusable, see below |
| `0x30` | GET_DEVICE_COUNTDOWN | none |
| `0x31` | SET_DEVICE_COUNTDOWN | seconds, 2 bytes big-endian |
| `0x40` | GET_DEVICE_CHANNEL_PARAMETERS | `channel` |
| `0x41` | SET_DEVICE_CHANNEL_BRIGHTNESS | `channel, brightness` |
| `0x42` | SET_DEVICE_CHANNEL_PULSE | `channel, pulse_hi, pulse_lo` |
| `0x50` | SET_LIGHT_MODE | `mode` |
| `0x51` | GET_LIGHT_MODE | none |
| `0x52` | GET_CHANNELS_FOR_MODE | `mode` |
| `0x53` | GET_BRIGHTNESS_AND_PULSE_FOR_MODE | `mode` |
| `0x54` | SET_BRIGHTNESS_AND_PULSE_FOR_MODE | `00, count, {channel, brightness, pulse_hi, pulse_lo} * count` |
| `0x60` | GET_ALARMS | none |
| `0x61` | ADD_ALARM | see alarms |
| `0x62` | DELETE_ALARM_BY_TIME | |
| `0x63` | DELETE_ALARM_BY_ID | `id` |
| `0x64` | MODIFY_ALARM_BY_ID | |
| `0x65` | TURN_ON_OFF_ALARM_BY_ID | `id, enabled` |

`AUTHENTICATION` must be the first frame after connecting. Its value is deliberately not
published here; the drivers in this repository read it from configuration
(`MITOLIGHT_AUTH_PASSWORD` or `mitolight.authPassword` in `local.properties`).

`QUICK_START` exists but should not be used: it zeroes red brightness, ignores its
arguments and persists settings instead of running once.

## Channels

Channel numbering depends on the model, decided by the Bluetooth name prefix:

| Model | Channels | Layout |
|---|---|---|
| `ML4 ...`, `ML5 BULB` | 2 | `0` = all red (630/660/670 nm), `1` = all NIR (810/830/850 nm) |
| `ML5 ...` | 6 | `0,1,2` = 630/660/670 nm, `3,4,5` = 810/830/850 nm |

Brightness is a percentage, 0–100. Zero switches a channel off; any other value is
clamped up to the panel minimum, which is 1 (3 on the bulb). Six-channel panels also
expose light modes (CLASSIC, INDIVIDUAL and the four presets) and advanced alarms;
two-channel panels ignore all of that and are driven purely by `0x41`.

## Session state

`DEVICE_STATUS` replies with five bytes:

```
running(1) countdown_hi countdown_lo remaining_hi remaining_lo
```

Both times are in seconds. The app divides by 60 to show minutes. A countdown of 0
means the light stays on until stopped.

## Turning the light on

```
0x41  channel 0, brightness      # red
0x41  channel 1, brightness      # near-infrared
0x31  countdown in seconds       # 0 for indefinite
0x20  01                         # start
```

Leave a short gap between writes and never send two at once; the drivers here wait 50 ms
between frames.

## Worked examples

```
ask for status         3a ff 10 00 00 0f 0a
red to 50 %            3a ff 41 00 02 00 32 74 0a
near-infrared to 100 % 3a ff 41 00 02 01 64 a7 0a
countdown 10 minutes   3a ff 31 00 02 02 58 8c 0a
start                  3a ff 20 00 01 01 21 0a
stop                   3a ff 20 00 01 00 20 0a
```

`tests/test_protocol.py` asserts these exact byte strings.
