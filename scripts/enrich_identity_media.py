#!/usr/bin/env python3
"""Resolve identity media URLs for clubs (crests) and players (photos).

Clubs (preferred): TheSportsDB team badge URLs (hotlink only).
Clubs (fallback): Wikidata P154 / Wikipedia page images.
Players: English Wikipedia page images, free licenses only.

Stores HTTPS URL + attribution via PUT /api/v1/{players|clubs}/{id}/media.
Does NOT download logo binaries into git and does not scrape Google Images.

Usage:
  python3 scripts/enrich_identity_media.py clubs --limit 20
  python3 scripts/enrich_identity_media.py clubs --include-existing
  python3 scripts/enrich_identity_media.py players --limit 50
  python3 scripts/enrich_identity_media.py clubs --dry-run

Requires network. Respects API etiquette (User-Agent + delay).
"""

from __future__ import annotations

import argparse
import json
import re
import sys
import threading
import time
import unicodedata
import urllib.error
import urllib.parse
import urllib.request
from concurrent.futures import ThreadPoolExecutor, as_completed

API_DEFAULT = "http://localhost:8080"
WIKI_API = "https://en.wikipedia.org/w/api.php"
COMMONS_API = "https://commons.wikimedia.org/w/api.php"
WIKIDATA_API = "https://www.wikidata.org/w/api.php"
SPORTSDB_SEARCH = "https://www.thesportsdb.com/api/v1/json/3/searchteams.php"
USER_AGENT = "KleosTransfersBot/0.1 (https://github.com/Ysetia10/kleos-transfers; research media enricher)"
REQUEST_GAP_SEC = 0.25

_request_lock = threading.Lock()
_next_request_ok = 0.0
_print_lock = threading.Lock()
# association football club / sports club-ish instance ids (Wikidata)
FOOTBALL_CLUB_INSTANCE_IDS = {
    "Q476028",  # association football club
    "Q12973014",  # sports team
    "Q847017",  # sports club
}
COUNTRY_HINTS = {
    "ENG": ("england", "english", "premier league"),
    "ESP": ("spain", "spanish", "la liga"),
    "WAL": ("wales", "welsh"),
    "SCO": ("scotland", "scottish"),
    "IRL": ("ireland", "irish"),
    "NIR": ("northern ireland", "northern irish"),
    "FRA": ("france", "french"),
    "GER": ("germany", "german"),
    "ITA": ("italy", "italian"),
    "POR": ("portugal", "portuguese"),
    "NED": ("netherlands", "dutch"),
    "BEL": ("belgium", "belgian"),
    "BRA": ("brazil", "brazilian"),
    "ARG": ("argentina", "argentine", "argentinian"),
}
SPORTSDB_COUNTRY = {
    "ENG": "england",
    "ESP": "spain",
    "WAL": "wales",
    "SCO": "scotland",
    "IRL": "ireland",
    "FRA": "france",
    "GER": "germany",
    "ITA": "italy",
    "POR": "portugal",
    "NED": "netherlands",
}

# Free licenses we accept (substring match on LicenseShortName / License).
FREE_LICENSE_MARKERS = (
    "cc0",
    "cc-zero",
    "public domain",
    "pd",
    "cc-by",
    "cc by",
    "cc-by-sa",
    "cc by-sa",
    "gfdl",
)

# Non-free / fair-use markers — reject even if other text is noisy.
BLOCKED_LICENSE_MARKERS = (
    "fair use",
    "non-free",
    "copyright",
    "all rights reserved",
)

CLUB_ALIASES = {
    "manchester utd": "Manchester United F.C.",
    "manchester city": "Manchester City F.C.",
    "tottenham": "Tottenham Hotspur F.C.",
    "nottingham": "Nottingham Forest F.C.",
    "brighton": "Brighton & Hove Albion F.C.",
    "wolves": "Wolverhampton Wanderers F.C.",
    "newcastle": "Newcastle United F.C.",
    "west ham": "West Ham United F.C.",
    "west brom": "West Bromwich Albion F.C.",
    "athletic club": "Athletic Bilbao",
    "atlético madrid": "Atlético Madrid",
    "barcelona": "FC Barcelona",
    "real madrid": "Real Madrid CF",
    "bayern": "FC Bayern Munich",
    "inter": "Inter Milan",
    "psg": "Paris Saint-Germain F.C.",
    "sheffield united": "Sheffield United F.C.",
    "ipswich town": "Ipswich Town F.C.",
    "luton town": "Luton Town F.C.",
    "leeds united": "Leeds United F.C.",
    "leicester city": "Leicester City F.C.",
    "crystal palace": "Crystal Palace F.C.",
    "aston villa": "Aston Villa F.C.",
    "bournemouth": "AFC Bournemouth",
    "brentford": "Brentford F.C.",
    "fulham": "Fulham F.C.",
    "everton": "Everton F.C.",
    "southampton": "Southampton F.C.",
    "burnley": "Burnley F.C.",
    "sunderland": "Sunderland A.F.C.",
    "arsenal": "Arsenal F.C.",
    "chelsea": "Chelsea F.C.",
    "liverpool": "Liverpool F.C.",
    "dep. la coruña": "Deportivo de La Coruña",
    "celta vigo": "Celta de Vigo",
}

# TheSportsDB search strings (when FBref short names differ).
SPORTSDB_SEARCH_NAMES = {
    "manchester utd": "Manchester United",
    "nottingham": "Nott Forest",
    "oviedo": "Real Oviedo",
    "ipswich town": "Ipswich Town",
    "brighton": "Brighton and Hove Albion",
    "wolves": "Wolverhampton Wanderers",
    "newcastle": "Newcastle United",
    "west ham": "West Ham United",
    "west brom": "West Bromwich",
    "valladolid": "Real Valladolid",
    "athletic club": "Athletic Bilbao",
    "atlético madrid": "Atletico Madrid",
    "tottenham": "Tottenham Hotspur",
    "sheffield united": "Sheffield United",
    "luton town": "Luton",
    "leeds united": "Leeds United",
    "leicester city": "Leicester City",
    "crystal palace": "Crystal Palace",
    "aston villa": "Aston Villa",
    "bournemouth": "Bournemouth",
    "brentford": "Brentford",
    "dep. la coruña": "Deportivo de La Coruna",
    "cardiff city": "Cardiff City",
    "celta vigo": "Celta Vigo",
    "real madrid": "Real Madrid",
    "barcelona": "Barcelona",
    "arsenal": "Arsenal",
    "chelsea": "Chelsea",
    "liverpool": "Liverpool",
    "sporting gijón": "Sporting de Gijon",
    "rayo vallecano": "Rayo Vallecano",
    "alavés": "Deportivo Alaves",
    "almería": "Almeria",
    "cádiz": "Cadiz",
    "málaga": "Malaga",
    "leganés": "Leganes",
    "huddersfield": "Huddersfield",
    "hull city": "Hull City",
    "norwich city": "Norwich City",
    "swansea city": "Swansea",
    "stoke city": "Stoke City",
    "watford": "Watford",
    "middlesbrough": "Middlesbrough",
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("resource", choices=("clubs", "players"), help="identity type to enrich")
    parser.add_argument("--api-url", default=API_DEFAULT, help="Kleos API base URL")
    parser.add_argument("--limit", type=int, default=0, help="max identities to process (0 = all)")
    parser.add_argument("--offset", type=int, default=0, help="skip first N identities")
    parser.add_argument("--only-missing", action="store_true", default=True, help="skip rows that already have media")
    parser.add_argument("--include-existing", action="store_true", help="re-resolve even when media exists")
    parser.add_argument("--dry-run", action="store_true", help="resolve but do not PUT")
    parser.add_argument(
        "--wikimedia-fallback",
        action="store_true",
        help="for clubs, try Wikidata/Wikipedia if TheSportsDB misses (slow)",
    )
    parser.add_argument("--workers", type=int, default=6, help="concurrent resolvers (shared throttle)")
    return parser.parse_args()


def http_json(url: str, method: str = "GET", payload: dict | None = None) -> dict:
    data = None if payload is None else json.dumps(payload).encode("utf-8")
    headers = {
        "Accept": "application/json",
        "User-Agent": USER_AGENT,
    }
    if payload is not None:
        headers["Content-Type"] = "application/json"
    request = urllib.request.Request(url, data=data, headers=headers, method=method)
    last_error: Exception | None = None
    for attempt in range(3):
        try:
            with urllib.request.urlopen(request, timeout=45) as response:
                body = response.read()
                return json.loads(body) if body else {}
        except urllib.error.HTTPError as error:
            detail = error.read().decode("utf-8", errors="replace")
            raise RuntimeError(f"{method} {url} -> HTTP {error.code}: {detail}") from error
        except Exception as error:  # noqa: BLE001 - retry transient network/SSL failures
            last_error = error
            time.sleep(0.8 * (attempt + 1))
    raise RuntimeError(f"{method} {url} failed after retries: {last_error}") from last_error


def list_all(api_url: str, path: str) -> list[dict]:
    items: list[dict] = []
    page = 0
    while True:
        query = urllib.parse.urlencode({"page": page, "size": 100})
        payload = http_json(f"{api_url.rstrip('/')}{path}?{query}")
        content = payload.get("content", [])
        items.extend(content)
        if payload.get("last", True) or not content:
            break
        page += 1
        time.sleep(0.05)
    return items


def log(message: str) -> None:
    with _print_lock:
        print(message, flush=True)


def throttle_request() -> None:
    global _next_request_ok
    with _request_lock:
        now = time.monotonic()
        wait = _next_request_ok - now
        if wait > 0:
            time.sleep(wait)
        _next_request_ok = time.monotonic() + REQUEST_GAP_SEC


def wiki_get(params: dict, api: str = WIKI_API) -> dict:
    params = {**params, "format": "json", "formatversion": "2"}
    url = f"{api}?{urllib.parse.urlencode(params)}"
    throttle_request()
    return http_json(url)


def is_free_license(license_name: str) -> bool:
    text = (license_name or "").strip().lower()
    if not text:
        return False
    if "public domain" in text or text in {"pd", "cc0", "cc-zero"}:
        return True
    if any(marker in text for marker in BLOCKED_LICENSE_MARKERS):
        return False
    return any(marker in text for marker in FREE_LICENSE_MARKERS)


CREST_FILE_MARKERS = ("logo", "crest", "badge", "emblem", "shield", "coat")
REJECT_FILE_MARKERS = (
    "flag of",
    "surfing",
    "photograph",
    " vs ",
    "versus",
    "training",
    "kit ",
    "stadium",
    "fans ",
    "matchday",
    "wedding",
    "family",
)
REJECT_PAGE_MARKERS = (
    " season",
    "list of",
    "signing",
    "policy",
    "history of",
    "results",
    "rivalry",
    "supporters",
    "femenino",
    "women",
    "hall of fame",
    "in european",
    "reserves",
    "academy",
    "player",
)
STOP_NAME_TOKENS = {
    "fc",
    "cf",
    "afc",
    "sc",
    "the",
    "de",
    "del",
    "la",
    "club",
    "football",
    "soccer",
}


def looks_like_football_page(title: str, description: str) -> bool:
    blob = f"{title} {description}".lower()
    needles = (
        "footballer",
        "football player",
        "soccer player",
        "football club",
        "association football",
        "soccer club",
        "f.c.",
        "fc ",
        " football",
    )
    return any(n in blob for n in needles)


def looks_like_club_identity_page(title: str) -> bool:
    title_l = f" {fold_text(title)} "
    if any(marker in title_l for marker in REJECT_PAGE_MARKERS):
        return False
    # Rivalry articles use an en-dash between two clubs.
    if "–" in title or "—" in title:
        return False
    return True


def club_page_matches(identity_name: str, title: str, search_query: str) -> bool:
    """Club Wikipedia/Wikidata titles must be the club page, not a related article."""
    if not looks_like_club_identity_page(title):
        return False
    title_l = fold_text(title)
    candidates = [fold_text(identity_name), fold_text(search_query)]
    for candidate in candidates:
        bare = re.sub(r"\b(f\.?c\.?|c\.?f\.?|a\.?f\.?c\.?|football club)\b", "", candidate).strip()
        bare = re.sub(r"\s+", " ", bare)
        if not bare:
            continue
        if title_l == bare or title_l.startswith(bare + " "):
            return True
        if title_l.startswith(bare + " f.c") or title_l.startswith(bare + " cf"):
            return True
        if bare.startswith(title_l) and len(name_tokens(title)) >= 2:
            return True
    tokens = name_tokens(identity_name)
    return len(tokens) >= 2 and title_matches_name(identity_name, title, strict=True)


def looks_like_crest_file(file_title: str) -> bool:
    text = file_title.casefold()
    if any(marker in text for marker in REJECT_FILE_MARKERS):
        return False
    if text.endswith(".svg") or ".svg/" in text:
        return True
    return any(marker in text for marker in CREST_FILE_MARKERS)


def fold_text(value: str) -> str:
    decomposed = unicodedata.normalize("NFKD", value)
    return "".join(char for char in decomposed if not unicodedata.combining(char)).casefold()


def name_tokens(value: str) -> list[str]:
    return [
        token
        for token in re.findall(r"[a-z0-9]+", fold_text(value))
        if token not in STOP_NAME_TOKENS and len(token) > 2
    ]


def title_matches_name(
    identity_name: str, title: str, *, min_hits: int | None = None, strict: bool = False
) -> bool:
    tokens = name_tokens(identity_name)
    if not tokens:
        return True
    title_token_set = set(name_tokens(title)) | set(re.findall(r"[a-z0-9]+", fold_text(title)))
    hits = sum(1 for token in tokens if token in title_token_set)
    if min_hits is not None:
        required = min_hits
    elif strict:
        required = len(tokens)
    else:
        required = max(1, (len(tokens) + 1) // 2)
    return hits >= required


def player_names_align(identity_name: str, title: str) -> bool:
    """Accept exact token coverage or Wikipedia short-name subsets (e.g. Abdul Fatawu)."""
    identity_tokens = name_tokens(identity_name)
    title_tokens = name_tokens(title)
    if not identity_tokens or not title_tokens:
        return False
    identity_set = set(identity_tokens)
    title_set = set(title_tokens)
    if identity_set <= title_set:
        return True
    if title_set <= identity_set and len(title_set) >= min(2, len(identity_set)):
        return True
    return False


def media_payload(
    url: str,
    license_name: str,
    attribution: str,
    page_title: str,
    *,
    source: str = "wikimedia",
) -> dict:
    return {
        "imageUrl": url,
        "attribution": attribution,
        "license": license_name,
        "source": source,
        "pageTitle": page_title,
    }


def file_license_and_artist(
    file_title: str, api: str = WIKI_API, *, allow_nonfree: bool = False
) -> tuple[str | None, str | None, str | None]:
    """Return (thumb_or_url, license, attribution) for a Commons/Wikipedia file."""
    if not file_title:
        return None, None, None
    if not file_title.startswith("File:"):
        file_title = f"File:{file_title}"
    payload = wiki_get(
        {
            "action": "query",
            "titles": file_title,
            "prop": "imageinfo",
            "iiprop": "url|extmetadata",
            "iiurlwidth": 256,
        },
        api=api,
    )
    pages = payload.get("query", {}).get("pages", [])
    if not pages:
        return None, None, None
    info_list = pages[0].get("imageinfo") or []
    if not info_list:
        return None, None, None
    info = info_list[0]
    meta = info.get("extmetadata") or {}
    license_name = (meta.get("LicenseShortName") or meta.get("License") or {}).get("value", "")
    artist = (meta.get("Artist") or {}).get("value", "")
    artist_clean = re.sub(r"<[^>]+>", "", artist).strip()
    credit = (meta.get("Credit") or {}).get("value", "")
    credit_clean = re.sub(r"<[^>]+>", "", credit).strip()
    attribution = artist_clean or credit_clean or "Wikimedia"
    url = info.get("thumburl") or info.get("url")
    if not url:
        return None, license_name or None, attribution
    if is_free_license(license_name):
        return url, license_name, attribution
    if allow_nonfree:
        return url, license_name or "Non-free trademark (Wikimedia)", attribution
    return None, license_name or None, attribution


def resolve_wikipedia_image(
    search_query: str,
    *,
    require_football: bool,
    require_crest_file: bool = False,
    identity_name: str | None = None,
    allow_nonfree: bool = False,
    nationality_code: str | None = None,
) -> dict | None:
    payload = wiki_get(
        {
            "action": "query",
            "generator": "search",
            "gsrsearch": search_query,
            "gsrlimit": 8,
            "prop": "pageimages|pageterms|info",
            "piprop": "thumbnail|name",
            "pithumbsize": 256,
            "wbptterms": "description",
            "inprop": "url",
        }
    )
    pages = payload.get("query", {}).get("pages", [])
    if not pages:
        return None

    pages = sorted(pages, key=lambda page: page.get("index", 999))
    for page in pages:
        title = page.get("title") or ""
        terms = page.get("terms") or {}
        description = " ".join(terms.get("description") or [])
        if require_football and not looks_like_football_page(title, description):
            continue
        if identity_name:
            if require_crest_file:
                if not club_page_matches(identity_name, title, search_query):
                    continue
            elif not player_names_align(identity_name, title):
                continue
        elif require_crest_file and not looks_like_club_identity_page(title):
            continue
        if nationality_code and not require_crest_file:
            # Disambiguate same-name footballers (e.g. Welsh vs English Aaron Ramsey).
            if not country_matches(f"{title} {description}", nationality_code):
                continue
        file_name = page.get("pageimage")
        thumb = (page.get("thumbnail") or {}).get("source")
        if file_name:
            url, license_name, attribution = file_license_and_artist(
                file_name, allow_nonfree=allow_nonfree
            )
            if not url and allow_nonfree:
                # Fair-use crests are often hosted on enwiki, not Commons.
                url, license_name, attribution = file_license_and_artist(
                    file_name, api=WIKI_API, allow_nonfree=True
                )
            if url and license_name:
                return media_payload(
                    url,
                    license_name,
                    f"{attribution} via Wikimedia ({title})",
                    title,
                )
        # Club pages: use the page thumbnail even when File: metadata is incomplete.
        if allow_nonfree and thumb:
            return media_payload(
                thumb,
                "Wikipedia page image",
                f"Wikimedia via ({title})",
                title,
            )
    return None


def resolve_commons_file(
    search_query: str,
    *,
    require_crest_file: bool = False,
    identity_name: str | None = None,
) -> dict | None:
    """Search Commons File namespace for a free-licensed crest/logo/photo."""
    payload = wiki_get(
        {
            "action": "query",
            "generator": "search",
            "gsrsearch": search_query,
            "gsrnamespace": 6,
            "gsrlimit": 12,
            "prop": "imageinfo",
            "iiprop": "url|extmetadata|mime",
            "iiurlwidth": 256,
        },
        api=COMMONS_API,
    )
    pages = payload.get("query", {}).get("pages", [])
    if not pages:
        return None
    pages = sorted(pages, key=lambda page: page.get("index", 999))
    for page in pages:
        title = page.get("title") or ""
        if require_crest_file and not looks_like_crest_file(title):
            continue
        if identity_name and not title_matches_name(identity_name, title):
            continue
        info_list = page.get("imageinfo") or []
        if not info_list:
            continue
        info = info_list[0]
        mime = (info.get("mime") or "").lower()
        if mime and not mime.startswith("image/"):
            continue
        meta = info.get("extmetadata") or {}
        license_name = (meta.get("LicenseShortName") or meta.get("License") or {}).get("value", "")
        if not is_free_license(license_name):
            continue
        artist = (meta.get("Artist") or {}).get("value", "")
        artist_clean = re.sub(r"<[^>]+>", "", artist).strip()
        credit = (meta.get("Credit") or {}).get("value", "")
        credit_clean = re.sub(r"<[^>]+>", "", credit).strip()
        attribution = artist_clean or credit_clean or "Wikimedia Commons"
        url = info.get("thumburl") or info.get("url")
        if not url:
            continue
        return media_payload(
            url,
            license_name,
            f"{attribution} via Wikimedia Commons ({title})",
            title,
        )
    return None


def club_search_query(club: dict) -> str:
    name = club["name"]
    alias = CLUB_ALIASES.get(name.casefold())
    if alias:
        return alias
    return f"{name} football club"


def player_search_query(player: dict) -> str:
    name = player["fullName"]
    code = (player.get("nationality") or "").upper()
    hints = COUNTRY_HINTS.get(code) or ()
    # Prefer "Aaron Ramsey welsh footballer" so English/Welsh namesakes diverge.
    nationality_word = hints[0] if hints else code.casefold()
    if nationality_word:
        return f"{name} {nationality_word} footballer"
    return f"{name} footballer"


def wikidata_get(params: dict) -> dict:
    return wiki_get(params, api=WIKIDATA_API)


def wikidata_search_entity(search: str) -> list[dict]:
    payload = wikidata_get(
        {
            "action": "wbsearchentities",
            "search": search,
            "language": "en",
            "uselang": "en",
            "type": "item",
            "limit": 8,
        }
    )
    return payload.get("search") or []


def wikidata_entity(entity_id: str) -> dict | None:
    payload = wikidata_get(
        {
            "action": "wbgetentities",
            "ids": entity_id,
            "props": "claims|labels|descriptions",
            "languages": "en",
        }
    )
    return (payload.get("entities") or {}).get(entity_id)


def entity_instance_ids(entity: dict) -> set[str]:
    claims = entity.get("claims") or {}
    ids: set[str] = set()
    for claim in claims.get("P31") or []:
        mainsnak = claim.get("mainsnak") or {}
        datavalue = (mainsnak.get("datavalue") or {}).get("value") or {}
        entity_id = datavalue.get("id")
        if entity_id:
            ids.add(entity_id)
    return ids


def entity_logo_filename(entity: dict) -> str | None:
    claims = entity.get("claims") or {}
    for prop in ("P154", "P41"):  # logo image, then flag image as last resort skip P41 for clubs
        if prop == "P41":
            continue
        for claim in claims.get(prop) or []:
            mainsnak = claim.get("mainsnak") or {}
            datavalue = (mainsnak.get("datavalue") or {}).get("value")
            if isinstance(datavalue, str) and datavalue.strip():
                return datavalue.strip()
    return None


def entity_english_label(entity: dict) -> str:
    labels = entity.get("labels") or {}
    return ((labels.get("en") or {}).get("value")) or ""


def country_matches(description: str, country_code: str | None) -> bool:
    if not country_code:
        return True
    hints = COUNTRY_HINTS.get(country_code.upper())
    if not hints:
        return True
    text = fold_text(description)
    return any(hint in text for hint in hints)


def resolve_wikidata_logo(
    search: str, identity_name: str, country_code: str | None
) -> dict | None:
    """Resolve club crest via Wikidata P154 (logo image), including non-free trademarks."""
    for hit in wikidata_search_entity(search)[:5]:
        entity_id = hit.get("id")
        if not entity_id:
            continue
        label = hit.get("label") or ""
        description = hit.get("description") or ""
        blob = f"{label} {description}"
        if not title_matches_name(identity_name, label, strict=True) and not title_matches_name(
            identity_name, blob, strict=True
        ):
            continue
        if not country_matches(blob, country_code):
            continue
        entity = wikidata_entity(entity_id)
        if not entity:
            continue
        instances = entity_instance_ids(entity)
        description_l = fold_text(description)
        looks_football = (
            "football club" in description_l
            or "soccer club" in description_l
            or bool(instances & FOOTBALL_CLUB_INSTANCE_IDS)
        )
        if not looks_football:
            continue
        filename = entity_logo_filename(entity)
        if not filename:
            continue
        file_title = filename if filename.startswith("File:") else f"File:{filename}"
        url, license_name, attribution = file_license_and_artist(
            file_title, api=COMMONS_API, allow_nonfree=True
        )
        if not url:
            # Club trademarks are usually on English Wikipedia (fair use), not Commons.
            url, license_name, attribution = file_license_and_artist(
                file_title, api=WIKI_API, allow_nonfree=True
            )
        if not url or not license_name:
            continue
        page_title = entity_english_label(entity) or label or entity_id
        return media_payload(
            url,
            license_name,
            f"{attribution} via Wikidata/{page_title}",
            f"{page_title} ({filename})",
        )
    return None


def sportsdb_search_name(club: dict) -> str:
    name = club["name"]
    return SPORTSDB_SEARCH_NAMES.get(name.casefold(), name)


def _sportsdb_team_score(team: dict, query: str, identity: str, expected_country: str | None) -> int:
    team_name = team.get("strTeam") or ""
    alternate = team.get("strTeamAlternate") or ""
    league = fold_text(team.get("strLeague") or "")
    name_l = fold_text(team_name)
    if any(
        marker in name_l or marker in league
        for marker in (
            "women",
            "w.f.c",
            "femenino",
            "gloriosas",
            "ladies",
            "womens",
            "women's",
            "u21",
            "u23",
            "u18",
            "reserves",
            "academy",
        )
    ):
        return -1
    if (team.get("strSport") or "").casefold() != "soccer":
        return -1
    if expected_country:
        team_country = fold_text(team.get("strCountry") or "")
        uk = {"england", "wales", "scotland"}
        country_ok = expected_country in team_country or team_country in expected_country
        # Cardiff etc. may be Wales while Kleos stores ENG (or the reverse).
        if not country_ok and expected_country in uk and team_country in uk:
            country_ok = True
        if not country_ok:
            return -1
    team_fold = fold_text(f"{team_name} {alternate}")
    identity_tokens = name_tokens(identity) or name_tokens(query)
    # Require all significant identity tokens to appear in the SportsDB team name.
    if identity_tokens and not all(token in team_fold for token in identity_tokens):
        # Allow exact query/name equality (Arsenal == Arsenal).
        if fold_text(team_name) not in {fold_text(query), fold_text(identity)}:
            return -1
    score = 10
    if fold_text(team_name) in {fold_text(query), fold_text(identity)}:
        score += 50
    if "premier league" in league or "la liga" in league:
        score += 40
    elif "championship" in league or "liga 2" in league or "segunda" in league:
        score += 20
    if not team.get("strBadge") and not team.get("strTeamBadge"):
        return -1
    return score


def resolve_thesportsdb_badge(club: dict) -> dict | None:
    """Resolve club crest from TheSportsDB search API (badge URL hotlink)."""
    query = sportsdb_search_name(club)
    url = f"{SPORTSDB_SEARCH}?{urllib.parse.urlencode({'t': query})}"
    throttle_request()
    payload = http_json(url)
    teams = payload.get("teams") or []
    if not teams:
        return None
    country_code = (club.get("countryCode") or "").upper()
    expected_country = SPORTSDB_COUNTRY.get(country_code)
    identity = club["name"]
    ranked: list[tuple[int, dict]] = []
    for team in teams:
        score = _sportsdb_team_score(team, query, identity, expected_country)
        if score >= 0:
            ranked.append((score, team))
    if not ranked:
        return None
    ranked.sort(key=lambda item: item[0], reverse=True)
    team = ranked[0][1]
    team_name = team.get("strTeam") or query
    badge = team.get("strBadge") or team.get("strTeamBadge")
    league = team.get("strLeague") or "TheSportsDB"
    return media_payload(
        badge,
        "TheSportsDB",
        f"Badge via TheSportsDB ({team_name} · {league})",
        team_name,
        source="thesportsdb",
    )


def resolve_club_media(club: dict, *, wikimedia_fallback: bool = False) -> dict | None:
    # TheSportsDB first (reliable current badges). Wikimedia fallback is opt-in (slow).
    media = resolve_thesportsdb_badge(club)
    if media or not wikimedia_fallback:
        return media
    name = club["name"]
    country = club.get("countryCode")
    query = club_search_query(club)
    searches = []
    for candidate in (query, name):
        if candidate not in searches:
            searches.append(candidate)
    for search in searches:
        media = resolve_wikidata_logo(search, name, country)
        if media:
            return media
    for wiki_query in searches:
        media = resolve_wikipedia_image(
            wiki_query,
            require_football=True,
            require_crest_file=True,
            identity_name=name,
            allow_nonfree=True,
        )
        if media:
            return media
    return None


def resolve_player_media(player: dict) -> dict | None:
    name = player["fullName"]
    query = player_search_query(player)
    # Wikipedia only — Commons filename search is too noisy for player identity.
    return resolve_wikipedia_image(
        query,
        require_football=True,
        identity_name=name,
        nationality_code=player.get("nationality"),
    )


def already_has_media(resource: str, item: dict) -> bool:
    if resource == "clubs":
        return bool(item.get("crestUrl"))
    return bool(item.get("photoUrl"))


def put_media(api_url: str, resource: str, item_id: str, media: dict, dry_run: bool) -> None:
    path = f"/api/v1/{resource}/{item_id}/media"
    payload = {
        "imageUrl": media["imageUrl"],
        "attribution": media["attribution"],
        "license": media["license"],
        "source": media["source"],
    }
    if dry_run:
        print(f"  dry-run PUT {path} -> {payload['imageUrl'][:80]}…")
        return
    http_json(f"{api_url.rstrip('/')}{path}", method="PUT", payload=payload)


def enrich_one(
    args: argparse.Namespace,
    item: dict,
    *,
    index: int,
    total: int,
    only_missing: bool,
) -> str:
    label = item.get("name") or item.get("fullName") or item["id"]
    if only_missing and already_has_media(args.resource, item):
        log(f"[{index}/{total}] skip existing {label}")
        return "skipped"

    query = club_search_query(item) if args.resource == "clubs" else player_search_query(item)
    log(f"[{index}/{total}] resolve {label!r} via {query!r}")
    try:
        media = (
            resolve_club_media(item, wikimedia_fallback=args.wikimedia_fallback)
            if args.resource == "clubs"
            else resolve_player_media(item)
        )
        if media is None:
            log("  no usable image")
            return "failed"
        put_media(args.api_url, args.resource, item["id"], media, args.dry_run)
        log(f"  ok ({media['license']}) {media['pageTitle']}")
        return "resolved"
    except Exception as error:  # noqa: BLE001 - batch job should continue
        log(f"  error: {error}")
        return "failed"


def main() -> None:
    args = parse_args()
    only_missing = not args.include_existing
    workers = max(1, args.workers)
    items = list_all(args.api_url, f"/api/v1/{args.resource}")
    selected = items[args.offset :]
    if args.limit > 0:
        selected = selected[: args.limit]

    log(f"Enriching {len(selected)} {args.resource} (workers={workers}, dry_run={args.dry_run})")
    resolved = 0
    skipped = 0
    failed = 0

    with ThreadPoolExecutor(max_workers=workers) as pool:
        futures = [
            pool.submit(
                enrich_one,
                args,
                item,
                index=index,
                total=len(selected),
                only_missing=only_missing,
            )
            for index, item in enumerate(selected, start=1)
        ]
        for future in as_completed(futures):
            status = future.result()
            if status == "resolved":
                resolved += 1
            elif status == "skipped":
                skipped += 1
            else:
                failed += 1

    log(f"Done. resolved={resolved} skipped={skipped} failed={failed} dry_run={args.dry_run}")


if __name__ == "__main__":
    try:
        main()
    except Exception as error:  # noqa: BLE001
        print(error, file=sys.stderr)
        raise SystemExit(1) from error
