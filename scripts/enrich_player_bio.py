#!/usr/bin/env python3
"""Fill missing player heightCm / preferredFoot from Wikidata + FBref profile pages.

Height: Wikidata P2048 (metres/cm) with Wikipedia search disambiguation by nationality.
Preferred foot: FBref player profile page ("Footed: Left/Right") when a profile can be
resolved via FBref search. Height on FBref is used as a fallback.

Usage:
  python3 scripts/enrich_player_bio.py --dry-run --limit 20
  python3 scripts/enrich_player_bio.py --only-missing --limit 200
  python3 scripts/enrich_player_bio.py --skip-fbref   # height-only (faster)

Requires network + running API. Respect crawl delays (FBref is slow by design).
"""

from __future__ import annotations

import argparse
import json
import re
import sys
import time
import unicodedata
import urllib.error
import urllib.parse
import urllib.request

API_DEFAULT = "http://localhost:8080"
WIKI_API = "https://en.wikipedia.org/w/api.php"
WIKIDATA_API = "https://www.wikidata.org/w/api.php"
FBREF_SEARCH = "https://fbref.com/en/search/search.fcgi"
USER_AGENT = "KleosTransfersBot/0.1 (https://github.com/Ysetia10/kleos-transfers; research bio enricher)"
WIKI_GAP_SEC = 0.25
FBREF_GAP_SEC = 3.0

COUNTRY_HINTS = {
    "ENG": ("england", "english"),
    "ESP": ("spain", "spanish"),
    "WAL": ("wales", "welsh"),
    "SCO": ("scotland", "scottish"),
    "IRL": ("ireland", "irish"),
    "FRA": ("france", "french"),
    "GER": ("germany", "german"),
    "ITA": ("italy", "italian"),
    "POR": ("portugal", "portuguese"),
    "NED": ("netherlands", "dutch"),
    "BEL": ("belgium", "belgian"),
    "BRA": ("brazil", "brazilian"),
    "ARG": ("argentina", "argentine", "argentinian"),
    "MAR": ("morocco", "moroccan"),
    "SEN": ("senegal", "senegalese"),
    "NGA": ("nigeria", "nigerian"),
    "GHA": ("ghana", "ghanaian"),
    "CIV": ("ivory coast", "ivorian", "côte d'ivoire"),
    "USA": ("united states", "american"),
    "MEX": ("mexico", "mexican"),
    "URU": ("uruguay", "uruguayan"),
    "COL": ("colombia", "colombian"),
    "CHI": ("chile", "chilean"),
    "CRO": ("croatia", "croatian"),
    "SRB": ("serbia", "serbian"),
    "DEN": ("denmark", "danish"),
    "SWE": ("sweden", "swedish"),
    "NOR": ("norway", "norwegian"),
    "POL": ("poland", "polish"),
    "TUR": ("turkey", "turkish"),
    "JPN": ("japan", "japanese"),
    "KOR": ("korea", "south korea", "korean"),
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--api-url", default=API_DEFAULT)
    parser.add_argument("--limit", type=int, default=0, help="max players to process (0 = all missing)")
    parser.add_argument("--offset", type=int, default=0)
    parser.add_argument("--only-missing", action="store_true", default=True)
    parser.add_argument("--include-existing", action="store_true", help="overwrite existing height/foot")
    parser.add_argument("--skip-fbref", action="store_true", help="skip FBref foot/height scrape")
    parser.add_argument("--dry-run", action="store_true")
    return parser.parse_args()


def fold_text(value: str) -> str:
    decomposed = unicodedata.normalize("NFKD", value)
    return "".join(char for char in decomposed if not unicodedata.combining(char)).casefold()


def http_json(url: str, method: str = "GET", payload: dict | None = None) -> dict:
    data = None if payload is None else json.dumps(payload).encode("utf-8")
    headers = {"Accept": "application/json", "User-Agent": USER_AGENT}
    if payload is not None:
        headers["Content-Type"] = "application/json"
    request = urllib.request.Request(url, data=data, headers=headers, method=method)
    with urllib.request.urlopen(request, timeout=45) as response:
        body = response.read()
        return json.loads(body) if body else {}


def http_text(url: str) -> str:
    request = urllib.request.Request(
        url,
        headers={
            "User-Agent": (
                "Mozilla/5.0 (compatible; KleosTransfersBot/0.1; "
                "+https://github.com/Ysetia10/kleos-transfers)"
            ),
            "Accept": "text/html,application/xhtml+xml",
            "Accept-Language": "en-US,en;q=0.9",
            "Referer": "https://fbref.com/",
        },
    )
    with urllib.request.urlopen(request, timeout=45) as response:
        return response.read().decode("utf-8", errors="replace")


def list_players(api_url: str) -> list[dict]:
    items: list[dict] = []
    page = 0
    while True:
        query = urllib.parse.urlencode({"page": page, "size": 100})
        payload = http_json(f"{api_url.rstrip('/')}/api/v1/players?{query}")
        content = payload.get("content", [])
        items.extend(content)
        if payload.get("last", True) or not content:
            break
        page += 1
    return items


def wiki_get(params: dict, api: str = WIKI_API) -> dict:
    params = {**params, "format": "json", "formatversion": "2"}
    time.sleep(WIKI_GAP_SEC)
    return http_json(f"{api}?{urllib.parse.urlencode(params)}")


def country_matches(blob: str, country_code: str | None) -> bool:
    if not country_code:
        return True
    hints = COUNTRY_HINTS.get(country_code.upper())
    if not hints:
        return True
    text = fold_text(blob)
    return any(hint in text for hint in hints)


def parse_height_cm(raw: str | None) -> int | None:
    if not raw:
        return None
    text = re.sub(r"<[^>]+>", " ", raw)
    text = text.replace(",", ".")
    # 1.81 m / {{convert|1.78|m|...}}
    convert = re.search(r"convert\|([0-9]+(?:\.[0-9]+)?)\|m\b", text, re.I)
    if convert:
        metres = float(convert.group(1))
        cm = int(round(metres * 100))
        return cm if 140 <= cm <= 230 else None
    metres = re.search(r"([0-9]+(?:\.[0-9]+)?)\s*m\b", text, re.I)
    if metres and "cm" not in text[: metres.start() + 10].casefold():
        cm = int(round(float(metres.group(1)) * 100))
        if 140 <= cm <= 230:
            return cm
    cm_match = re.search(r"([12][0-9]{2})\s*cm\b", text, re.I)
    if cm_match:
        cm = int(cm_match.group(1))
        return cm if 140 <= cm <= 230 else None
    # bare +180 style from Wikidata amount already handled elsewhere
    bare = re.search(r"\b([12][0-9]{2})\b", text)
    if bare:
        cm = int(bare.group(1))
        return cm if 140 <= cm <= 230 else None
    return None


def parse_preferred_foot(raw: str | None) -> str | None:
    if not raw:
        return None
    text = fold_text(re.sub(r"<[^>]+>", " ", raw))
    # Prefer explicit football phrasing to avoid "both clubs" false positives.
    if re.search(r"\bboth[\s-]?footed\b", text) or re.search(r"\btwo[\s-]?footed\b", text):
        return "BOTH"
    if re.search(r"\bleft[\s-]?footed\b", text) or re.search(r"\bfooted:\s*left\b", text):
        return "LEFT"
    if re.search(r"\bright[\s-]?footed\b", text) or re.search(r"\bfooted:\s*right\b", text):
        return "RIGHT"
    # Short infobox values: "Left", "Right", "Both"
    compact = text.strip()
    if compact in {"left", "l"}:
        return "LEFT"
    if compact in {"right", "r"}:
        return "RIGHT"
    if compact in {"both", "either"}:
        return "BOTH"
    return None


def wikidata_height_cm(entity_id: str) -> int | None:
    payload = wiki_get(
        {"action": "wbgetentities", "ids": entity_id, "props": "claims", "languages": "en"},
        api=WIKIDATA_API,
    )
    entity = (payload.get("entities") or {}).get(entity_id) or {}
    claims = (entity.get("claims") or {}).get("P2048") or []
    if not claims:
        return None
    value = ((claims[0].get("mainsnak") or {}).get("datavalue") or {}).get("value") or {}
    amount = value.get("amount")
    unit = value.get("unit") or ""
    if amount is None:
        return None
    number = float(str(amount).lstrip("+"))
    if unit.endswith("/Q174728") or unit.endswith("Q174728"):  # centimetre
        cm = int(round(number))
    elif unit.endswith("/Q11573") or unit.endswith("Q11573"):  # metre
        cm = int(round(number * 100))
    else:
        # Heuristic: values like 1.8 are metres; 180 are cm.
        cm = int(round(number * 100)) if number < 3 else int(round(number))
    return cm if 140 <= cm <= 230 else None


def resolve_wikidata_height(player: dict) -> int | None:
    name = player["fullName"]
    nationality = player.get("nationality")
    # Search by name only — appending nationality words often returns zero Wikidata hits.
    payload = wiki_get(
        {
            "action": "wbsearchentities",
            "search": name,
            "language": "en",
            "uselang": "en",
            "type": "item",
            "limit": 8,
        },
        api=WIKIDATA_API,
    )
    for hit in payload.get("search") or []:
        label = hit.get("label") or ""
        description = hit.get("description") or ""
        blob = f"{label} {description}"
        name_tokens = set(re.findall(r"[a-z0-9]+", fold_text(name)))
        label_tokens = set(re.findall(r"[a-z0-9]+", fold_text(label)))
        if not name_tokens or len(name_tokens & label_tokens) < min(2, len(name_tokens)):
            if fold_text(name) != fold_text(label):
                continue
        if "football" not in fold_text(blob) and "soccer" not in fold_text(blob):
            continue
        if not country_matches(blob, nationality):
            continue
        height = wikidata_height_cm(hit["id"])
        if height:
            return height
    return None


def resolve_wikipedia_foot(player: dict) -> str | None:
    """Best-effort preferred foot from Wikipedia article prose / infobox."""
    name = player["fullName"]
    nationality = player.get("nationality")
    hints = COUNTRY_HINTS.get((nationality or "").upper(), ())
    search = f"{name} footballer"
    payload = wiki_get(
        {
            "action": "query",
            "generator": "search",
            "gsrsearch": search,
            "gsrlimit": 5,
            "prop": "extracts|pageterms",
            "exintro": 1,
            "explaintext": 1,
            "wbptterms": "description",
        }
    )
    pages = sorted((payload.get("query") or {}).get("pages") or [], key=lambda page: page.get("index", 999))
    for page in pages:
        title = page.get("title") or ""
        terms = page.get("terms") or {}
        description = " ".join(terms.get("description") or [])
        extract = page.get("extract") or ""
        blob = f"{title} {description} {extract}"
        if fold_text(name.split()[0]) not in fold_text(title):
            continue
        if "football" not in fold_text(blob) and "soccer" not in fold_text(blob):
            continue
        if not country_matches(f"{title} {description}", nationality):
            continue
        foot = parse_preferred_foot(extract) or parse_preferred_foot(
            " ".join(re.findall(r"(left|right)-footed", extract, re.I))
        )
        if foot:
            return foot
        # Infobox via wikitext when extract lacks footedness.
        wt = wiki_get(
            {
                "action": "query",
                "prop": "revisions",
                "rvprop": "content",
                "rvslots": "main",
                "titles": title,
            }
        )
        revisions = ((wt.get("query") or {}).get("pages") or [{}])[0].get("revisions") or []
        if not revisions:
            continue
        text = ((revisions[0].get("slots") or {}).get("main") or {}).get("content") or ""
        for key in ("foot", "footed", "footedness"):
            match = re.search(rf"\|\s*{key}\s*=\s*([^\n]+)", text, re.I)
            if match:
                foot = parse_preferred_foot(match.group(1))
                if foot:
                    return foot
        mentions = re.findall(r"(left|right)-footed", text, re.I)
        if mentions:
            foot = parse_preferred_foot(mentions[0])
            if foot:
                return foot
    return None


def resolve_fbref_profile(player: dict) -> tuple[int | None, str | None]:
    """Return (heightCm, preferredFoot) from an FBref player page when found."""
    name = player["fullName"]
    query = urllib.parse.urlencode({"search": name})
    time.sleep(FBREF_GAP_SEC)
    try:
        html = http_text(f"{FBREF_SEARCH}?{query}")
    except Exception as error:  # noqa: BLE001
        print(f"  fbref search error: {error}")
        return None, None

    # Prefer /en/players/<id>/Name links.
    links = re.findall(r'href="(/en/players/[0-9a-f]{8}/[^"#]+)"', html, re.I)
    if not links:
        return None, None

    nationality = player.get("nationality")
    chosen = None
    for link in links[:8]:
        slug = urllib.parse.unquote(link.split("/")[-1].replace("-", " "))
        if fold_text(name.split()[0]) not in fold_text(slug):
            continue
        chosen = link
        break
    if chosen is None:
        chosen = links[0]

    profile_url = urllib.parse.urljoin("https://fbref.com", chosen)
    time.sleep(FBREF_GAP_SEC)
    try:
        page = http_text(profile_url)
    except Exception as error:  # noqa: BLE001
        print(f"  fbref profile error: {error}")
        return None, None

    # Soft nationality check from page meta line when present.
    if nationality and not country_matches(page[:4000], nationality):
        # Still accept if name matches strongly; FBref pages vary.
        pass

    foot = None
    foot_match = re.search(r"Footed:\s*</strong>\s*([A-Za-z]+)", page, re.I)
    if not foot_match:
        foot_match = re.search(r"Footed:\s*([A-Za-z]+)", page, re.I)
    if foot_match:
        foot = parse_preferred_foot(foot_match.group(1))

    height = None
    height_match = re.search(r"([12][0-9]{2})\s*cm", page, re.I)
    if height_match:
        height = parse_height_cm(height_match.group(0))

    return height, foot


def put_player_update(api_url: str, player: dict, height: int | None, foot: str | None, dry_run: bool) -> None:
    payload = {
        "fullName": player["fullName"],
        "dateOfBirth": player["dateOfBirth"],
        "nationality": player["nationality"],
        "heightCm": height if height is not None else player.get("heightCm"),
        "preferredFoot": foot if foot is not None else player.get("preferredFoot"),
        "primaryPosition": player["primaryPosition"],
        "fbrefId": player.get("fbrefId"),
    }
    path = f"/api/v1/players/{player['id']}"
    if dry_run:
        print(f"  dry-run PUT {path} height={payload['heightCm']} foot={payload['preferredFoot']}")
        return
    http_json(f"{api_url.rstrip('/')}{path}", method="PUT", payload=payload)


def needs_enrichment(player: dict, include_existing: bool) -> bool:
    if include_existing:
        return True
    return player.get("heightCm") is None or player.get("preferredFoot") is None


def main() -> None:
    args = parse_args()
    include_existing = args.include_existing
    players = list_players(args.api_url)
    candidates = [player for player in players if needs_enrichment(player, include_existing)]
    selected = candidates[args.offset :]
    if args.limit > 0:
        selected = selected[: args.limit]

    updated = 0
    skipped = 0
    failed = 0

    for index, player in enumerate(selected, start=1):
        label = f"{player['fullName']} ({player.get('nationality')})"
        print(f"[{index}/{len(selected)}] {label}", flush=True)
        try:
            height = player.get("heightCm") if not include_existing else None
            foot = player.get("preferredFoot") if not include_existing else None

            if height is None:
                height = resolve_wikidata_height(player)
                if height:
                    print(f"  height {height}cm via Wikidata", flush=True)

            if foot is None:
                foot = resolve_wikipedia_foot(player)
                if foot:
                    print(f"  foot {foot} via Wikipedia", flush=True)

            if (foot is None or height is None) and not args.skip_fbref:
                fb_height, fb_foot = resolve_fbref_profile(player)
                if height is None and fb_height is not None:
                    height = fb_height
                    print(f"  height {height}cm via FBref", flush=True)
                if foot is None and fb_foot is not None:
                    foot = fb_foot
                    print(f"  foot {foot} via FBref", flush=True)

            changed_height = height is not None and (include_existing or player.get("heightCm") is None)
            changed_foot = foot is not None and (include_existing or player.get("preferredFoot") is None)
            if not changed_height and not changed_foot:
                skipped += 1
                print("  no bio fields resolved", flush=True)
                continue

            put_player_update(
                args.api_url,
                player,
                height if changed_height else player.get("heightCm"),
                foot if changed_foot else player.get("preferredFoot"),
                args.dry_run,
            )
            updated += 1
        except Exception as error:  # noqa: BLE001
            failed += 1
            print(f"  error: {error}", flush=True)

    print(
        f"Done. updated={updated} skipped={skipped} failed={failed} dry_run={args.dry_run}",
        flush=True,
    )


if __name__ == "__main__":
    try:
        main()
    except Exception as error:  # noqa: BLE001
        print(error, file=sys.stderr)
        raise SystemExit(1) from error
