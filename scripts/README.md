# Scripts

Small, documented automation for development and maintenance. Scripts must not embed secrets or unlicensed data.

## `create-roadmap-issues.sh`

Idempotent helper that creates GitHub labels, milestones, and roadmap issues for Kleos Transfers.

```bash
./scripts/create-roadmap-issues.sh
```

Requires `gh` authenticated (`gh auth login`).
