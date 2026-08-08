#!/usr/bin/env python3
"""Fill missing player heightCm / preferredFoot / full DOB from Wikidata + Wikipedia.

Height: batched Wikidata SPARQL (P2048), matched by name + birth year + nationality.
DOB: Wikidata P569 with day precision; only applied when the year matches the
stored FBref birth year (avoids namesake mix-ups). Sets dateOfBirthPrecision=DAY.
Preferred foot: Wikipedia article/infobox phrasing; optional FBref profile fallback.

Usage:
  python3 scripts/enrich_player_bio.py --dry-run --limit 20
  python3 scripts/enrich_player_bio.py --skip-fbref --workers 8
  # full dates only (year-precision rows → day when Wikidata agrees on year)
  python3 scripts/enrich_player_bio.py --dob-only --workers 8

Requires network + running API. Heights/DOB use concurrent SPARQL batches; foot uses
concurrent Wikipedia lookups behind a shared throttle.
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
from dataclasses import dataclass

API_DEFAULT = "http://localhost:8080"
WIKI_API = "https://en.wikipedia.org/w/api.php"
WIKIDATA_API = "https://www.wikidata.org/w/api.php"
SPARQL_API = "https://query.wikidata.org/sparql"
FBREF_SEARCH = "https://fbref.com/en/search/search.fcgi"
USER_AGENT = "KleosTransfersBot/0.1 (https://github.com/Ysetia10/kleos-transfers; research bio enricher)"
# Shared Wikimedia/SPARQL budget. Workers wait on this lock.
WIKI_GAP_SEC = 0.35
SPARQL_GAP_SEC = 0.5
FBREF_GAP_SEC = 3.0
WIKI_MAX_RETRIES = 6
SPARQL_BATCH_SIZE = 40

_wiki_lock = threading.Lock()
_wiki_next_ok = 0.0
_print_lock = threading.Lock()

COUNTRY_HINTS = {
    "ENG": ("england", "english", "united kingdom", "great britain"),
    "ESP": ("spain", "spanish"),
    "WAL": ("wales", "welsh", "united kingdom", "great britain"),
    "SCO": ("scotland", "scottish", "united kingdom", "great britain"),
    "NIR": ("northern ireland", "united kingdom", "great britain"),
    "IRL": ("ireland", "irish", "republic of ireland"),
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
    "CIV": ("ivory coast", "ivorian", "côte d'ivoire", "cote d'ivoire"),
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
    "AUS": ("australia", "australian"),
    "CAN": ("canada", "canadian"),
    "SUI": ("switzerland", "swiss"),
    "AUT": ("austria", "austrian"),
    "CZE": ("czech", "czechia", "czech republic"),
    "SVK": ("slovakia", "slovak"),
    "HUN": ("hungary", "hungarian"),
    "ROU": ("romania", "romanian"),
    "GRE": ("greece", "greek"),
    "UKR": ("ukraine", "ukrainian"),
    "RUS": ("russia", "russian"),
    "EGY": ("egypt", "egyptian"),
    "ALG": ("algeria", "algerian"),
    "TUN": ("tunisia", "tunisian"),
    "CMR": ("cameroon", "cameroonian"),
    "COD": ("congo", "dr congo", "democratic republic of the congo"),
    "RSA": ("south africa", "south african"),
    "JAM": ("jamaica", "jamaican"),
    "TRI": ("trinidad", "tobago"),
    "PAR": ("paraguay", "paraguayan"),
    "PER": ("peru", "peruvian"),
    "ECU": ("ecuador", "ecuadorian"),
    "VEN": ("venezuela", "venezuelan"),
    "CRC": ("costa rica", "costa rican"),
    "HON": ("honduras", "honduran"),
    "PAN": ("panama", "panamanian"),
    "ISR": ("israel", "israeli"),
    "IRN": ("iran", "iranian"),
    "IRQ": ("iraq", "iraqi"),
    "KSA": ("saudi arabia", "saudi"),
    "UAE": ("united arab emirates", "emirati"),
    "QAT": ("qatar", "qatari"),
    "CHN": ("china", "chinese"),
    "NZL": ("new zealand"),
    "ISL": ("iceland", "icelandic"),
    "FIN": ("finland", "finnish"),
    "BIH": ("bosnia", "bosnian"),
    "MNE": ("montenegro", "montenegrin"),
    "MKD": ("north macedonia", "macedonia", "macedonian"),
    "ALB": ("albania", "albanian"),
    "KOS": ("kosovo"),
    "GEO": ("georgia", "georgian"),
    "ARM": ("armenia", "armenian"),
    "SLE": ("sierra leone"),
    "GIN": ("guinea", "guinean"),
    "GAB": ("gabon", "gabonese"),
    "MLI": ("mali", "malian"),
    "BFA": ("burkina faso"),
    "TOG": ("togo", "togolese"),
    "BEN": ("benin"),
    "ZIM": ("zimbabwe"),
    "ZAM": ("zambia"),
    "ANG": ("angola"),
    "MOZ": ("mozambique"),
    "CPV": ("cape verde"),
    "SUR": ("suriname"),
    "GUY": ("guyana"),
    "MTN": ("mauritania"),
}


@dataclass
class WikiHeightHit:
    name: str
    height_cm: int
    country: str
    birth_year: int | None
    qid: str


@dataclass
class WikiDobHit:
    name: str
    birth_date: str  # YYYY-MM-DD
    country: str
    qid: str


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--api-url", default=API_DEFAULT)
    parser.add_argument("--limit", type=int, default=0, help="max players to process (0 = all missing)")
    parser.add_argument("--offset", type=int, default=0)
    parser.add_argument("--only-missing", action="store_true", default=True)
    parser.add_argument("--include-existing", action="store_true", help="overwrite existing height/foot")
    parser.add_argument("--skip-fbref", action="store_true", help="skip FBref foot/height scrape")
    parser.add_argument("--skip-height", action="store_true", help="only resolve preferred foot")
    parser.add_argument("--skip-foot", action="store_true", help="only resolve height")
    parser.add_argument(
        "--skip-dob",
        action="store_true",
        help="do not upgrade YEAR-precision dates from Wikidata",
    )
    parser.add_argument(
        "--dob-only",
        action="store_true",
        help="only upgrade full DOB (implies --skip-height --skip-foot --skip-fbref)",
    )
    parser.add_argument("--workers", type=int, default=8, help="concurrent workers for foot + PUTs")
    parser.add_argument("--batch-size", type=int, default=SPARQL_BATCH_SIZE, help="SPARQL names per query")
    parser.add_argument("--dry-run", action="store_true")
    return parser.parse_args()


def log(message: str) -> None:
    with _print_lock:
        print(message, flush=True)


def throttle(gap_sec: float) -> None:
    global _wiki_next_ok
    with _wiki_lock:
        now = time.monotonic()
        wait = _wiki_next_ok - now
        if wait > 0:
            time.sleep(wait)
        _wiki_next_ok = time.monotonic() + gap_sec


def fold_text(value: str) -> str:
    decomposed = unicodedata.normalize("NFKD", value)
    return "".join(char for char in decomposed if not unicodedata.combining(char)).casefold()


def http_json(
    url: str,
    method: str = "GET",
    payload: dict | None = None,
    *,
    data: bytes | None = None,
    headers: dict | None = None,
    retries: int = 0,
) -> dict:
    body = data
    req_headers = {"Accept": "application/json", "User-Agent": USER_AGENT}
    if headers:
        req_headers.update(headers)
    if payload is not None:
        body = json.dumps(payload).encode("utf-8")
        req_headers["Content-Type"] = "application/json"
    request = urllib.request.Request(url, data=body, headers=req_headers, method=method)
    attempt = 0
    while True:
        try:
            with urllib.request.urlopen(request, timeout=60) as response:
                raw = response.read()
                return json.loads(raw) if raw else {}
        except urllib.error.HTTPError as error:
            if error.code not in {429, 503} or attempt >= retries:
                raise
            retry_after = error.headers.get("Retry-After")
            try:
                delay = float(retry_after) if retry_after else min(90.0, 2.0 ** attempt)
            except ValueError:
                delay = min(90.0, 2.0 ** attempt)
            log(f"  HTTP {error.code}; backing off {delay:.1f}s")
            time.sleep(delay)
            attempt += 1


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
    throttle(WIKI_GAP_SEC)
    return http_json(f"{api}?{urllib.parse.urlencode(params)}", retries=WIKI_MAX_RETRIES)


def country_matches(blob: str, country_code: str | None) -> bool:
    if not country_code:
        return True
    hints = COUNTRY_HINTS.get(country_code.upper())
    if not hints:
        return True
    text = fold_text(blob)
    return any(hint in text for hint in hints)


def parse_height_cm(raw: str | None) -> int | None:
    if raw is None:
        return None
    if isinstance(raw, (int, float)):
        cm = int(round(float(raw)))
        return cm if 140 <= cm <= 230 else None
    text = re.sub(r"<[^>]+>", " ", str(raw))
    text = text.replace(",", ".")
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
    try:
        number = float(str(raw).lstrip("+"))
    except ValueError:
        return None
    cm = int(round(number * 100)) if number < 3 else int(round(number))
    return cm if 140 <= cm <= 230 else None


def parse_preferred_foot(raw: str | None) -> str | None:
    if not raw:
        return None
    text = fold_text(re.sub(r"<[^>]+>", " ", raw))
    if re.search(r"\bboth[\s-]?footed\b", text) or re.search(r"\btwo[\s-]?footed\b", text):
        return "BOTH"
    if re.search(r"\bleft[\s-]?footed\b", text) or re.search(r"\bfooted:\s*left\b", text):
        return "LEFT"
    if re.search(r"\bright[\s-]?footed\b", text) or re.search(r"\bfooted:\s*right\b", text):
        return "RIGHT"
    compact = text.strip()
    if compact in {"left", "l"}:
        return "LEFT"
    if compact in {"right", "r"}:
        return "RIGHT"
    if compact in {"both", "either"}:
        return "BOTH"
    return None


def birth_year(player: dict) -> int | None:
    dob = player.get("dateOfBirth") or ""
    match = re.match(r"(\d{4})", str(dob))
    return int(match.group(1)) if match else None


def sparql_json(query: str) -> dict:
    throttle(SPARQL_GAP_SEC)
    data = urllib.parse.urlencode({"query": query, "format": "json"}).encode("utf-8")
    return http_json(
        SPARQL_API,
        method="POST",
        data=data,
        headers={
            "Accept": "application/sparql-results+json",
            "Content-Type": "application/x-www-form-urlencoded",
            "User-Agent": USER_AGENT,
        },
        retries=WIKI_MAX_RETRIES,
    )


NICKNAME_FIRST = {
    "alexander": ("alex",),
    "benjamin": ("ben",),
    "daniel": ("dani", "dan"),
    "david": ("dave",),
    "edward": ("ed", "eddie"),
    "frederick": ("fred", "freddie"),
    "gabriel": ("gabi", "gabby"),
    "james": ("jamie", "jim", "jimmy"),
    "jonathan": ("jon", "johnny"),
    "joseph": ("joe", "joey"),
    "joshua": ("josh",),
    "matthew": ("matt",),
    "michael": ("mike", "mick"),
    "nicholas": ("nick", "nico"),
    "oliver": ("oli", "ollie"),
    "patrick": ("pat", "paddy"),
    "philip": ("phil",),
    "robert": ("rob", "bob", "bobby"),
    "samuel": ("sam",),
    "stephen": ("steve",),
    "steven": ("steve",),
    "thomas": ("tom", "tommy"),
    "william": ("will", "willy", "bill"),
    "christopher": ("chris",),
    "antonio": ("toni", "tony"),
    "francisco": ("fran", "paco"),
    "alejandro": ("alex",),
}


def name_label_variants(name: str) -> list[str]:
    """English Wikidata labels sometimes omit diacritics or use nicknames."""
    variants = [name]
    ascii_name = "".join(
        char for char in unicodedata.normalize("NFKD", name) if not unicodedata.combining(char)
    )
    if ascii_name != name:
        variants.append(ascii_name)
    parts = name.split()
    if len(parts) >= 2:
        first_fold = fold_text(parts[0])
        for nick in NICKNAME_FIRST.get(first_fold, ()):
            variants.append(" ".join([nick.title(), *parts[1:]]))
            # Preserve trailing surname particles as-is.
            variants.append(f"{nick.title()} {' '.join(parts[1:])}")
    # De-dupe preserving order.
    seen: set[str] = set()
    ordered: list[str] = []
    for variant in variants:
        if variant not in seen:
            seen.add(variant)
            ordered.append(variant)
    return ordered


def sparql_height_hits(names: list[str]) -> list[WikiHeightHit]:
    """Batch-resolve footballer heights for exact English labels."""
    if not names:
        return []
    labels: list[str] = []
    seen: set[str] = set()
    for name in names:
        for variant in name_label_variants(name):
            cleaned = variant.replace('"', "")
            if cleaned and cleaned not in seen:
                seen.add(cleaned)
                labels.append(cleaned)
    values = " ".join(f'"{label}"@en' for label in labels)
    query = f"""
SELECT ?name ?item ?height ?unit ?countryLabel ?born WHERE {{
  VALUES ?name {{ {values} }}
  ?item rdfs:label|skos:altLabel ?name .
  ?item wdt:P106 wd:Q937857 .
  OPTIONAL {{
    ?item p:P2048 ?st .
    ?st psv:P2048 [ wikibase:quantityAmount ?height ; wikibase:quantityUnit ?unit ] .
  }}
  OPTIONAL {{ ?item wdt:P27 ?country . }}
  OPTIONAL {{ ?item wdt:P569 ?born . }}
  SERVICE wikibase:label {{ bd:serviceParam wikibase:language "en". }}
}}
"""
    payload = sparql_json(query)
    hits: list[WikiHeightHit] = []
    for row in (payload.get("results") or {}).get("bindings") or []:
        name = (row.get("name") or {}).get("value")
        height_raw = (row.get("height") or {}).get("value")
        unit = (row.get("unit") or {}).get("value") or ""
        if not name or height_raw is None:
            continue
        try:
            number = float(height_raw)
        except ValueError:
            continue
        if unit.endswith("Q11573"):  # metre
            cm = int(round(number * 100))
        else:
            cm = int(round(number * 100)) if number < 3 else int(round(number))
        if not (140 <= cm <= 230):
            continue
        born = (row.get("born") or {}).get("value") or ""
        byear = None
        year_match = re.search(r"(\d{4})", born)
        if year_match:
            byear = int(year_match.group(1))
        qid = ((row.get("item") or {}).get("value") or "").rsplit("/", 1)[-1]
        hits.append(
            WikiHeightHit(
                name=name,
                height_cm=cm,
                country=(row.get("countryLabel") or {}).get("value") or "",
                birth_year=byear,
                qid=qid,
            )
        )
    return hits


def score_height_hit(player: dict, hit: WikiHeightHit) -> int:
    score = 0
    py = birth_year(player)
    if py is not None and hit.birth_year is not None:
        if py == hit.birth_year:
            score += 50
        elif abs(py - hit.birth_year) <= 1:
            score += 20
        else:
            score -= 40
    if country_matches(hit.country, player.get("nationality")):
        score += 30
    elif hit.country:
        score -= 5
    return score


def match_height(player: dict, hits_by_name: dict[str, list[WikiHeightHit]]) -> int | None:
    hits: list[WikiHeightHit] = []
    for variant in name_label_variants(player["fullName"]):
        hits.extend(hits_by_name.get(variant) or [])
    if not hits:
        return None
    # De-dupe by qid, keep first.
    deduped: dict[str, WikiHeightHit] = {}
    for hit in hits:
        deduped.setdefault(hit.qid or f"{hit.name}:{hit.height_cm}", hit)
    ranked = sorted(deduped.values(), key=lambda hit: score_height_hit(player, hit), reverse=True)
    best = ranked[0]
    best_score = score_height_hit(player, best)
    if best_score < 0:
        return None
    if best_score < 20 and len(ranked) > 1:
        return None
    return best.height_cm


def sparql_dob_hits(names: list[str]) -> list[WikiDobHit]:
    """Batch-resolve day-precision birth dates (Wikidata timePrecision >= 11)."""
    if not names:
        return []
    labels: list[str] = []
    seen: set[str] = set()
    for name in names:
        for variant in name_label_variants(name):
            cleaned = variant.replace('"', "")
            if cleaned and cleaned not in seen:
                seen.add(cleaned)
                labels.append(cleaned)
    values = " ".join(f'"{label}"@en' for label in labels)
    # precision: 9=year, 10=month, 11=day
    query = f"""
SELECT ?name ?item ?born ?countryLabel WHERE {{
  VALUES ?name {{ {values} }}
  ?item rdfs:label|skos:altLabel ?name .
  ?item wdt:P106 wd:Q937857 .
  ?item p:P569/psv:P569 [
    wikibase:timeValue ?born ;
    wikibase:timePrecision ?prec
  ] .
  FILTER(?prec >= 11)
  OPTIONAL {{ ?item wdt:P27 ?country . }}
  SERVICE wikibase:label {{ bd:serviceParam wikibase:language "en". }}
}}
"""
    payload = sparql_json(query)
    hits: list[WikiDobHit] = []
    for row in (payload.get("results") or {}).get("bindings") or []:
        name = (row.get("name") or {}).get("value")
        born = (row.get("born") or {}).get("value") or ""
        if not name or len(born) < 10:
            continue
        date = born[:10]
        if not re.match(r"^\d{4}-\d{2}-\d{2}$", date):
            continue
        year, month, day = int(date[0:4]), int(date[5:7]), int(date[8:10])
        if not (1900 <= year <= 2015 and 1 <= month <= 12 and 1 <= day <= 31):
            continue
        qid = ((row.get("item") or {}).get("value") or "").rsplit("/", 1)[-1]
        hits.append(
            WikiDobHit(
                name=name,
                birth_date=date,
                country=(row.get("countryLabel") or {}).get("value") or "",
                qid=qid,
            )
        )
    return hits


def score_dob_hit(player: dict, hit: WikiDobHit) -> int:
    """Require exact birth-year match with stored FBref year to avoid namesakes."""
    py = birth_year(player)
    hit_year = int(hit.birth_date[0:4])
    if py is None or hit_year != py:
        return -100
    score = 50
    if country_matches(hit.country, player.get("nationality")):
        score += 30
    elif hit.country:
        score -= 5
    return score


def match_dob(player: dict, hits_by_name: dict[str, list[WikiDobHit]]) -> str | None:
    hits: list[WikiDobHit] = []
    for variant in name_label_variants(player["fullName"]):
        hits.extend(hits_by_name.get(variant) or [])
    if not hits:
        return None
    deduped: dict[str, WikiDobHit] = {}
    for hit in hits:
        deduped.setdefault(hit.qid or f"{hit.name}:{hit.birth_date}", hit)
    ranked = sorted(deduped.values(), key=lambda hit: score_dob_hit(player, hit), reverse=True)
    best = ranked[0]
    best_score = score_dob_hit(player, best)
    if best_score < 50:
        return None
    if len(ranked) > 1 and score_dob_hit(player, ranked[1]) == best_score:
        # Ambiguous same-year namesakes — skip.
        if ranked[0].birth_date != ranked[1].birth_date or ranked[0].qid != ranked[1].qid:
            if not country_matches(best.country, player.get("nationality")):
                return None
    return best.birth_date


def title_matches_player(name: str, title: str) -> bool:
    """Match wiki titles that use nicknames (Oliver → Oli McBurnie)."""
    name_tokens = [tok for tok in re.findall(r"[a-z0-9]+", fold_text(name)) if len(tok) > 1]
    title_fold = fold_text(title)
    if not name_tokens:
        return False
    surname = name_tokens[-1]
    if len(surname) > 2 and surname in title_fold:
        return True
    # Single-token / mononym players: require full token.
    if len(name_tokens) == 1:
        return name_tokens[0] in title_fold
    # Otherwise require first token prefix/ overlap (oli ⊂ oliver) plus another token.
    first = name_tokens[0]
    first_ok = first in title_fold or any(
        title_tok.startswith(first[:3]) and len(title_tok) >= 3
        for title_tok in re.findall(r"[a-z0-9]+", title_fold)
        if first.startswith(title_tok) or title_tok.startswith(first[: max(3, len(title_tok))])
    )
    return first_ok and any(tok in title_fold for tok in name_tokens[1:])


def parse_wiki_infobox_height(text: str) -> int | None:
    metre_patterns = (
        r"\|\s*height\s*=\s*\{\{[^\}]*?([12]\.[0-9]{1,2})\s*\|?\s*m",
        r"\|\s*height\s*=\s*\{\{convert\|([12]\.[0-9]{1,2})\|m",
        r"\|\s*height\s*=\s*([12]\.[0-9]{1,2})\s*m\b",
        r"\|\s*height\s*=\s*\{\{height\|m=([12]\.[0-9]{1,2})",
        r"\b([12]\.[0-9]{1,2})\s*m\b",
    )
    for pattern in metre_patterns:
        match = re.search(pattern, text[:8000], re.I)
        if match:
            cm = parse_height_cm(f"{match.group(1)} m")
            if cm:
                return cm
    cm_patterns = (
        r"\|\s*height\s*=\s*([12][0-9]{2})\s*cm\b",
        r"\|\s*height\s*=\s*\{\{height\|cm=([12][0-9]{2})",
        r"\b([12][0-9]{2})\s*cm\b",
    )
    for pattern in cm_patterns:
        match = re.search(pattern, text[:8000], re.I)
        if match:
            cm = parse_height_cm(f"{match.group(1)} cm")
            if cm:
                return cm
    return None


def resolve_wikipedia_bio(player: dict, *, want_height: bool, want_foot: bool) -> tuple[int | None, str | None]:
    """Best-effort height + preferred foot from Wikipedia search + infobox."""
    if not want_height and not want_foot:
        return None, None
    name = player["fullName"]
    nationality = player.get("nationality")
    payload = wiki_get(
        {
            "action": "query",
            "generator": "search",
            "gsrsearch": f"{name} footballer",
            "gsrlimit": 5,
            "prop": "extracts|pageterms",
            "exintro": 1,
            "explaintext": 1,
            "wbptterms": "description",
        }
    )
    pages = sorted((payload.get("query") or {}).get("pages") or [], key=lambda page: page.get("index", 999))
    height = None
    foot = None
    for page in pages:
        title = page.get("title") or ""
        terms = page.get("terms") or {}
        description = " ".join(terms.get("description") or [])
        extract = page.get("extract") or ""
        blob = f"{title} {description} {extract}"
        if not title_matches_player(name, title):
            continue
        if "football" not in fold_text(blob) and "soccer" not in fold_text(blob):
            continue
        if not country_matches(f"{title} {description} {extract[:400]}", nationality):
            continue
        if want_height and height is None:
            height = parse_wiki_infobox_height(extract)
        if want_foot and foot is None:
            mentions = re.findall(r"((?:left|right|both|two)[\s-]?footed)", extract, re.I)
            foot = parse_preferred_foot(" ".join(mentions)) if mentions else parse_preferred_foot(extract)
        if (not want_height or height) and (not want_foot or foot):
            return height, foot
        # Full wikitext is expensive; only fetch when height is still missing or
        # the lead already hints at footedness (infobox often repeats it).
        needs_wikitext = (want_height and height is None) or (
            want_foot and foot is None and re.search(r"foot", extract, re.I)
        )
        if not needs_wikitext:
            if height or foot:
                continue
            continue
        wt = wiki_get(
            {
                "action": "query",
                "prop": "revisions",
                "rvprop": "content",
                "rvslots": "main",
                "titles": title,
            }
        )
        pages_wt = (wt.get("query") or {}).get("pages") or []
        if not pages_wt:
            continue
        revisions = pages_wt[0].get("revisions") or []
        if not revisions:
            continue
        text = ((revisions[0].get("slots") or {}).get("main") or {}).get("content") or ""
        if want_height and height is None:
            height = parse_wiki_infobox_height(text)
        if want_foot and foot is None:
            for key in ("foot", "footed", "footedness"):
                match = re.search(rf"\|\s*{key}\s*=\s*([^\n\|]+)", text, re.I)
                if match:
                    foot = parse_preferred_foot(match.group(1).strip())
                    if foot:
                        break
            if foot is None:
                mentions = re.findall(r"((?:left|right|both|two)[\s-]?footed)", text, re.I)
                if mentions:
                    foot = parse_preferred_foot(" ".join(mentions[:3]))
        if (not want_height or height) and (not want_foot or foot):
            return height, foot
        if height or foot:
            # Keep scanning only if still missing something; otherwise return partial.
            continue
    return height, foot


def resolve_fbref_profile(player: dict) -> tuple[int | None, str | None]:
    name = player["fullName"]
    query = urllib.parse.urlencode({"search": name})
    time.sleep(FBREF_GAP_SEC)
    try:
        html = http_text(f"{FBREF_SEARCH}?{query}")
    except Exception as error:  # noqa: BLE001
        log(f"  fbref search error: {error}")
        return None, None

    links = re.findall(r'href="(/en/players/[0-9a-f]{8}/[^"#]+)"', html, re.I)
    if not links:
        return None, None

    chosen = None
    for link in links[:8]:
        slug = urllib.parse.unquote(link.split("/")[-1].replace("-", " "))
        if fold_text(name.split()[0]) in fold_text(slug):
            chosen = link
            break
    if chosen is None:
        chosen = links[0]

    profile_url = urllib.parse.urljoin("https://fbref.com", chosen)
    time.sleep(FBREF_GAP_SEC)
    try:
        page = http_text(profile_url)
    except Exception as error:  # noqa: BLE001
        log(f"  fbref profile error: {error}")
        return None, None

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


def put_player_update(
    api_url: str,
    player: dict,
    height: int | None,
    foot: str | None,
    dry_run: bool,
    *,
    date_of_birth: str | None = None,
    date_of_birth_precision: str | None = None,
) -> None:
    payload = {
        "fullName": player["fullName"],
        "dateOfBirth": date_of_birth or player["dateOfBirth"],
        "dateOfBirthPrecision": date_of_birth_precision
        or player.get("dateOfBirthPrecision")
        or "YEAR",
        "nationality": player["nationality"],
        "heightCm": height if height is not None else player.get("heightCm"),
        "preferredFoot": foot if foot is not None else player.get("preferredFoot"),
        "primaryPosition": player["primaryPosition"],
        "fbrefId": player.get("fbrefId"),
    }
    path = f"/api/v1/players/{player['id']}"
    if dry_run:
        log(
            f"  dry-run PUT {path} dob={payload['dateOfBirth']} "
            f"prec={payload['dateOfBirthPrecision']} "
            f"height={payload['heightCm']} foot={payload['preferredFoot']}"
        )
        return
    http_json(f"{api_url.rstrip('/')}{path}", method="PUT", payload=payload)


def needs_enrichment(
    player: dict,
    include_existing: bool,
    skip_height: bool,
    skip_foot: bool,
    skip_dob: bool,
) -> bool:
    if include_existing:
        return True
    need_height = (not skip_height) and player.get("heightCm") is None
    need_foot = (not skip_foot) and player.get("preferredFoot") is None
    need_dob = (not skip_dob) and player.get("dateOfBirthPrecision") != "DAY"
    return need_height or need_foot or need_dob


def fetch_batched_hits(names: list[str], batch_size: int, workers: int, kind: str, fetch_fn):
    unique_names = sorted(set(names))
    batches = [unique_names[i : i + batch_size] for i in range(0, len(unique_names), batch_size)]
    log(f"SPARQL {kind} lookup: {len(unique_names)} names in {len(batches)} batches (workers={workers})")
    hits_by_name: dict[str, list] = {}

    def run_batch(batch: list[str]):
        try:
            return fetch_fn(batch)
        except Exception as error:  # noqa: BLE001
            log(f"  SPARQL {kind} batch failed ({len(batch)} names): {error}")
            return []

    with ThreadPoolExecutor(max_workers=max(1, workers)) as pool:
        futures = [pool.submit(run_batch, batch) for batch in batches]
        done = 0
        for future in as_completed(futures):
            for hit in future.result():
                hits_by_name.setdefault(hit.name, []).append(hit)
            done += 1
            if done % 5 == 0 or done == len(batches):
                log(f"  SPARQL {kind} batches {done}/{len(batches)} · hit-names={len(hits_by_name)}")
    return hits_by_name


def enrich_one(
    player: dict,
    *,
    api_url: str,
    include_existing: bool,
    skip_fbref: bool,
    skip_height: bool,
    skip_foot: bool,
    skip_dob: bool,
    dry_run: bool,
    height_hint: int | None,
    dob_hint: str | None,
    index: int,
    total: int,
) -> str:
    label = f"{player['fullName']} ({player.get('nationality')})"
    try:
        height = None if include_existing else player.get("heightCm")
        foot = None if include_existing else player.get("preferredFoot")
        notes: list[str] = []
        new_dob = None

        if height is None and not skip_height and height_hint is not None:
            height = height_hint
            notes.append(f"height {height}cm(sparql)")

        if (
            not skip_dob
            and dob_hint
            and (include_existing or player.get("dateOfBirthPrecision") != "DAY")
        ):
            new_dob = dob_hint
            notes.append(f"dob {new_dob}")

        want_height = height is None and not skip_height
        want_foot = foot is None and not skip_foot
        if want_height or want_foot:
            wiki_height, wiki_foot = resolve_wikipedia_bio(player, want_height=want_height, want_foot=want_foot)
            if height is None and wiki_height is not None:
                height = wiki_height
                notes.append(f"height {height}cm(wiki)")
            if foot is None and wiki_foot is not None:
                foot = wiki_foot
                notes.append(f"foot {foot}")

        if (foot is None or height is None) and not skip_fbref:
            fb_height, fb_foot = resolve_fbref_profile(player)
            if height is None and fb_height is not None:
                height = fb_height
                notes.append(f"height {height}cm(fbref)")
            if foot is None and fb_foot is not None:
                foot = fb_foot
                notes.append(f"foot {foot}(fbref)")

        changed_height = height is not None and (include_existing or player.get("heightCm") is None)
        changed_foot = foot is not None and (include_existing or player.get("preferredFoot") is None)
        changed_dob = new_dob is not None and (
            include_existing
            or player.get("dateOfBirthPrecision") != "DAY"
            or player.get("dateOfBirth") != new_dob
        )
        if not changed_height and not changed_foot and not changed_dob:
            log(f"[{index}/{total}] skip {label}")
            return "skipped"

        put_player_update(
            api_url,
            player,
            height if changed_height else player.get("heightCm"),
            foot if changed_foot else player.get("preferredFoot"),
            dry_run,
            date_of_birth=new_dob if changed_dob else None,
            date_of_birth_precision="DAY" if changed_dob else None,
        )
        detail = ", ".join(notes) if notes else "updated"
        log(f"[{index}/{total}] ok {label} · {detail}")
        return "updated"
    except Exception as error:  # noqa: BLE001
        log(f"[{index}/{total}] fail {label} · {error}")
        return "failed"


def main() -> None:
    args = parse_args()
    if args.dob_only:
        args.skip_height = True
        args.skip_foot = True
        args.skip_fbref = True
        args.skip_dob = False
    include_existing = args.include_existing
    workers = max(1, args.workers)
    batch_size = max(5, args.batch_size)

    players = list_players(args.api_url)
    candidates = [
        player
        for player in players
        if needs_enrichment(
            player, include_existing, args.skip_height, args.skip_foot, args.skip_dob
        )
    ]
    selected = candidates[args.offset :]
    if args.limit > 0:
        selected = selected[: args.limit]

    log(
        f"Enriching {len(selected)} players "
        f"(workers={workers}, skip_fbref={args.skip_fbref}, "
        f"skip_height={args.skip_height}, skip_foot={args.skip_foot}, "
        f"skip_dob={args.skip_dob}, dry_run={args.dry_run})"
    )

    height_hints: dict[str, int] = {}
    if not args.skip_height:
        need_height = [
            player
            for player in selected
            if include_existing or player.get("heightCm") is None
        ]
        hits_by_name = fetch_batched_hits(
            [player["fullName"] for player in need_height],
            batch_size=batch_size,
            workers=min(workers, 4),
            kind="height",
            fetch_fn=sparql_height_hits,
        )
        for player in need_height:
            matched = match_height(player, hits_by_name)
            if matched is not None:
                height_hints[player["id"]] = matched
        log(f"Matched heights for {len(height_hints)}/{len(need_height)} players")

    dob_hints: dict[str, str] = {}
    if not args.skip_dob:
        need_dob = [
            player
            for player in selected
            if include_existing or player.get("dateOfBirthPrecision") != "DAY"
        ]
        hits_by_name = fetch_batched_hits(
            [player["fullName"] for player in need_dob],
            batch_size=batch_size,
            workers=min(workers, 4),
            kind="dob",
            fetch_fn=sparql_dob_hits,
        )
        for player in need_dob:
            matched = match_dob(player, hits_by_name)
            if matched is not None:
                dob_hints[player["id"]] = matched
        log(f"Matched full DOBs for {len(dob_hints)}/{len(need_dob)} players")

    updated = 0
    skipped = 0
    failed = 0

    def run(player: dict, index: int) -> str:
        return enrich_one(
            player,
            api_url=args.api_url,
            include_existing=include_existing,
            skip_fbref=args.skip_fbref,
            skip_height=args.skip_height,
            skip_foot=args.skip_foot,
            skip_dob=args.skip_dob,
            dry_run=args.dry_run,
            height_hint=height_hints.get(player["id"]),
            dob_hint=dob_hints.get(player["id"]),
            index=index,
            total=len(selected),
        )

    # DOB/height-only PUTs are local/fast — allow a wider pool.
    pool_workers = 1 if workers == 1 else min(workers, 12 if args.skip_foot else workers)
    with ThreadPoolExecutor(max_workers=pool_workers) as pool:
        futures = [pool.submit(run, player, index) for index, player in enumerate(selected, start=1)]
        results = [future.result() for future in as_completed(futures)]

    for status in results:
        if status == "updated":
            updated += 1
        elif status == "skipped":
            skipped += 1
        else:
            failed += 1

    log(f"Done. updated={updated} skipped={skipped} failed={failed} dry_run={args.dry_run}")


if __name__ == "__main__":
    try:
        main()
    except Exception as error:  # noqa: BLE001
        print(error, file=sys.stderr)
        raise SystemExit(1) from error
