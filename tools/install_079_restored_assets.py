from __future__ import annotations

import base64
import io
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PARTS = ROOT / "tools/restored_079_assets"
TARGET = ROOT / "build/generated/metatech_assets"


def main() -> None:
    encoded = "".join(
        path.read_text(encoding="ascii").strip()
        for path in sorted(PARTS.glob("part*.txt"))
    )
    archive_bytes = base64.b85decode(encoded.encode("ascii"))
    with zipfile.ZipFile(io.BytesIO(archive_bytes)) as archive:
        bad = archive.testzip()
        if bad is not None:
            raise RuntimeError(f"Corrupted restored asset: {bad}")
        archive.extractall(TARGET)
        count = len([name for name in archive.namelist() if name.endswith(".png")])
    print(f"Restored {count} approved 0.6.79 texture and GUI resources")


if __name__ == "__main__":
    main()
