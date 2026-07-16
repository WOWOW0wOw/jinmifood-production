#!/usr/bin/env python3
"""Rotate the production bootstrap admin password without logging it."""

from __future__ import annotations

import os
from pathlib import Path
import secrets
import sys


def main() -> int:
    env_path = Path(sys.argv[1] if len(sys.argv) > 1 else ".env.production")
    if not env_path.is_file():
        print(f"environment file not found: {env_path}", file=sys.stderr)
        return 1

    password = secrets.token_urlsafe(36)
    replacement = f"ADMIN_PASSWORD='{password}'"
    lines = env_path.read_text(encoding="utf-8").splitlines()
    replaced = False
    updated: list[str] = []
    for line in lines:
        if line.startswith("ADMIN_PASSWORD="):
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

    # The caller must capture stdout directly into a password manager or clipboard.
    print(password, end="")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
