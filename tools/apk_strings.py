"""Search an APK (or any binary) for ASCII strings matching a pattern.

A dex file keeps its string pool in plain ASCII, so grepping for a UUID or a
protocol keyword often locates the relevant code before any decompiler is involved.
"""

import argparse
import re
import zipfile
from pathlib import Path

MIN_RUN = 4
ASCII_RUN = re.compile(rb"[\x20-\x7e]{%d,}" % MIN_RUN)

# Patterns worth checking on any BLE app, in rough order of usefulness.
PRESETS = {
    "uuid": r"[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}",
    "ble": r"(?i)gatt|characteristic|writeValue|notif|0000ff[a-f0-9]{2}",
    "crypto": r"(?i)AES|CBC|ECB|SecretKeySpec|Cipher\.|md5|sha1|crc",
    "protocol": r"(?i)brightness|wavelength|nir|660|850|810|830|timer|pulse|preset|command|frame|payload|checksum",
}


def iter_blobs(path: Path):
    if path.suffix.lower() in {".apk", ".zip", ".jar"}:
        with zipfile.ZipFile(path) as archive:
            for info in archive.infolist():
                if info.file_size == 0:
                    continue
                yield info.filename, archive.read(info)
    else:
        yield path.name, path.read_bytes()


def main(path: Path, pattern: str, context: int, limit: int) -> None:
    regex = re.compile(PRESETS.get(pattern, pattern).encode(), re.IGNORECASE)
    seen: set[bytes] = set()
    total = 0

    for name, blob in iter_blobs(path):
        hits = []
        for match in ASCII_RUN.finditer(blob):
            text = match.group()
            if not regex.search(text):
                continue
            if text in seen:
                continue
            seen.add(text)
            hits.append(text.decode("ascii", "replace"))
            if len(seen) >= limit:
                break

        if hits:
            print(f"\n--- {name} ---")
            for hit in hits:
                print(f"  {hit[:200]}")
            total += len(hits)
        if len(seen) >= limit:
            print(f"\n(stopped at {limit} unique strings)")
            break

    print(f"\n{total} unique matching string(s).")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("path", type=Path)
    parser.add_argument(
        "-p",
        "--pattern",
        default="uuid",
        help=f"regex, or one of the presets: {', '.join(PRESETS)}",
    )
    parser.add_argument("-c", "--context", type=int, default=0)
    parser.add_argument("-n", "--limit", type=int, default=400)
    args = parser.parse_args()
    main(args.path, args.pattern, args.context, args.limit)
