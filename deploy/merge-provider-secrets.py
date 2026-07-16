#!/usr/bin/env python3
"""Merge provider credentials into .env.production without printing values."""

from __future__ import annotations

import argparse
import os
import re
from pathlib import Path


ALLOWED_KEYS = (
    "SOLAPI_API_KEY",
    "SOLAPI_API_SECRET",
    "SOLAPI_SENDER",
    "GOOGLE_CLIENT_ID",
    "GOOGLE_CLIENT_SECRET",
    "KAKAO_CLIENT_ID",
    "KAKAO_CLIENT_SECRET",
    "NAVER_CLIENT_ID",
    "NAVER_CLIENT_SECRET",
)
REQUIRED_KEYS = (
    "SOLAPI_API_KEY",
    "SOLAPI_API_SECRET",
    "SOLAPI_SENDER",
    "KAKAO_CLIENT_ID",
    "KAKAO_CLIENT_SECRET",
    "NAVER_CLIENT_ID",
    "NAVER_CLIENT_SECRET",
)
PAIR_PREFIXES = ("GOOGLE", "KAKAO", "NAVER")
SENDER_PATTERN = re.compile(r"01[016789]\d{7,8}")


def parse_env(path: Path, *, restrict_keys: bool) -> dict[str, str]:
    values: dict[str, str] = {}
    for number, raw_line in enumerate(path.read_text(encoding="utf-8-sig").splitlines(), 1):
        line = raw_line.strip()
        if not line or line.startswith("#"):
            continue
        if "=" not in line:
            raise ValueError(f"Invalid environment line {number}")
        key, value = line.split("=", 1)
        key = key.strip()
        value = value.strip()
        if restrict_keys and key not in ALLOWED_KEYS:
            raise ValueError(f"Unsupported key at line {number}: {key}")
        if key in values:
            raise ValueError(f"Duplicate key at line {number}: {key}")
        if not value or any(character in value for character in "\r\n\0"):
            raise ValueError(f"Empty or invalid value at line {number}: {key}")
        values[key] = value
    return values


def validate_provider_values(values: dict[str, str]) -> None:
    missing = [key for key in REQUIRED_KEYS if not values.get(key)]
    if missing:
        raise ValueError("Missing required keys: " + ", ".join(missing))

    for prefix in PAIR_PREFIXES:
        client_id = bool(values.get(f"{prefix}_CLIENT_ID"))
        client_secret = bool(values.get(f"{prefix}_CLIENT_SECRET"))
        if client_id != client_secret:
            raise ValueError(f"{prefix} client ID and secret must be configured together")

    sender = values["SOLAPI_SENDER"].replace("-", "")
    if not SENDER_PATTERN.fullmatch(sender):
        raise ValueError("SOLAPI_SENDER must be a valid Korean mobile number")
    values["SOLAPI_SENDER"] = sender


def merge_lines(existing_text: str, replacements: dict[str, str]) -> str:
    result: list[str] = []
    written: set[str] = set()
    for raw_line in existing_text.splitlines():
        stripped = raw_line.strip()
        if stripped and not stripped.startswith("#") and "=" in stripped:
            key = stripped.split("=", 1)[0].strip()
            if key in replacements:
                if key not in written:
                    result.append(f"{key}={replacements[key]}")
                    written.add(key)
                continue
        result.append(raw_line)

    for key, value in replacements.items():
        if key not in written:
            result.append(f"{key}={value}")
    return "\n".join(result).rstrip() + "\n"


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=Path)
    parser.add_argument("target", type=Path)
    args = parser.parse_args()

    if not args.source.is_file():
        raise SystemExit("Provider credential source file does not exist")
    if not args.target.is_file():
        raise SystemExit("Target environment file does not exist")

    provider_values = parse_env(args.source, restrict_keys=True)
    validate_provider_values(provider_values)

    replacements = {"SMS_MODE": "solapi", **provider_values}
    merged = merge_lines(args.target.read_text(encoding="utf-8"), replacements)
    temporary = args.target.with_suffix(args.target.suffix + ".tmp")
    temporary.write_text(merged, encoding="utf-8", newline="\n")
    os.chmod(temporary, 0o600)
    os.replace(temporary, args.target)
    os.chmod(args.target, 0o600)
    args.source.unlink()

    updated = ", ".join(replacements)
    print(f"Updated provider configuration: {updated}")
    print("Temporary credential file removed")


if __name__ == "__main__":
    main()
