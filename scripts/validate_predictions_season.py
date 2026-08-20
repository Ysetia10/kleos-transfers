#!/usr/bin/env python3
"""Backtest Kleos predictions against completed PlayerSeason outcomes.

For each club-changer in a completed season:
  1. POST /api/v1/predictions  (engine as-of season start — no outcome leak)
  2. Reuse evaluation from create when present, else POST …/evaluate
  3. Aggregate MAE / RMSE / bias overall and **by destination league**

Examples:
  ./scripts/validate_predictions_season.py --season 2024/25 --dry-run
  ./scripts/validate_predictions_season.py --seasons 2022/23,2023/24,2024/25 --per-league-limit 40
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
from dataclasses import dataclass, field
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_OUT_DIR = ROOT / "research" / "validation"

LEAGUE_LABELS = {
    "ENG": "Premier League",
    "ESP": "La Liga",
    "GER": "Bundesliga",
    "ITA": "Serie A",
    "FRA": "Ligue 1",
}

DEFAULT_COUNTRIES = list(LEAGUE_LABELS.keys())


@dataclass
class Candidate:
    player_id: str
    player_name: str
    club_id: str
    club_name: str
    season_id: str
    season_label: str
    country_code: str
    league_name: str
    position: str
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

    def add(self, evaluation: dict[str, Any]) -> None:
        self.n += 1
        me = float(evaluation["minutesError"])
        ge = float(evaluation["goalsError"])
        ae = float(evaluation["assistsError"])
        self.minutes_abs += abs(me)
        self.minutes_sq += me * me
        self.minutes_bias += me
        self.goals_abs += abs(ge)
        self.goals_sq += ge * ge
        self.goals_bias += ge
        self.assists_abs += abs(ae)
        self.assists_sq += ae * ae
        self.assists_bias += ae

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
        }


@dataclass
class RunBucket:
    metrics: Metrics = field(default_factory=Metrics)
    samples: list[dict[str, Any]] = field(default_factory=list)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--api-url", default=os.environ.get("KLEOS_API_URL", "http://localhost:8080"))
    parser.add_argument("--season", default="", help="Single completed season label (e.g. 2024/25)")
    parser.add_argument(
        "--seasons",
        default="",
        help="Comma-separated seasons (overrides --season). Empty = every completed season with PlayerSeason data",
    )
    parser.add_argument("--min-minutes", type=int, default=900, help="Minimum actual minutes in target season")
    parser.add_argument(
        "--require-club-change",
        action=argparse.BooleanOptionalAction,
        default=True,
        help="Only players whose most recent prior club differs from the target club",
    )
    parser.add_argument("--limit", type=int, default=0, help="Max candidates overall (0 = no overall cap)")
    parser.add_argument(
        "--per-league-limit",
        type=int,
        default=0,
        help="Max candidates per destination league per season (0 = all eligible club-changers)",
    )
    parser.add_argument(
        "--max-samples-per-league",
        type=int,
        default=0,
        help="Max sample rows kept per league in the published artifact (0 = keep all evaluated)",
    )
    parser.add_argument(
        "--countries",
        default=",".join(DEFAULT_COUNTRIES),
        help="Comma-separated destination club country codes (default: ENG,ESP,GER,ITA,FRA)",
    )
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
    parser.add_argument(
        "--publish-latest",
        action=argparse.BooleanOptionalAction,
        default=True,
        help="Also write research/validation/latest.json for the product API (default: on)",
    )
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
        with urllib.request.urlopen(request, timeout=90) as response:
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
    per_league_limit: int,
    countries: list[str],
) -> list[Candidate]:
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

    safe_countries = ",".join("'" + code.replace("'", "") + "'" for code in countries)
    rank_filter = "TRUE" if per_league_limit <= 0 else f"league_rank <= {int(per_league_limit)}"
    sql = f"""
    SELECT COALESCE(json_agg(row_to_json(t)), '[]'::json)
    FROM (
      SELECT * FROM (
        SELECT
          p.id::text AS player_id,
          p.full_name AS player_name,
          c.id::text AS club_id,
          c.name AS club_name,
          s.id::text AS season_id,
          s.label AS season_label,
          c.country_code,
          CASE c.country_code
            WHEN 'ENG' THEN 'Premier League'
            WHEN 'ESP' THEN 'La Liga'
            WHEN 'GER' THEN 'Bundesliga'
            WHEN 'ITA' THEN 'Serie A'
            WHEN 'FRA' THEN 'Ligue 1'
            ELSE c.country_code
          END AS league_name,
          COALESCE(NULLIF(TRIM(ps.primary_position), ''), p.primary_position) AS position,
          ps.minutes_played AS minutes,
          ps.goals,
          ps.assists,
          ROW_NUMBER() OVER (
            PARTITION BY c.country_code
            ORDER BY ps.minutes_played DESC, p.full_name
          ) AS league_rank
        FROM player_seasons ps
        JOIN players p ON p.id = ps.player_id AND p.deleted_at IS NULL
        JOIN clubs c ON c.id = ps.club_id AND c.deleted_at IS NULL
        JOIN seasons s ON s.id = ps.season_id AND s.deleted_at IS NULL
        WHERE s.label = '{season_label}'
          AND ps.deleted_at IS NULL
          AND ps.minutes_played >= {int(min_minutes)}
          AND c.country_code IN ({safe_countries})
          AND EXISTS (
            SELECT 1
            FROM player_seasons hist
            JOIN seasons hist_s ON hist_s.id = hist.season_id
            WHERE hist.player_id = ps.player_id
              AND hist_s.start_date < s.start_date
              AND hist.deleted_at IS NULL
          )
          {club_change_sql}
      ) ranked
      WHERE {rank_filter}
      ORDER BY country_code, minutes DESC
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
            season_label=row["season_label"],
            country_code=row["country_code"],
            league_name=row["league_name"],
            position=str(row.get("position") or ""),
            minutes=int(row["minutes"]),
            goals=int(row["goals"]),
            assists=int(row["assists"]),
        )
        for row in payload
    ]


def discover_completed_seasons(db_url: str, countries: list[str], min_minutes: int) -> list[str]:
    """Seasons with club-changer outcomes in the top-5 leagues (excludes seasons still in progress)."""
    safe_countries = ",".join("'" + code.replace("'", "") + "'" for code in countries)
    sql = f"""
    SELECT COALESCE(json_agg(label ORDER BY start_date), '[]'::json)
    FROM (
      SELECT s.label, s.start_date
      FROM seasons s
      WHERE s.deleted_at IS NULL
        AND s.end_date < CURRENT_DATE
        AND EXISTS (
          SELECT 1
          FROM player_seasons ps
          JOIN clubs c ON c.id = ps.club_id AND c.deleted_at IS NULL
          WHERE ps.season_id = s.id
            AND ps.deleted_at IS NULL
            AND ps.minutes_played >= {int(min_minutes)}
            AND c.country_code IN ({safe_countries})
            AND EXISTS (
              SELECT 1
              FROM player_seasons hist
              JOIN seasons hist_s ON hist_s.id = hist.season_id
              WHERE hist.player_id = ps.player_id
                AND hist_s.start_date < s.start_date
                AND hist.deleted_at IS NULL
            )
        )
    ) t
    """
    completed = subprocess.run(
        ["psql", db_url, "-v", "ON_ERROR_STOP=1", "-t", "-A", "-c", sql],
        check=True,
        capture_output=True,
        text=True,
    )
    return list(json.loads(completed.stdout.strip() or "[]"))


def metric_table_lines(metrics: dict[str, Any]) -> list[str]:
    if not metrics.get("n"):
        return ["| _(no evaluations)_ | | | |"]
    return [
        f"| Minutes | {metrics['minutes']['mae']} | {metrics['minutes']['rmse']} | {metrics['minutes']['bias_actual_minus_predicted']} |",
        f"| Goals | {metrics['goals']['mae']} | {metrics['goals']['rmse']} | {metrics['goals']['bias_actual_minus_predicted']} |",
        f"| Assists | {metrics['assists']['mae']} | {metrics['assists']['rmse']} | {metrics['assists']['bias_actual_minus_predicted']} |",
    ]


def write_report(out_dir: Path, label: str, payload: dict[str, Any]) -> tuple[Path, Path]:
    out_dir.mkdir(parents=True, exist_ok=True)
    stamp = label.replace("/", "-").replace(",", "_")
    model_slug = str(payload.get("modelVersion") or "model").replace("/", "-")
    json_path = out_dir / f"{model_slug}-{stamp}.json"
    md_path = out_dir / f"{model_slug}-{stamp}.md"
    json_path.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")

    metrics = payload["metrics"]
    lines = [
        f"# Prediction validation — {label}",
        "",
        f"Generated: `{payload['generatedAt']}`",
        f"Model: `{payload['modelVersion']}`",
        f"Sample size: **{metrics.get('n', 0)}** evaluated predictions",
        "",
        "## Selection",
        "",
        f"- Min actual minutes: `{payload['selection']['minMinutes']}`",
        f"- Require prior club change: `{payload['selection']['requireClubChange']}`",
        f"- Per-league limit: `{payload['selection']['perLeagueLimit']}`",
        f"- Countries: `{', '.join(payload['selection']['countries'])}`",
        f"- Seasons: `{', '.join(payload['selection']['seasons'])}`",
        "",
        "## Overall error metrics (actual − predicted)",
        "",
        "| Metric | MAE | RMSE | Bias |",
        "|--------|-----|------|------|",
        *metric_table_lines(metrics),
        "",
        "## By league",
        "",
    ]
    by_league = payload.get("byLeague") or {}
    for code in sorted(by_league.keys()):
        league = by_league[code]
        lines.extend(
            [
                f"### {league.get('leagueName', code)} (`{code}`)",
                "",
                f"n = **{league['metrics'].get('n', 0)}**",
                "",
                "| Metric | MAE | RMSE | Bias |",
                "|--------|-----|------|------|",
                *metric_table_lines(league["metrics"]),
                "",
            ]
        )
        samples = league.get("samples") or []
        if samples:
            lines.append("Example club-changers:")
            lines.append("")
            for sample in samples[:5]:
                lines.append(
                    f"- **{sample['player']}** → {sample['club']} ({sample['season']}): "
                    f"pred {sample['predictedMinutes']} min / {sample['predictedGoals']} G / {sample['predictedAssists']} A; "
                    f"actual {sample['actualMinutes']} min / {sample['actualGoals']} G / {sample['actualAssists']} A"
                )
            lines.append("")

    lines.extend(
        [
            "## Notes",
            "",
            "- See [`docs/prediction-validation.md`](../../docs/prediction-validation.md) for methodology.",
            "- Negative bias means the model over-predicted on average.",
            "- Cohort is destination-league club-changers with prior history (as-of season start).",
            "",
        ]
    )
    md_path.write_text("\n".join(lines), encoding="utf-8")
    return json_path, md_path


def resolve_evaluation(created: dict[str, Any], api_url: str) -> dict[str, Any]:
    evaluation = created.get("evaluation")
    if evaluation:
        return evaluation
    evaluated = http_json(
        "POST",
        f"{api_url.rstrip('/')}/api/v1/predictions/{created['id']}/evaluate",
    )
    return evaluated["evaluation"]


def main() -> int:
    args = parse_args()
    countries = [c.strip().upper() for c in args.countries.split(",") if c.strip()]
    if not countries:
        countries = DEFAULT_COUNTRIES

    seasons = [s.strip() for s in args.seasons.split(",") if s.strip()]
    if not seasons and args.season.strip():
        seasons = [args.season.strip()]
    if not seasons:
        seasons = discover_completed_seasons(args.db, countries, args.min_minutes)
        if not seasons:
            raise SystemExit("No completed seasons with PlayerSeason data found")

    print("Kleos prediction validation")
    print(f"  seasons: {', '.join(seasons)}")
    print(f"  api:     {args.api_url}{' (dry-run)' if args.dry_run else ''}")
    print(f"  min minutes: {args.min_minutes}")
    print(f"  club change: {args.require_club_change}")
    print(f"  per-league limit: {args.per_league_limit or 'all'}")
    print(f"  max samples/league: {args.max_samples_per_league or 'all'}")
    print(f"  countries: {','.join(countries)}")

    candidates: list[Candidate] = []
    for season in seasons:
        season_rows = discover_candidates(
            args.db,
            season,
            args.min_minutes,
            args.require_club_change,
            args.per_league_limit,
            countries,
        )
        print(f"  {season}: {len(season_rows)} candidates")
        candidates.extend(season_rows)

    if args.limit and args.limit > 0:
        candidates = candidates[: args.limit]
    print(f"  total candidates: {len(candidates)}")

    if args.dry_run:
        for row in candidates[:30]:
            print(
                f"    [{row.country_code}] {row.player_name} ({row.position}) -> {row.club_name} "
                f"({row.season_label}, {row.minutes} min)"
            )
        if len(candidates) > 30:
            print(f"    … {len(candidates) - 30} more")
        return 0

    overall = RunBucket()
    by_league: dict[str, RunBucket] = {code: RunBucket() for code in countries}
    failures: list[dict[str, str]] = []
    model_version = "v0.3-heuristic"
    max_samples = args.max_samples_per_league

    for index, candidate in enumerate(candidates, start=1):
        note = f"backtest:{candidate.season_label}:{candidate.country_code}:{index}"
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
            evaluation = resolve_evaluation(created, args.api_url)
            overall.metrics.add(evaluation)
            bucket = by_league.setdefault(candidate.country_code, RunBucket())
            bucket.metrics.add(evaluation)
            sample = {
                "player": candidate.player_name,
                "playerId": candidate.player_id,
                "position": candidate.position,
                "club": candidate.club_name,
                "clubId": candidate.club_id,
                "season": candidate.season_label,
                "league": candidate.league_name,
                "countryCode": candidate.country_code,
                "predictedMinutes": created["predictedMinutes"],
                "actualMinutes": evaluation["actualMinutes"],
                "minutesError": evaluation["minutesError"],
                "predictedGoals": created["predictedGoals"],
                "actualGoals": evaluation["actualGoals"],
                "goalsError": evaluation["goalsError"],
                "predictedAssists": created["predictedAssists"],
                "actualAssists": evaluation["actualAssists"],
                "assistsError": evaluation["assistsError"],
                "predictionId": created["id"],
            }
            if len(overall.samples) < 24:
                overall.samples.append(sample)
            if max_samples <= 0 or len(bucket.samples) < max_samples:
                bucket.samples.append(sample)
            if index % 50 == 0 or index == len(candidates):
                print(f"  progress {index}/{len(candidates)}")
        except Exception as error:  # noqa: BLE001 — collect and continue batch
            failures.append({"player": candidate.player_name, "error": str(error)[:300]})
            print(f"  fail[{index}] {candidate.player_name}: {error}")

    by_league_payload = {
        code: {
            "countryCode": code,
            "leagueName": LEAGUE_LABELS.get(code, code),
            "metrics": bucket.metrics.summary(),
            "samples": bucket.samples,
        }
        for code, bucket in sorted(by_league.items())
        if bucket.metrics.n > 0 or code in countries
    }

    payload = {
        "generatedAt": datetime.now(timezone.utc).isoformat(),
        "seasons": seasons,
        "modelVersion": model_version,
        "selection": {
            "minMinutes": args.min_minutes,
            "requireClubChange": args.require_club_change,
            "perLeagueLimit": args.per_league_limit,
            "limit": args.limit,
            "countries": countries,
            "seasons": seasons,
            "candidatesFound": len(candidates),
            "evaluated": overall.metrics.n,
            "failed": len(failures),
        },
        "metrics": overall.metrics.summary(),
        "byLeague": by_league_payload,
        "samplePredictions": overall.samples,
        "failures": failures[:30],
    }
    print(json.dumps(payload["metrics"], indent=2))
    if overall.metrics.n == 0:
        print("No evaluations succeeded — refusing to overwrite research/validation artifacts.", file=sys.stderr)
        if failures:
            print(f"First failure: {failures[0]}", file=sys.stderr)
        return 1

    label = "_".join(s.replace("/", "-") for s in seasons)
    json_path, md_path = write_report(args.out_dir, label, payload)
    print(f"Wrote {json_path}")
    print(f"Wrote {md_path}")
    if args.publish_latest:
        latest = args.out_dir / "latest.json"
        latest.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")
        print(f"Wrote {latest}")
        # Product API classpath copy
        resource_dir = ROOT / "backend" / "src" / "main" / "resources" / "model-accuracy"
        resource_dir.mkdir(parents=True, exist_ok=True)
        resource_path = resource_dir / "latest.json"
        resource_path.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")
        print(f"Wrote {resource_path}")
        # Live bootRun classpath (src/main/resources is not reloaded until rebuild)
        build_resource = ROOT / "backend" / "build" / "resources" / "main" / "model-accuracy" / "latest.json"
        if build_resource.parent.exists() or (ROOT / "backend" / "build" / "resources" / "main").exists():
            build_resource.parent.mkdir(parents=True, exist_ok=True)
            build_resource.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")
            print(f"Wrote {build_resource}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
