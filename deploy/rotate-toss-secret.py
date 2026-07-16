#!/usr/bin/env python3
"""Replace TOSS_SECRET_KEY from stdin without printing the secret."""

from __future__ import annotations

import os
from pathlib import Path
import re
import sys


def main() -> int:
    env_path = Path(sys.argv[1] if len(sys.argv) > 1 else ".env.production")
    secret = sys.stdin.read().strip()
    if not re.fullmatch(r"test_sk_[A-Za-z0-9]+", secret):
        print("invalid Toss test secret format", file=sys.stderr)
        return 1
    if not env_path.is_file():
        print(f"environment file not found: {env_path}", file=sys.stderr)
        return 1

    replacement = f"TOSS_SECRET_KEY='{secret}'"
    lines = env_path.read_text(encoding="utf-8").splitlines()
    updated: list[str] = []
    replaced = False
    for line in lines:
        if line.startswith("TOSS_SECRET_KEY="):
            updated.append(replacement)
            replaced = True
        else:
            updated.append(line)
    if not replaced:
        updated.append(replacement)

    temp_path = env_path.with_suffix(env_path.suffix + ".tmp")
    temp_path.write_text("\n".join(updated) + "\n", encoding="utf-8")
    os.chmod(temp_path, 0o600)
    temp_path.replace(env_path)
    os.chmod(env_path, 0o600)
    print("toss-secret-rotated")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
