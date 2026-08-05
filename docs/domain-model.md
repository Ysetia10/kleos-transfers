# Kleos Transfers Domain Model

This document is the single source of truth for the Kleos Transfers domain model. It records agreed concepts, their responsibilities, and the questions that must be resolved before implementation.

## Domain Modeling Principles

Kleos Transfers separates its domain into three categories:

1. **Identity Entities** — permanent information that rarely changes.
2. **Historical Entities** — information that changes every season.
3. **Prediction Entities** — Kleos-specific entities used for prediction and evaluation.

The model follows these principles:

- Identity and historical data must never be mixed.
- Everything that changes over time belongs in a historical entity.
- Every entity must exist because it helps answer a business question.
- Avoid duplicate data whenever possible.
- Relationships should be explicit and meaningful.
- Keep the model extensible for future prediction versions.

## Player

### Purpose

_To be finalized._

### Attributes

_To be finalized._

### Relationships

_To be finalized._

### Notes

_To be finalized._

## Club

### Purpose

_To be finalized._

### Attributes

_To be finalized._

### Relationships

_To be finalized._

### Notes

_To be finalized._

## Manager

### Purpose

_To be finalized._

### Attributes

_To be finalized._

### Relationships

_To be finalized._

### Notes

_To be finalized._

## Season

### Purpose

_To be finalized._

### Attributes

_To be finalized._

### Relationships

_To be finalized._

### Notes

_To be finalized._

## Tournament

### Purpose

_To be finalized._

### Attributes

_To be finalized._

### Relationships

_To be finalized._

### Notes

_To be finalized._

## PlayerSeason

### Purpose

_To be finalized._

### Attributes

_To be finalized._

### Relationships

_To be finalized._

### Notes

_To be finalized._

## ClubSeason

### Purpose

_To be finalized._

### Attributes

_To be finalized._

### Relationships

_To be finalized._

### Notes

_To be finalized._

## Transfer

### Purpose

_To be finalized._

### Attributes

_To be finalized._

### Relationships

_To be finalized._

### Notes

_To be finalized._

## Prediction

### Purpose

_To be finalized._

### Attributes

_To be finalized._

### Relationships

_To be finalized._

### Notes

_To be finalized._

## Contract

### Purpose

_To be finalized._

### Attributes

_To be finalized._

### Relationships

_To be finalized._

### Notes

_To be finalized._

## Injury

### Purpose

_To be finalized._

### Attributes

_To be finalized._

### Relationships

_To be finalized._

### Notes

_To be finalized._

## National Team

### Purpose

_To be finalized._

### Attributes

_To be finalized._

### Relationships

_To be finalized._

### Notes

_To be finalized._

## Open Questions

Record future design decisions here before implementation, including entity ownership, identifiers, cardinality, temporal boundaries, data provenance, and prediction-version requirements.
