#!/usr/bin/env python3
"""#65 spot-check: PL exact-role competition via live predictions."""

from __future__ import annotations

import json
import pathlib
import urllib.request

API = "http://localhost:8080"
SEASON_2025 = "7292522f-d354-4dc2-b684-5a20dbfb372d"  # 2025/26


def get(path: str):
    with urllib.request.urlopen(API + path, timeout=60) as response:
        return json.loads(response.read())


def post(path: str, body: dict):
    request = urllib.request.Request(
        API + path,
        data=json.dumps(body).encode(),
        headers={"Content-Type": "application/json", "Accept": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(request, timeout=120) as response:
        return json.loads(response.read())


def main() -> None:
    clubs: dict[str, str] = {}
    page = 0
    while True:
        payload = get(f"/api/v1/clubs?page={page}&size=200")
        for club in payload.get("content", []):
            clubs[club["name"]] = club["id"]
        if payload.get("last", True) or page > 20:
            break
        page += 1

    def club(*names: str) -> str | None:
        for name in names:
            if name in clubs:
                return clubs[name]
        return None

    pairs = [
        ("Matty Cash RB → Arsenal", "368a7800-56c8-4886-a264-aa4b3eb4c780", club("Arsenal")),
        ("Matty Cash RB → Liverpool", "368a7800-56c8-4886-a264-aa4b3eb4c780", club("Liverpool")),
        (
            "Tyrick Mitchell LB → Man City",
            "781a3a88-88ea-4203-a6ca-c09b2813daf7",
            club("Manchester City"),
        ),
        ("Pedro Porro RB → Chelsea", "98a5ada8-6b3e-4162-9ba7-a4d725d381ca", club("Chelsea")),
        (
            "Malo Gusto RB → Tottenham",
            "0e1a2751-b342-4686-9c73-06fa541114e2",
            club("Tottenham", "Tottenham Hotspur"),
        ),
        (
            "Lewis Hall LB → Man Utd",
            "a9caafb3-1123-414b-a340-ccfc1d4fee2a",
            club("Manchester Utd", "Manchester United"),
        ),
    ]

    results = []
    for label, player_id, club_id in pairs:
        if not club_id:
            print(f"skip missing club: {label}")
            continue
        try:
            pred = post(
                "/api/v1/predictions",
                {
                    "playerId": player_id,
                    "targetClubId": club_id,
                    "seasonId": SEASON_2025,
                    "note": "#65 preciseRoles spot-check",
                },
            )
        except Exception as error:  # noqa: BLE001
            print(f"FAIL {label}: {error}")
            continue

        expl = pred.get("explanations") or []
        if isinstance(expl, dict):
            expl = expl.get("items") or expl.get("factors") or []
        codes = []
        texts = []
        for item in expl:
            if isinstance(item, dict):
                codes.append(item.get("code") or item.get("factorCode") or "")
                texts.append(item.get("message") or item.get("text") or item.get("detail") or "")

        has_role_prec = "ROLE_PRECISION" in codes
        role_msgs = [
            text
            for text in texts
            if text
            and any(
                token in text.lower()
                for token in ("role", "rb", "lb", "exact", "flank", "right-back", "left-back")
            )
        ]
        minutes = pred.get("predictedMinutes") or pred.get("minutes")
        if minutes is None and isinstance(pred.get("prediction"), dict):
            minutes = pred["prediction"].get("predictedMinutes")

        row = {
            "label": label,
            "minutes": minutes,
            "ROLE_PRECISION": has_role_prec,
            "codes": [code for code in codes if code][:12],
            "role_msgs": role_msgs[:3],
            "id": pred.get("id"),
        }
        results.append(row)
        print(
            f"{label}: min={minutes} ROLE_PRECISION={has_role_prec} "
            f"codes={[code for code in codes if code][:8]}"
        )
        for message in role_msgs[:2]:
            print("  ", message[:160])

    absent = sum(1 for row in results if not row["ROLE_PRECISION"])
    print(
        f"\nSummary: {len(results)} preds, ROLE_PRECISION absent on "
        f"{absent}/{len(results)} (want absent = precise path active)"
    )
    out = pathlib.Path("/tmp/kleos-65-spotcheck.json")
    out.write_text(json.dumps(results, indent=2))
    print("wrote", out)


if __name__ == "__main__":
    main()
