# iNXT BrokerVerse

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

Demo users (password `Brokerverse@2026`): `fmanager` (finance manager), `accountant` (maker),
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

Module guides (business rules, accounting, reports, demo data and open points of each module):

| Module | Guide |
|---|---|
| Underwriting | [docs/modules/UNDERWRITING.md](docs/modules/UNDERWRITING.md) |
| Claims | [docs/modules/CLAIMS.md](docs/modules/CLAIMS.md) |
| Reinsurance | [docs/modules/REINSURANCE.md](docs/modules/REINSURANCE.md) |
| Receivables & Banking | [docs/modules/RECEIVABLES_AND_BANKING.md](docs/modules/RECEIVABLES_AND_BANKING.md) |
| Payables & Cash | [docs/modules/PAYABLES_AND_CASH.md](docs/modules/PAYABLES_AND_CASH.md) |
| Assets & Investments | [docs/modules/ASSETS_AND_INVESTMENTS.md](docs/modules/ASSETS_AND_INVESTMENTS.md) |
| Planning & Closing | [docs/development/PLANNING_AND_CLOSING.md](docs/development/PLANNING_AND_CLOSING.md) |
| Actuarial Reserves | [docs/modules/ACTUARIAL_RESERVES.md](docs/modules/ACTUARIAL_RESERVES.md) |
| Tax & Statutory | [docs/modules/TAX_AND_STATUTORY.md](docs/modules/TAX_AND_STATUTORY.md) |

## Repository layout

```
backend/     Spring Boot application (modules under src/main/java/com/iortatechnxt/brokerverse)
frontend/    React application (features under src/features)
deploy/      Kubernetes manifests
docs/        Architecture, development, operations, security, requirements
```

## Build

```bash
cd backend && mvn verify          # all quality gates + tests
cd frontend && npm ci && npm run verify && npm run build
```
