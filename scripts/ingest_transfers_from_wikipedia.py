#!/usr/bin/env python3
"""Ingest latest transfer-window deals from Wikipedia list pages.

Why Wikipedia (not Transfermarkt): Kleos policy forbids Transfermarkt scrapes.
English Wikipedia maintains dated, cited summer/winter transfer tables for major
leagues (CC BY-SA). We store a product-shaped Transfer subset with
source=wikipedia:<page title>.

Primary use: populate the upcoming season (e.g. 2026/27) with real move dates so
the Transfers tab can project current-window signings — not only PlayerSeason
diffs dated 1 July from prior campaigns.

Examples:
  ./scripts/ingest_transfers_from_wikipedia.py --dry-run --pages en-summer-2026
  ./scripts/ingest_transfers_from_wikipedia.py --season 2026/27 --sleep 1.2
"""

from __future__ import annotations

import argparse
import json
import os
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from dataclasses import dataclass
from datetime import datetime
from typing import Any

UA = "KleosTransfersBot/0.1 (research; https://github.com/Ysetia10/kleos-transfers)"
API_DEFAULT = os.environ.get("API_URL", "http://localhost:8080")

# Named page packs → MediaWiki titles
PAGE_PACKS: dict[str, list[str]] = {
    "en-summer-2026": ["List of English football transfers summer 2026"],
    "en-summer-2025": ["List of English football transfers summer 2025"],
    "top5-summer-2026": [
        "List of English football transfers summer 2026",
        "List of Spanish football transfers summer 2026",
        "List of German football transfers summer 2026",
        "List of Italian football transfers summer 2026",
        "List of French football transfers summer 2026",
    ],
}

MONTHS = {
    "january": 1,
    "february": 2,
    "march": 3,
    "april": 4,
    "may": 5,
    "june": 6,
    "july": 7,
    "august": 8,
    "september": 9,
    "october": 10,
    "november": 11,
    "december": 12,
}

CLUB_ALIASES = {
    "wolverhampton wanderers": "Wolverhampton",
    "tottenham hotspur": "Tottenham",
    "brighton & hove albion": "Brighton",
    "brighton and hove albion": "Brighton",
    "nottingham forest": "Nott'ham Forest",
    "manchester united": "Manchester Utd",
    "manchester city": "Manchester City",
    "west ham united": "West Ham",
    "newcastle united": "Newcastle Utd",
    "leicester city": "Leicester City",
    "leeds united": "Leeds United",
    "afc bournemouth": "Bournemouth",
    "bournemouth": "Bournemouth",
    "athletic bilbao": "Athletic Club",
    "athletic club": "Athletic Club",
    "atletico madrid": "Atlético Madrid",
    "atlético madrid": "Atlético Madrid",
    "inter milan": "Inter",
    "internazionale": "Inter",
    "ac milan": "Milan",
    "bayern munich": "Bayern Munich",
    "rb leipzig": "RB Leipzig",
    "paris saint-germain": "Paris S-G",
    "paris saint germain": "Paris S-G",
    "psg": "Paris S-G",
}


@dataclass
class RawTransfer:
    date: str  # YYYY-MM-DD
    player: str
    from_club: str | None
    to_club: str | None
    fee_raw: str
    transfer_type: str  # PERMANENT | LOAN | FREE
    page_title: str


def http_json(method: str, url: str, body: dict | None = None) -> Any:
    data = None if body is None else json.dumps(body).encode("utf-8")
    req = urllib.request.Request(
        url,
        data=data,
        method=method,
        headers={
            "Accept": "application/json",
            "Content-Type": "application/json",
            "User-Agent": UA,
        },
    )
    with urllib.request.urlopen(req, timeout=60) as resp:
        raw = resp.read().decode("utf-8")
        return json.loads(raw) if raw else None


def wiki_wikitext(title: str) -> str:
    url = "https://en.wikipedia.org/w/api.php?" + urllib.parse.urlencode(
        {"action": "parse", "page": title, "prop": "wikitext", "format": "json"}
    )
    req = urllib.request.Request(url, headers={"User-Agent": UA, "Accept": "application/json"})
    with urllib.request.urlopen(req, timeout=90) as resp:
        payload = json.loads(resp.read().decode("utf-8"))
    return payload["parse"]["wikitext"]["*"]


def strip_wiki(cell: str) -> str:
    text = cell
    text = re.sub(r"<ref[^>]*>.*?</ref>", "", text, flags=re.S | re.I)
    text = re.sub(r"<ref[^/]*/>", "", text, flags=re.I)
    text = re.sub(r"\{\{ntsh\|[^}]*\}\}", "", text, flags=re.I)
    text = re.sub(r"\{\{flagg?\|[^}]*\}\}", "", text, flags=re.I)
    text = re.sub(r"\{\{fla\|[^}]*\}\}", "", text, flags=re.I)
    # {{sortname|First|Last|dab=...}}
    text = re.sub(
        r"\{\{sortname\|([^}|]+)\|([^}|]+)(?:\|[^}]*)?\}\}",
        r"\1 \2",
        text,
        flags=re.I,
    )
    # [[Link|Display]] or [[Link]]
    text = re.sub(r"\[\[(?:[^|\]]*\|)?([^\]]+)\]\]", r"\1", text)
    text = re.sub(r"\{\{[^}]+\}\}", "", text)
    text = re.sub(r"'{2,}", "", text)
    text = re.sub(r"<[^>]+>", "", text)
    text = text.replace("&nbsp;", " ").replace("\xa0", " ")
    return re.sub(r"\s+", " ", text).strip()


def parse_date(cell: str) -> str | None:
    text = strip_wiki(cell)
    # rowspan leftovers / empty
    if not text or text.lower() in {"", "—", "-"}:
        return None
    m = re.match(
        r"(?P<d>\d{1,2})\s+(?P<m>[A-Za-z]+)\s+(?P<y>\d{4})",
        text,
    )
    if not m:
        return None
    month = MONTHS.get(m.group("m").lower())
    if not month:
        return None
    return f"{int(m.group('y')):04d}-{month:02d}-{int(m.group('d')):02d}"


def parse_fee(cell: str) -> tuple[str, float | None]:
    text = strip_wiki(cell)
    lower = text.lower()
    if not text or "undisclosed" in lower or text in {"—", "-"}:
        return "PERMANENT", None
    if lower in {"free", "free transfer"} or "free" == lower:
        return "FREE", 0.0
    # £12.6m / €25m / $10m
    m = re.search(r"([£€$])\s*([\d.,]+)\s*([mb]|million|bn)?", text, re.I)
    if not m:
        return "PERMANENT", None
    amount = float(m.group(2).replace(",", ""))
    unit = (m.group(3) or "").lower()
    if unit in {"m", "million"}:
        amount *= 1_000_000
    elif unit in {"b", "bn"}:
        amount *= 1_000_000_000
    # Store as EUR-ish; Wikipedia mixes currencies — keep numeric magnitude only.
    return "PERMANENT", amount


def club_name(cell: str) -> str | None:
    text = strip_wiki(cell)
    if not text or text.lower() in {"unattached", "free agent", "n/a", "—", "-"}:
        return None
    text = re.sub(r"\s+F\.?C\.?$", "", text, flags=re.I)
    text = re.sub(r"\s+A\.?F\.?C\.?$", "", text, flags=re.I)
    return text.strip() or None


def split_table_rows(wikitext: str) -> list[list[str]]:
    """Split first permanent-transfer wikitable into rows of cells."""
    # Prefer Transfers section before Loans
    section = wikitext
    if "== Transfers ==" in wikitext:
        section = wikitext.split("== Transfers ==", 1)[1]
    if "== Loans ==" in section:
        section = section.split("== Loans ==", 1)[0]

    start = section.find('{| class="wikitable')
    if start < 0:
        start = section.find("{|")
    if start < 0:
        return []
    end = section.find("|}", start)
    table = section[start : end if end > start else None]
    rows_raw = re.split(r"\n\|-", table)
    rows: list[list[str]] = []
    for chunk in rows_raw[1:]:  # skip header chunk
        cells: list[str] = []
        for line in chunk.splitlines():
            line = line.strip()
            if not line.startswith("|") or line.startswith("|+"):
                continue
            if line.startswith("|-" ):
                continue
            # |value or |rowspan=2|value
            cell = re.sub(r"^\|(?:rowspan=\"?\d+\"?\|)?", "", line, count=1, flags=re.I)
            if cell.startswith("!"):
                continue
            cells.append(cell)
        if cells:
            rows.append(cells)
    return rows


def parse_permanent_transfers(page_title: str, wikitext: str) -> list[RawTransfer]:
    out: list[RawTransfer] = []
    last_date: str | None = None
    for cells in split_table_rows(wikitext):
        # Expected: Date | Player | From | To | Fee  (some rows omit date via rowspan)
        if len(cells) < 4:
            continue
        if len(cells) >= 5:
            date_cell, player_cell, from_cell, to_cell, fee_cell = cells[:5]
            parsed = parse_date(date_cell)
            if parsed:
                last_date = parsed
            elif not last_date:
                continue
            date = last_date
        else:
            # Date omitted (rowspan) → Player From To Fee
            if not last_date:
                continue
            date = last_date
            player_cell, from_cell, to_cell, fee_cell = cells[:4]

        player = strip_wiki(player_cell)
        if not player or player.lower() in {"player", "name"}:
            continue
        from_club = club_name(from_cell)
        to_club = club_name(to_cell)
        kind, _fee = parse_fee(fee_cell)
        # Keep fee string parse separately for payload
        _kind2, fee_eur = parse_fee(fee_cell)
        out.append(
            RawTransfer(
                date=date,
                player=player,
                from_club=from_club,
                to_club=to_club,
                fee_raw=strip_wiki(fee_cell),
                transfer_type=kind if fee_eur != 0.0 or kind == "FREE" else "PERMANENT",
                page_title=page_title,
            )
        )
        # attach fee on object via monkey — store in fee_raw parse later
        setattr(out[-1], "fee_eur", fee_eur)
    return out


def normalize_club_query(name: str) -> str:
    key = name.lower().strip()
    key = key.replace("ü", "u").replace("ö", "o").replace("ä", "a")
    return CLUB_ALIASES.get(key, name)


def resolve_season_id(api: str, label: str) -> str:
    page = 0
    while True:
        payload = http_json(
            "GET",
            f"{api.rstrip('/')}/api/v1/seasons?page={page}&size=50&sort=startDate,desc",
        )
        for row in payload.get("content") or []:
            if row["label"] == label:
                return row["id"]
        if payload.get("last", True):
            break
        page += 1
    raise SystemExit(f"Season {label!r} not found — run scripts/ensure_predict_seasons.py")


def fetch_all(api: str, path: str, size: int = 200) -> list[dict[str, Any]]:
    page = 0
    rows: list[dict[str, Any]] = []
    while True:
        q = urllib.parse.urlencode({"page": page, "size": size, "sort": "id,asc"})
        payload = http_json("GET", f"{api.rstrip('/')}{path}?{q}")
        content = payload.get("content") or []
        rows.extend(content)
        if payload.get("last", True) or not content:
            break
        page += 1
    return rows


def build_player_index(api: str) -> dict[str, str]:
    index: dict[str, str] = {}
    last_name_counts: dict[str, int] = {}
    last_name_ids: dict[str, str] = {}
    for player in fetch_all(api, "/api/v1/players", size=500):
        full = player["fullName"].lower().strip()
        index[full] = player["id"]
        parts = full.split()
        if len(parts) >= 2:
            ln = parts[-1]
            last_name_counts[ln] = last_name_counts.get(ln, 0) + 1
            last_name_ids[ln] = player["id"]
    for ln, count in last_name_counts.items():
        if count == 1:
            index[f"ln:{ln}"] = last_name_ids[ln]
    return index


def build_club_index(api: str) -> dict[str, str]:
    index: dict[str, str] = {}
    for club in fetch_all(api, "/api/v1/clubs", size=200):
        name = club["name"].lower().strip()
        index[name] = club["id"]
        index[re.sub(r"[^a-z0-9]+", "", name)] = club["id"]
    for alias, canonical in CLUB_ALIASES.items():
        cid = index.get(canonical.lower())
        if cid:
            index[alias] = cid
            index[re.sub(r"[^a-z0-9]+", "", alias)] = cid
    return index


def search_player(name: str, index: dict[str, str]) -> str | None:
    key = name.lower().strip()
    if key in index:
        return index[key]
    parts = key.split()
    if len(parts) >= 2:
        ln = index.get(f"ln:{parts[-1]}")
        if ln:
            return ln
    # substring contains
    for full, pid in index.items():
        if full.startswith("ln"):
            continue
        if key in full or full in key:
            return pid
    return None


def search_club(name: str | None, index: dict[str, str]) -> str | None:
    if not name:
        return None
    query = normalize_club_query(name).lower().strip()
    if query in index:
        return index[query]
    compact = re.sub(r"[^a-z0-9]+", "", query)
    if compact in index:
        return index[compact]
    for key, cid in index.items():
        if key.startswith("ln"):
            continue
        if query in key or key in query:
            return cid
    return None


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--api-url", default=API_DEFAULT)
    parser.add_argument("--season", default="2026/27", help="Kleos season label to attach")
    parser.add_argument(
        "--pages",
        default="en-summer-2026",
        help=f"Page pack key or comma-separated Wikipedia titles. Packs: {', '.join(PAGE_PACKS)}",
    )
    parser.add_argument("--sleep", type=float, default=1.0, help="Delay between wiki fetches")
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--limit", type=int, default=0)
    parser.add_argument(
        "--require-matched-club",
        action=argparse.BooleanOptionalAction,
        default=True,
        help="Skip rows where neither from nor to club matches Kleos (default: on)",
    )
    args = parser.parse_args()

    if args.pages in PAGE_PACKS:
        titles = PAGE_PACKS[args.pages]
    else:
        titles = [t.strip() for t in args.pages.split(",") if t.strip()]

    season_id = resolve_season_id(args.api_url, args.season)
    print(f"season {args.season} → {season_id}")

    raw: list[RawTransfer] = []
    for title in titles:
        print(f"fetch {title}")
        try:
            wt = wiki_wikitext(title)
        except urllib.error.HTTPError as exc:
            print(f"  skip HTTP {exc.code}", file=sys.stderr)
            time.sleep(args.sleep)
            continue
        parsed = parse_permanent_transfers(title, wt)
        print(f"  parsed {len(parsed)} permanent moves")
        raw.extend(parsed)
        time.sleep(args.sleep)

    if args.limit:
        raw = raw[: args.limit]

    print("indexing players/clubs from API…")
    player_index = build_player_index(args.api_url)
    club_index = build_club_index(args.api_url)
    print(f"  players={len(player_index)} club_keys={len(club_index)}")

    items: list[dict[str, Any]] = []
    skipped = {"player": 0, "club": 0}

    for row in raw:
        player_id = search_player(row.player, player_index)
        if not player_id:
            skipped["player"] += 1
            continue
        from_id = search_club(row.from_club, club_index)
        to_id = search_club(row.to_club, club_index)
        if args.require_matched_club and not from_id and not to_id:
            skipped["club"] += 1
            continue
        if not to_id and not from_id:
            skipped["club"] += 1
            continue
        fee_eur = getattr(row, "fee_eur", None)
        items.append(
            {
                "playerId": player_id,
                "fromClubId": from_id,
                "toClubId": to_id,
                "seasonId": season_id,
                "transferDate": row.date,
                "feeEur": fee_eur,
                "type": row.transfer_type,
                "status": "COMPLETED",
                "source": f"wikipedia:{row.page_title}"[:64],
                "notes": (
                    f"{row.player}: {row.from_club or 'Unattached'} → "
                    f"{row.to_club or 'Unattached'} ({row.fee_raw})"
                )[:500],
            }
        )

    print(
        f"matched {len(items)} / {len(raw)} "
        f"(skip player={skipped['player']} club={skipped['club']})"
    )
    if args.dry_run:
        for item in items[:15]:
            print(" ", item["notes"])
        if len(items) > 15:
            print(f"  … {len(items) - 15} more")
        return 0

    # Bulk in chunks
    created = 0
    skipped_dup = 0
    for i in range(0, len(items), 50):
        chunk = items[i : i + 50]
        resp = http_json(
            "POST",
            f"{args.api_url.rstrip('/')}/api/v1/transfers/bulk",
            {"items": chunk},
        )
        created += int(resp.get("createdCount") or 0)
        skipped_dup += int(resp.get("skippedCount") or 0)
        print(f"  bulk {i // 50 + 1}: created={resp.get('createdCount')} skipped={resp.get('skippedCount')}")

    print(f"done created={created} skipped={skipped_dup}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
