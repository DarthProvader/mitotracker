"""Extract ATT traffic from an Android btsnoop_hci.log without needing Wireshark.

Prints every GATT write, notification and read in time order, so the app's command
frames can be read straight off the console.
"""

import argparse
import struct
from datetime import datetime, timedelta

BTSNOOP_MAGIC = b"btsnoop\x00"

# btsnoop timestamps count microseconds from year 0; this is the offset of the Unix epoch.
EPOCH_OFFSET_US = 0x00DCDDB30F2F8000

H4_ACL = 0x02
L2CAP_CID_ATT = 0x0004

ATT_OPCODES = {
    0x0A: "Read Request",
    0x0B: "Read Response",
    0x12: "Write Request",
    0x13: "Write Response",
    0x16: "Prepare Write Request",
    0x1B: "Notification",
    0x1D: "Indication",
    0x1E: "Confirmation",
    0x52: "Write Command",
}

# Opcodes that carry a payload we actually care about, as (has_handle, payload_offset).
PAYLOAD_OPCODES = {0x12: 3, 0x52: 3, 0x1B: 3, 0x1D: 3, 0x0B: 1}


def read_records(path: str):
    with open(path, "rb") as fh:
        header = fh.read(16)
        if not header.startswith(BTSNOOP_MAGIC):
            raise SystemExit(f"{path} is not a btsnoop file")
        _version, datalink = struct.unpack(">II", header[8:16])

        while True:
            record_header = fh.read(24)
            if len(record_header) < 24:
                return
            _orig_len, incl_len, flags, _drops, timestamp = struct.unpack(">IIIIq", record_header)
            data = fh.read(incl_len)
            if len(data) < incl_len:
                return
            yield timestamp, flags, datalink, data


def to_datetime(timestamp: int) -> datetime:
    return datetime(1970, 1, 1) + timedelta(microseconds=timestamp - EPOCH_OFFSET_US)


def parse_att(data: bytes) -> tuple[int, bytes] | None:
    """Return (acl_handle, att_pdu) if this H4 packet carries an ATT PDU."""
    if len(data) < 9 or data[0] != H4_ACL:
        return None

    handle_flags, acl_len = struct.unpack("<HH", data[1:5])
    acl_handle = handle_flags & 0x0FFF
    pb_flag = (handle_flags >> 12) & 0x03
    if pb_flag == 0x01:  # continuation fragment, no L2CAP header to read
        return None

    payload = data[5 : 5 + acl_len]
    if len(payload) < 4:
        return None
    l2cap_len, cid = struct.unpack("<HH", payload[:4])
    if cid != L2CAP_CID_ATT:
        return None
    return acl_handle, payload[4 : 4 + l2cap_len]


def main(path: str, only_handle: int | None, show_all: bool) -> None:
    first_time = None
    count = 0

    for timestamp, flags, _datalink, data in read_records(path):
        parsed = parse_att(data)
        if parsed is None:
            continue
        acl_handle, pdu = parsed
        if not pdu:
            continue

        opcode = pdu[0]
        name = ATT_OPCODES.get(opcode)
        if name is None and not show_all:
            continue

        when = to_datetime(timestamp)
        if first_time is None:
            first_time = when
        elapsed = (when - first_time).total_seconds()

        # Bit 0 of flags: 0 means host to controller, i.e. sent by the phone.
        direction = "<-" if flags & 0x01 else "->"

        att_handle = None
        payload = b""
        offset = PAYLOAD_OPCODES.get(opcode)
        if offset is not None:
            if offset == 3 and len(pdu) >= 3:
                att_handle = struct.unpack("<H", pdu[1:3])[0]
            payload = pdu[offset:]

        if only_handle is not None and att_handle != only_handle:
            continue

        handle_text = f"h=0x{att_handle:04x}" if att_handle is not None else "        "
        printable = "".join(chr(b) if 32 <= b < 127 else "." for b in payload)
        print(
            f"[{elapsed:8.3f}s] {direction} conn=0x{acl_handle:03x} {handle_text} "
            f"{name or f'op 0x{opcode:02x}':<18} {payload.hex(' ')}   {printable!r}"
        )
        count += 1

    print(f"\n{count} ATT packet(s).")
    if count == 0:
        print("Nothing found. Was the HCI snoop log enabled before Bluetooth was restarted?")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("path", help="path to btsnoop_hci.log")
    parser.add_argument("--handle", type=lambda s: int(s, 0), help="show only this ATT handle, e.g. 0x000f")
    parser.add_argument("--all", action="store_true", help="include unrecognised ATT opcodes")
    args = parser.parse_args()
    main(args.path, args.handle, args.all)
