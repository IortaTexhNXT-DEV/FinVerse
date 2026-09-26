# iNXT BrokerVerse for BDOI (BIBS)

The BIBS broker system of BDO Insurance and Reinsurance Brokers, Inc. (BDOI), built on iNXT BrokerVerse by
IortaTechNXT. It covers the BDOI BRDs: BRD-00 Core Replacement (umbrella), New Business (BRD-1), Operations (BRD-2),
Product Maintenance (BRD-3), Collections (BRD-4), Accounting, Disbursement and ACSL (BRD-5), Renewal (BRD-6),
Claims (BRD-7), Employee Benefits (BRD-8), Customer Servicing Facility (BRD-9), Sanction Screening (BRD-10),
User Access Maintenance (BRD-11), Submitted Policies (BRD-12) and Data Migration (BRD-13). The user interface follows
the BDO Insure visual language.

| | |
|---|---|
| Backend | Java 21, Spring Boot 3.5, PostgreSQL 16, Flyway, JWT, Redis, Kafka |
| Frontend | React 19, TypeScript, Vite, TanStack Query |
| Reports | On screen, PDF, Excel, CSV and Word (one engine for all reports) |
| Quality | Checkstyle, PMD/CPD, SpotBugs + FindSecBugs, ArchUnit, JaCoCo, SonarJS, CodeQL, SonarQube-ready |

## Quick start (demo)

```bash
docker compose up -d --build
open http://localhost:8081
```

Demo users (password `Brokerverse@2026`) are the BDOI personas, for example `ao` (Marketing account officer), `proc`
(Processing), `tsu` (TSU), `cashier`, `remittl` (Remittance team lead), `clxhandler` (Collections), `disbtl`
(Disbursement team lead), `gltl` (FRBS / GL team lead), `acsltl` (ACSL team lead) and `badmin` (business
administrator). The screenshot manifest [`tools/screenshots/screens.cjs`](tools/screenshots/screens.cjs) names the
demo user of every screen. The demo company is fictitious.

## Documentation

| Document | For |
|---|---|
| [Client deliverables: plan and status](docs/deliverables/README.md) | Project team, BDOI |
| [Requirements per BRD](docs/requirements/) (`BDOI_*_BRD_SPEC.md`, cross-BRD decisions) | Business analysts, testers |
| [Source documents](docs/source-documents/README.md) (BDOI BRDs and inputs) | Everyone |
| [Broking architecture](docs/architecture/BROKING_ARCHITECTURE.md) and the design per BRD (`docs/architecture/*_DESIGN.md`) | Everyone |
| [Platform architecture](docs/architecture/ARCHITECTURE.md) | Developers |
| [Developer guide](docs/development/DEVELOPER_GUIDE.md) | Developers (binding conventions) |
| [Codebase relevance audit](docs/development/CODEBASE_RELEVANCE_AUDIT.md) (insurer-suite code that BIBS does not use) | Developers, project lead |
| [UX guidelines](docs/design/BDO_UX_GUIDELINES.md) and [current screenshots](docs/design/screenshots/README.md) | Designers, developers, testers |
| [Quality gates](docs/quality/QUALITY_GATES.md) | Developers, QA |
| [Operations runbook](docs/operations/RUNBOOK.md) | Production support |
| [Configuration](docs/operations/CONFIGURATION.md) | Deployment |
| [Security controls](docs/security/SECURITY.md) | Security, audit |

Module guides:

| Module | Guide |
|---|---|
| Operations (BRD-2) | [docs/modules/OPERATIONS.md](docs/modules/OPERATIONS.md) |
| Sanction Screening (BRD-10) | [docs/modules/SANCTION_SCREENING.md](docs/modules/SANCTION_SCREENING.md) |
| User Access Maintenance (BRD-11) | [docs/modules/USER_ACCESS.md](docs/modules/USER_ACCESS.md) |
| Receivables & Banking (bank reconciliation, BRD-5) | [docs/modules/RECEIVABLES_AND_BANKING.md](docs/modules/RECEIVABLES_AND_BANKING.md) |
| Payables & Cash (bank accounts and cheque books used by Disbursement, BRD-5) | [docs/modules/PAYABLES_AND_CASH.md](docs/modules/PAYABLES_AND_CASH.md) |
| Assets & Investments (BRD-5 schedules) | [docs/modules/ASSETS_AND_INVESTMENTS.md](docs/modules/ASSETS_AND_INVESTMENTS.md) |
| Planning & Closing (budgets, period and year-end close, BRD-5) | [docs/development/PLANNING_AND_CLOSING.md](docs/development/PLANNING_AND_CLOSING.md) |
| Tax & Statutory (BIR, BRD-5) | [docs/modules/TAX_AND_STATUTORY.md](docs/modules/TAX_AND_STATUTORY.md) |

## Repository layout

```
backend/     Spring Boot application (modules under src/main/java/com/iortatechnxt/brokerverse)
frontend/    React application (features under src/features)
deploy/      Kubernetes manifests
docs/        Requirements, architecture and designs, deliverables, development, operations, security
tools/       Client-document generators (tools/deliverables) and the screenshot capture (tools/screenshots)
```

## Build

```bash
cd backend && mvn verify          # all quality gates + tests
cd frontend && npm ci && npm run verify && npm run build
```
