#!/usr/bin/env python3
"""Infer FREE releases when a player disappears from a club between adjacent seasons.

`infer_transfers_from_seasons.py` only emits club→club moves. Free-agent exits
(De Gea leaving Man Utd, Lloris leaving Spurs, retirements) leave no row, so the
prediction engine still treats the prior #1 as staying and benches the arrival.

This script adds COMPLETED / FREE transfers with ``toClubId=null`` dated at the
*next* season start — a fact that was already known then (contract expired /
release announced), reconstructed from consecutive PlayerSeason gaps.

Usage:
  python3 scripts/infer_free_agent_exits.py --dry-run
  python3 scripts/infer_free_agent_exits.py --min-minutes 1500
"""

from __future__ import annotations

import argparse
import json
import os
import subprocess
import sys
import urllib.error
import urllib.request


DEFAULT_DB = "postgresql://kleos:kleos@localhost:5432/kleos_transfers"
DEFAULT_API = "http://localhost:8080"

SQL = """
WITH ordered AS (
  SELECT
    s.id AS season_id,
    s.label,
    s.start_date,
    LAG(s.id) OVER (ORDER BY s.start_date) AS prev_season_id,
    LAG(s.label) OVER (ORDER BY s.start_date) AS prev_label,
    LAG(s.start_date) OVER (ORDER BY s.start_date) AS prev_start
  FROM seasons s
  WHERE s.deleted_at IS NULL
),
pairs AS (
  SELECT * FROM ordered WHERE prev_season_id IS NOT NULL
),
season_coverage AS (
  SELECT season_id, COUNT(*) AS rows
  FROM player_seasons
  WHERE deleted_at IS NULL
  GROUP BY season_id
),
prior AS (
  SELECT
    ps.player_id,
    p.full_name,
    ps.club_id,
    c.name AS club_name,
    c.country_code,
    ps.primary_position,
    ps.minutes_played,
    ps.season_id AS prior_season_id,
    s.label AS prior_label
  FROM player_seasons ps
  JOIN players p ON p.id = ps.player_id AND p.deleted_at IS NULL
  JOIN clubs c ON c.id = ps.club_id AND c.deleted_at IS NULL
  JOIN seasons s ON s.id = ps.season_id AND s.deleted_at IS NULL
  WHERE ps.deleted_at IS NULL
    AND c.country_code IN ('ENG', 'ESP', 'GER', 'ITA', 'FRA')
    AND ps.minutes_played >= %(min_minutes)s
)
SELECT
  prior.player_id::text,
  prior.full_name,
  prior.club_id::text,
  prior.club_name,
  prior.country_code,
  prior.primary_position,
  prior.minutes_played,
  prior.prior_label,
  pairs.label AS next_label,
  pairs.season_id::text AS next_season_id,
  pairs.start_date::text AS transfer_date
FROM prior
JOIN pairs ON pairs.prev_season_id = prior.prior_season_id
JOIN seasons next_s ON next_s.id = pairs.season_id
JOIN season_coverage cov ON cov.season_id = pairs.season_id AND cov.rows >= 500
WHERE next_s.end_date < CURRENT_DATE - 30
AND NOT EXISTS (
  SELECT 1
  FROM player_seasons nxt
  WHERE nxt.player_id = prior.player_id
    AND nxt.club_id = prior.club_id
    AND nxt.season_id = pairs.season_id
    AND nxt.deleted_at IS NULL
)
AND NOT EXISTS (
  SELECT 1
  FROM transfers t
  WHERE t.player_id = prior.player_id
    AND t.deleted_at IS NULL
    AND t.status = 'COMPLETED'
    AND t.from_club_id = prior.club_id
    AND t.season_id = pairs.season_id
)
ORDER BY pairs.start_date, prior.full_name
"""


def psql_json(db: str, sql: str, min_minutes: int) -> list[dict]:
    rendered = sql.replace("%(min_minutes)s", str(int(min_minutes)))
    cmd = [
        "psql",
        db,
        "-v",
        "ON_ERROR_STOP=1",
        "-t",
        "-A",
        "-F",
        "\t",
        "-c",
        rendered,
    ]
    # Prefer JSON via COPY-like query for safety
    json_sql = f"""
    SELECT COALESCE(json_agg(row_to_json(q)), '[]'::json) FROM (
{rendered}
    ) q
    """
    out = subprocess.check_output(
        ["psql", db, "-v", "ON_ERROR_STOP=1", "-t", "-A", "-c", json_sql],
        text=True,
    ).strip()
    return json.loads(out or "[]")


def post_json(url: str, payload: dict) -> dict:
    data = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(
        url, data=data, headers={"Content-Type": "application/json"}, method="POST"
    )
    with urllib.request.urlopen(req, timeout=120) as response:
        return json.loads(response.read().decode("utf-8"))


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--db", default=os.environ.get("KLEOS_DATABASE", DEFAULT_DB))
    parser.add_argument("--api-url", default=os.environ.get("KLEOS_API_URL", DEFAULT_API))
    parser.add_argument("--min-minutes", type=int, default=1500)
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--limit", type=int, default=0)
    parser.add_argument("--positions", default="", help="Comma filter e.g. GK (empty=all)")
    args = parser.parse_args()

    rows = psql_json(args.db, SQL, args.min_minutes)
    if args.positions.strip():
        wanted = {p.strip().upper() for p in args.positions.split(",") if p.strip()}
        rows = [r for r in rows if (r.get("primary_position") or "").upper() in wanted]

    if args.limit > 0:
        rows = rows[: args.limit]

    by_pos: dict[str, int] = {}
    for r in rows:
        pos = r.get("primary_position") or "?"
        by_pos[pos] = by_pos.get(pos, 0) + 1

    print(f"Inferred free-agent exits: {len(rows)} (min_minutes={args.min_minutes})")
    print("  by position:", dict(sorted(by_pos.items(), key=lambda kv: -kv[1])[:12]))
    sample = [r for r in rows if r.get("full_name") in {"David de Gea", "Hugo Lloris"}]
    for r in sample:
        print(
            f"  sample {r['full_name']}: {r['club_name']} {r['prior_label']} → free @ {r['next_label']}"
        )

    items = [
        {
            "playerId": r["player_id"],
            "fromClubId": r["club_id"],
            "toClubId": None,
            "seasonId": r["next_season_id"],
            "transferDate": r["transfer_date"],
            "feeEur": None,
            "type": "FREE",
            "status": "COMPLETED",
            "source": "kleos-free-agent-exit-infer",
            "notes": (
                f"Inferred free/release exit after {r['prior_label']} at {r['club_name']} "
                f"({r['minutes_played']} min); no {r['next_label']} row at club"
            ),
        }
        for r in rows
    ]

    if args.dry_run or not items:
        return 0

    base = args.api_url.rstrip("/")
    created = skipped = 0
    batch_size = 200
    for offset in range(0, len(items), batch_size):
        batch = items[offset : offset + batch_size]
        try:
            result = post_json(f"{base}/api/v1/transfers/bulk", {"items": batch})
        except urllib.error.HTTPError as exc:
            body = exc.read().decode("utf-8", errors="replace")
            print(f"bulk failed: {exc.code} {body}", file=sys.stderr)
            return 1
        created += int(result.get("createdCount") or 0)
        skipped += int(result.get("skippedCount") or 0)
        print(
            f"batch {offset // batch_size + 1}: "
            f"created={result.get('createdCount')} skipped={result.get('skippedCount')}"
        )

    print(f"Done. created={created} skipped={skipped}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
