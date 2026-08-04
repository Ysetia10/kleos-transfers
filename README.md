# Kleos Transfers

Kleos Transfers is an open-source, context-aware football transfer prediction platform. Its long-term purpose is to estimate how a player is likely to perform after joining a specific club, before the season begins.

The project focuses on the environment around a transfer—not only a player's historical record. Future analyses will consider factors such as tactical fit, squad competition, managerial approach, fixture demands, league transition, age profile, injury history, and international commitments.

## Vision

Build an accessible and explainable decision-support platform for football transfers. Kleos Transfers should make its conclusions understandable: users should be able to see the positive and negative contextual factors behind each transfer assessment.

The project is intended to serve fans, journalists, researchers, students, developers, and smaller football organisations that may not have access to commercial scouting platforms.

## Future features

Kleos Transfers may eventually provide, for a proposed transfer:

- Expected minutes, goals, assists, xG, and xA
- Likely position or positions and starting-XI probability
- End-of-season market-value estimate
- Transfer compatibility score and prediction confidence
- Clear, evidence-based explanations of contributing factors

No prediction models or football datasets are included at this stage.

## High-level architecture

```text
Data sources and research
        |
        v
Database <--> Analytics and evaluation <--> Backend API <--> Frontend
        ^                                      |
        |                                      v
Database schema and migrations           Documentation and operations
```

The concrete technology choices, interfaces, and data contracts will be defined through project design work before implementation begins.

## Repository structure

```text
backend/    Future application services and API contracts
frontend/   Future web application and user experience
analytics/  Future analysis, evaluation, and experimentation work
database/   Database schema, migrations, and data-access documentation
docs/       Architecture, decisions, and contributor documentation
research/   Research notes, methodology, and source assessment
scripts/    Reproducible development and maintenance automation
.github/    Repository-level GitHub configuration and workflows
```

## Development roadmap

1. Define product requirements, user journeys, and success metrics.
2. Document ethical data sourcing, licensing, and data-governance standards.
3. Design the domain model, database schema, and API boundaries.
4. Establish data-ingestion and validation foundations.
5. Build an explainable analytics and evaluation framework.
6. Implement the backend, frontend, and deployment workflows.
7. Develop, validate, and transparently publish prediction methodologies.

## Getting started

This repository currently contains the project foundation only. There are no runtime services to install or run yet. See the folder-level README files for the intended responsibility of each area.

## License

Kleos Transfers is released under the [MIT License](LICENSE).
