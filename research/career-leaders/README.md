# League career leaders (curated)

Small, attributed top-N career goal / assist tables for Premier League and La Liga.

## Source

- Wikipedia list articles (public encyclopedia facts), compiled into CSV for Kleos ingest.
- Totals are **as of the `as_of_date` column** and will drift until refreshed.
- Do **not** treat these as Transfermarkt scrapes; values are hand-checked encyclopedia figures.

## Files

- `premier_league_goals.csv`
- `premier_league_assists.csv`
- `la_liga_goals.csv`
- `la_liga_assists.csv`

Import with:

```bash
python3 scripts/import_career_leaders.py
```
