# iNXT FinVerse

**Insurance General Ledger & Finance Suite** by IortaTechNXT – event-driven insurance accounting,
sub-ledgers, reinsurance, reserves, statutory and management reporting, with maker-checker control
and a complete audit trail. User interface styled after the BDO Insure visual language.

| | |
|---|---|
| Backend | Java 21, Spring Boot 3.5, PostgreSQL 16, Flyway, JWT |
| Frontend | React 19, TypeScript, Vite, TanStack Query |
| Reports | On screen, PDF, Excel, CSV (one engine for all reports) |
| Quality | Checkstyle, PMD/CPD, SpotBugs + FindSecBugs, ArchUnit, JaCoCo, SonarJS, CodeQL, SonarQube-ready |

## Quick start (demo)

```bash
docker compose up -d --build
open http://localhost:8081
```

Demo users (password `Finverse@2026`): `fmanager` (finance manager), `accountant` (maker),
`checker` (authorizer, limit 5M), `uw` (underwriter), `claims`, `reinsurer`, `auditor`, `admin`
(security administrator). The demo company is fictitious.

## Documentation

| Document | For |
|---|---|
| [Architecture](docs/architecture/ARCHITECTURE.md) | Everyone |
| [Developer guide](docs/development/DEVELOPER_GUIDE.md) | Developers (binding conventions) |
| [Quality gates](docs/quality/QUALITY_GATES.md) | Developers, QA |
| [Operations runbook](docs/operations/RUNBOOK.md) | Production support |
| [Configuration](docs/operations/CONFIGURATION.md) | Deployment |
| [Security controls](docs/security/SECURITY.md) | Security, audit |
| [Requirements](docs/requirements/) | Business analysts, testers |

## Repository layout

```
backend/     Spring Boot application (modules under src/main/java/com/iortatechnxt/finverse)
frontend/    React application (features under src/features)
deploy/      Kubernetes manifests
docs/        Architecture, development, operations, security, requirements
```

## Build

```bash
cd backend && mvn verify          # all quality gates + tests
cd frontend && npm ci && npm run verify && npm run build
```
