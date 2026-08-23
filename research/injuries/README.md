# Clinical injury spells (curated)

Attributed injury spells for high-minute Big-5 players, replacing inferred
`Inferred availability gap` rows where public sources document the spell.

## Source policy

- Club statements, league injury reports, and major sports journalism (no Transfermarkt scrapes).
- Dates are best-effort **as of the `source` column**; refresh when seasons progress.
- Severity follows Kleos buckets: MINOR (&lt;21 days), MODERATE (21–89), SEVERE (≥90).

## Files

- `big5_clinical_spells.csv` — starter cohort (≥2200 min prior-season inference targets + headline spells)

## Import

```bash
python3 scripts/import-injuries.py research/injuries/big5_clinical_spells.csv
python3 scripts/prune_inferred_injuries.py --apply
```

Export more targets for manual curation:

```bash
python3 scripts/export_injury_targets.py --output research/injuries/targets.csv
```
