#!/usr/bin/env python3
"""Backtest v0 predictions against a completed season's PlayerSeason outcomes.

For each candidate (player at a club in the target season with prior history):
  1. POST /api/v1/predictions  (engine runs as-of season start — no outcome leak)
  2. POST /api/v1/predictions/{id}/evaluate
  3. Aggregate MAE / RMSE / bias for minutes, goals, assists, xG, xA

Candidate discovery uses local PostgreSQL (same DB the backend uses). Predictions and
evaluations go through the HTTP API so they reuse PredictionEvaluation persistence.

Examples:
  ./scripts/validate_predictions_season.py --season 2024/25 --dry-run
  ./scripts/validate_predictions_season.py --season 2024/25 --limit 200
"""

from __future__ import annotations

import argparse
import json
import math
import os
import subprocess
import sys
import urllib.error
import urllib.request
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_OUT_DIR = ROOT / "research" / "validation"


@dataclass
class Candidate:
    player_id: str
    player_name: str
    club_id: str
    club_name: str
    season_id: str
    minutes: int
    goals: int
    assists: int


@dataclass
class Metrics:
    n: int = 0
    minutes_abs: float = 0.0
    minutes_sq: float = 0.0
    minutes_bias: float = 0.0
    goals_abs: float = 0.0
    goals_sq: float = 0.0
    goals_bias: float = 0.0
    assists_abs: float = 0.0
    assists_sq: float = 0.0
    assists_bias: float = 0.0
    xg_abs: float = 0.0
    xg_sq: float = 0.0
    xa_abs: float = 0.0
    xa_sq: float = 0.0

    def add(self, evaluation: dict[str, Any]) -> None:
        self.n += 1
        me = float(evaluation["minutesError"])
        ge = float(evaluation["goalsError"])
        ae = float(evaluation["assistsError"])
        xge = float(evaluation["xgError"])
        xae = float(evaluation["xaError"])
        self.minutes_abs += abs(me)
        self.minutes_sq += me * me
        self.minutes_bias += me
        self.goals_abs += abs(ge)
        self.goals_sq += ge * ge
        self.goals_bias += ge
        self.assists_abs += abs(ae)
        self.assists_sq += ae * ae
        self.assists_bias += ae
        self.xg_abs += abs(xge)
        self.xg_sq += xge * xge
        self.xa_abs += abs(xae)
        self.xa_sq += xae * xae

    def summary(self) -> dict[str, Any]:
        if self.n == 0:
            return {"n": 0}
        n = self.n
        return {
            "n": n,
            "minutes": {
                "mae": round(self.minutes_abs / n, 2),
                "rmse": round(math.sqrt(self.minutes_sq / n), 2),
                "bias_actual_minus_predicted": round(self.minutes_bias / n, 2),
            },
            "goals": {
                "mae": round(self.goals_abs / n, 3),
                "rmse": round(math.sqrt(self.goals_sq / n), 3),
                "bias_actual_minus_predicted": round(self.goals_bias / n, 3),
            },
            "assists": {
                "mae": round(self.assists_abs / n, 3),
                "rmse": round(math.sqrt(self.assists_sq / n), 3),
                "bias_actual_minus_predicted": round(self.assists_bias / n, 3),
            },
            "xg": {
                "mae": round(self.xg_abs / n, 3),
                "rmse": round(math.sqrt(self.xg_sq / n), 3),
            },
            "xa": {
                "mae": round(self.xa_abs / n, 3),
                "rmse": round(math.sqrt(self.xa_sq / n), 3),
            },
        }


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--api-url", default=os.environ.get("KLEOS_API_URL", "http://localhost:8080"))
    parser.add_argument("--season", default="2024/25", help="Completed season label to validate")
    parser.add_argument("--min-minutes", type=int, default=900, help="Minimum actual minutes in target season")
    parser.add_argument(
        "--require-club-change",
        action=argparse.BooleanOptionalAction,
        default=True,
        help="Only players whose most recent prior club differs from the target club",
    )
    parser.add_argument("--limit", type=int, default=200, help="Max candidates to predict/evaluate")
    parser.add_argument("--dry-run", action="store_true", help="List candidates only; no API writes")
    parser.add_argument(
        "--db",
        default=os.environ.get(
            "KLEOS_DATABASE",
            "postgresql://kleos:kleos@localhost:5432/kleos_transfers",
        ),
        help="PostgreSQL URL for candidate discovery (psql)",
    )
    parser.add_argument("--out-dir", type=Path, default=DEFAULT_OUT_DIR)
    return parser.parse_args()


def http_json(method: str, url: str, body: dict | None = None) -> Any:
    data = None if body is None else json.dumps(body).encode("utf-8")
    request = urllib.request.Request(
        url,
        data=data,
        method=method,
        headers={"Content-Type": "application/json", "Accept": "application/json"},
    )
    try:
        with urllib.request.urlopen(request, timeout=60) as response:
            raw = response.read().decode("utf-8")
            return json.loads(raw) if raw else None
    except urllib.error.HTTPError as error:
        detail = error.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"{method} {url} -> HTTP {error.code}: {detail}") from error


def discover_candidates(
    db_url: str,
    season_label: str,
    min_minutes: int,
    require_club_change: bool,
    limit: int,
) -> list[Candidate]:
    # Most recent prior club differs from the target-season club (arrived / returned).
    club_change_sql = """
      AND (
        SELECT prior_ps.club_id
        FROM player_seasons prior_ps
        JOIN seasons prior_s ON prior_s.id = prior_ps.season_id
        WHERE prior_ps.player_id = ps.player_id
          AND prior_s.start_date < s.start_date
          AND prior_ps.deleted_at IS NULL
        ORDER BY prior_s.start_date DESC
        LIMIT 1
      ) IS DISTINCT FROM ps.club_id
    """ if require_club_change else ""

    sql = f"""
    SELECT COALESCE(json_agg(row_to_json(t)), '[]'::json)
    FROM (
      SELECT
        p.id::text AS player_id,
        p.full_name AS player_name,
        c.id::text AS club_id,
        c.name AS club_name,
        s.id::text AS season_id,
        ps.minutes_played AS minutes,
        ps.goals,
        ps.assists
      FROM player_seasons ps
      JOIN players p ON p.id = ps.player_id AND p.deleted_at IS NULL
      JOIN clubs c ON c.id = ps.club_id AND c.deleted_at IS NULL
      JOIN seasons s ON s.id = ps.season_id AND s.deleted_at IS NULL
      WHERE s.label = '{season_label}'
        AND ps.deleted_at IS NULL
        AND ps.minutes_played >= {int(min_minutes)}
        AND EXISTS (
          SELECT 1
          FROM player_seasons hist
          JOIN seasons hist_s ON hist_s.id = hist.season_id
          WHERE hist.player_id = ps.player_id
            AND hist_s.start_date < s.start_date
            AND hist.deleted_at IS NULL
        )
        {club_change_sql}
      ORDER BY ps.minutes_played DESC
      LIMIT {int(limit)}
    ) t
    """
    try:
        completed = subprocess.run(
            ["psql", db_url, "-v", "ON_ERROR_STOP=1", "-t", "-A", "-c", sql],
            check=True,
            capture_output=True,
            text=True,
        )
    except FileNotFoundError as error:
        raise SystemExit("psql not found on PATH; install PostgreSQL client tools") from error
    except subprocess.CalledProcessError as error:
        raise SystemExit(f"Candidate query failed:\n{error.stderr}") from error

    payload = json.loads(completed.stdout.strip() or "[]")
    return [
        Candidate(
            player_id=row["player_id"],
            player_name=row["player_name"],
            club_id=row["club_id"],
            club_name=row["club_name"],
            season_id=row["season_id"],
            minutes=int(row["minutes"]),
            goals=int(row["goals"]),
            assists=int(row["assists"]),
        )
        for row in payload
    ]


def write_report(out_dir: Path, season: str, payload: dict[str, Any]) -> tuple[Path, Path]:
    out_dir.mkdir(parents=True, exist_ok=True)
    stamp = season.replace("/", "-")
    model_slug = str(payload.get("modelVersion") or "model").replace("/", "-")
    json_path = out_dir / f"{model_slug}-{stamp}.json"
    md_path = out_dir / f"{model_slug}-{stamp}.md"
    json_path.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")

    metrics = payload["metrics"]
    lines = [
        f"# Prediction validation — {season}",
        "",
        f"Generated: `{payload['generatedAt']}`",
        f"Model: `{payload['modelVersion']}`",
        f"Sample size: **{metrics.get('n', 0)}** evaluated predictions",
        "",
        "## Selection",
        "",
        f"- Min actual minutes: `{payload['selection']['minMinutes']}`",
        f"- Require prior club change: `{payload['selection']['requireClubChange']}`",
        f"- Limit: `{payload['selection']['limit']}`",
        "",
        "## Error metrics (actual − predicted)",
        "",
        "| Metric | MAE | RMSE | Bias |",
        "|--------|-----|------|------|",
    ]
    if metrics.get("n"):
        lines.extend(
            [
                f"| Minutes | {metrics['minutes']['mae']} | {metrics['minutes']['rmse']} | {metrics['minutes']['bias_actual_minus_predicted']} |",
                f"| Goals | {metrics['goals']['mae']} | {metrics['goals']['rmse']} | {metrics['goals']['bias_actual_minus_predicted']} |",
                f"| Assists | {metrics['assists']['mae']} | {metrics['assists']['rmse']} | {metrics['assists']['bias_actual_minus_predicted']} |",
                f"| xG | {metrics['xg']['mae']} | {metrics['xg']['rmse']} | — |",
                f"| xA | {metrics['xa']['mae']} | {metrics['xa']['rmse']} | — |",
            ]
        )
    else:
        lines.append("| _(no evaluations)_ | | | |")
    lines.extend(
        [
            "",
            "## Notes",
            "",
            "- See [`docs/prediction-validation.md`](../../docs/prediction-validation.md) for methodology.",
            "- Negative bias means the model over-predicted on average.",
            "",
        ]
    )
    md_path.write_text("\n".join(lines), encoding="utf-8")
    return json_path, md_path


def main() -> int:
    args = parse_args()
    print("Kleos prediction validation")
    print(f"  season: {args.season}")
    print(f"  api:    {args.api_url}{' (dry-run)' if args.dry_run else ''}")
    print(f"  min minutes: {args.min_minutes}")
    print(f"  club change: {args.require_club_change}")
    print(f"  limit:  {args.limit}")

    candidates = discover_candidates(
        args.db,
        args.season,
        args.min_minutes,
        args.require_club_change,
        args.limit,
    )
    print(f"  candidates: {len(candidates)}")
    if args.dry_run:
        for row in candidates[:20]:
            print(f"    {row.player_name} -> {row.club_name} ({row.minutes} min)")
        if len(candidates) > 20:
            print(f"    … {len(candidates) - 20} more")
        return 0

    metrics = Metrics()
    failures: list[dict[str, str]] = []
    samples: list[dict[str, Any]] = []
    model_version = "v0-heuristic"

    for index, candidate in enumerate(candidates, start=1):
        note = f"backtest:{args.season}:{index}"
        try:
            created = http_json(
                "POST",
                f"{args.api_url.rstrip('/')}/api/v1/predictions",
                {
                    "playerId": candidate.player_id,
                    "targetClubId": candidate.club_id,
                    "seasonId": candidate.season_id,
                    "note": note,
                },
            )
            model_version = created.get("modelVersion") or model_version
            evaluated = http_json(
                "POST",
                f"{args.api_url.rstrip('/')}/api/v1/predictions/{created['id']}/evaluate",
            )
            evaluation = evaluated["evaluation"]
            metrics.add(evaluation)
            if len(samples) < 10:
                samples.append(
                    {
                        "player": candidate.player_name,
                        "club": candidate.club_name,
                        "predictedMinutes": created["predictedMinutes"],
                        "actualMinutes": evaluation["actualMinutes"],
                        "minutesError": evaluation["minutesError"],
                        "predictedGoals": created["predictedGoals"],
                        "actualGoals": evaluation["actualGoals"],
                    }
                )
            if index % 25 == 0 or index == len(candidates):
                print(f"  progress {index}/{len(candidates)}")
        except Exception as error:  # noqa: BLE001 — collect and continue batch
            failures.append({"player": candidate.player_name, "error": str(error)[:300]})
            print(f"  fail[{index}] {candidate.player_name}: {error}")

    payload = {
        "generatedAt": datetime.now(timezone.utc).isoformat(),
        "season": args.season,
        "modelVersion": model_version,
        "selection": {
            "minMinutes": args.min_minutes,
            "requireClubChange": args.require_club_change,
            "limit": args.limit,
            "candidatesFound": len(candidates),
            "evaluated": metrics.n,
            "failed": len(failures),
        },
        "metrics": metrics.summary(),
        "samplePredictions": samples,
        "failures": failures[:20],
    }
    print(json.dumps(payload["metrics"], indent=2))
    if metrics.n == 0:
        print("No evaluations succeeded — refusing to overwrite research/validation artifacts.", file=sys.stderr)
        if failures:
            print(f"First failure: {failures[0]}", file=sys.stderr)
        return 1
    json_path, md_path = write_report(args.out_dir, args.season, payload)
    print(f"Wrote {json_path}")
    print(f"Wrote {md_path}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
