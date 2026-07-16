#!/usr/bin/env python3
"""Merge a downloaded Google OAuth web-client JSON into the local key file."""

from __future__ import annotations

import argparse
import json
from pathlib import Path


KEYS = ("GOOGLE_CLIENT_ID", "GOOGLE_CLIENT_SECRET")
EXPECTED_REDIRECT = "https://jinmifood.com/login/oauth2/code/google"


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=Path)
    parser.add_argument("target", type=Path)
    parser.add_argument("--delete-source", action="store_true")
    args = parser.parse_args()

    payload = json.loads(args.source.read_text(encoding="utf-8"))
    web = payload.get("web")
    if not isinstance(web, dict):
        raise SystemExit("OAuth JSON does not contain a web client")
    client_id = str(web.get("client_id", "")).strip()
    client_secret = str(web.get("client_secret", "")).strip()
    redirects = web.get("redirect_uris", [])
    if not client_id or not client_secret:
        raise SystemExit("OAuth JSON is missing the client ID or secret")
    if EXPECTED_REDIRECT not in redirects:
        raise SystemExit("OAuth JSON does not contain the Jinmifood redirect URI")

    replacements = {"GOOGLE_CLIENT_ID": client_id, "GOOGLE_CLIENT_SECRET": client_secret}
    existing = args.target.read_text(encoding="utf-8-sig") if args.target.exists() else ""
    result: list[str] = []
    written: set[str] = set()
    for raw_line in existing.splitlines():
        key = raw_line.split("=", 1)[0].strip() if "=" in raw_line else ""
        if key in replacements:
            if key not in written:
                result.append(f"{key}={replacements[key]}")
                written.add(key)
            continue
        result.append(raw_line)
    for key in KEYS:
        if key not in written:
            result.append(f"{key}={replacements[key]}")
    args.target.write_text("\n".join(result).rstrip() + "\n", encoding="utf-8", newline="\n")
    if args.delete_source:
        args.source.unlink()
    print("Google OAuth credentials merged without displaying values")
    print("Downloaded credential JSON removed" if args.delete_source else "Downloaded credential JSON retained")


if __name__ == "__main__":
    main()
