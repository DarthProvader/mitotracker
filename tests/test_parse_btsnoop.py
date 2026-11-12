r"""Self-check for the btsnoop parser using a synthetic capture file.

Run directly: .\.venv\Scripts\python.exe tests\test_parse_btsnoop.py
"""

import struct
import subprocess
import sys
import tempfile
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(REPO / "tools"))

EPOCH_OFFSET_US = 0x00DCDDB30F2F8000


def att_write_command(conn_handle: int, att_handle: int, payload: bytes) -> bytes:
    att = bytes([0x52]) + struct.pack("<H", att_handle) + payload
    l2cap = struct.pack("<HH", len(att), 0x0004) + att
    acl = struct.pack("<HH", conn_handle | (0x02 << 12), len(l2cap)) + l2cap
    return bytes([0x02]) + acl


def att_notification(conn_handle: int, att_handle: int, payload: bytes) -> bytes:
    att = bytes([0x1B]) + struct.pack("<H", att_handle) + payload
    l2cap = struct.pack("<HH", len(att), 0x0004) + att
    acl = struct.pack("<HH", conn_handle | (0x02 << 12), len(l2cap)) + l2cap
    return bytes([0x02]) + acl


def build_capture(path: Path) -> None:
    packets = [
        (0, att_write_command(0x0040, 0x000F, bytes.fromhex("7e00050301000000"))),
        (2_000_000, att_notification(0x0040, 0x0011, bytes.fromhex("aa01ff"))),
        (4_000_000, att_write_command(0x0040, 0x000F, bytes.fromhex("7e000503ff0000ef"))),
    ]

    with path.open("wb") as fh:
        fh.write(b"btsnoop\x00" + struct.pack(">II", 1, 1002))
        base = EPOCH_OFFSET_US + 1_700_000_000_000_000
        for offset, data in packets:
            # flags bit 0: 0 = sent by host, 1 = received by host
            flags = 0x01 if data[0:1] == b"\x02" and offset == 2_000_000 else 0x00
            fh.write(struct.pack(">IIIIq", len(data), len(data), flags, 0, base + offset))
            fh.write(data)


def main() -> int:
    with tempfile.TemporaryDirectory() as tmp:
        capture = Path(tmp) / "synthetic.log"
        build_capture(capture)

        result = subprocess.run(
            [sys.executable, str(REPO / "tools" / "parse_btsnoop.py"), str(capture)],
            capture_output=True,
            text=True,
        )
        print(result.stdout)
        if result.stderr:
            print(result.stderr, file=sys.stderr)

        checks = {
            "parses three ATT packets": "3 ATT packet(s)" in result.stdout,
            "decodes write commands": "Write Command" in result.stdout,
            "decodes notifications": "Notification" in result.stdout,
            "resolves the command handle": "h=0x000f" in result.stdout,
            "shows payload hex": "7e 00 05 03 ff 00 00 ef" in result.stdout,
            "marks direction": "->" in result.stdout and "<-" in result.stdout,
        }

    failed = [name for name, ok in checks.items() if not ok]
    for name, ok in checks.items():
        print(f"  {'PASS' if ok else 'FAIL'}  {name}")
    return 1 if failed else 0


if __name__ == "__main__":
    raise SystemExit(main())
