"""Shared helpers for Kleos ingest scripts calling the REST API."""

from __future__ import annotations

import os

INGEST_KEY_HEADER = "X-Kleos-Ingest-Key"


def ingest_api_key() -> str:
    return os.environ.get("KLEOS_INGEST_API_KEY", "").strip()


def auth_headers(extra: dict[str, str] | None = None) -> dict[str, str]:
    headers = {
        "Accept": "application/json",
        "Content-Type": "application/json",
    }
    key = ingest_api_key()
    if key:
        headers[INGEST_KEY_HEADER] = key
    if extra:
        headers.update(extra)
    return headers
