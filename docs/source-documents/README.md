# Source Documents (Requirements Inputs)

BDOI's requirement documents and programme inputs. They are the baseline that BIBS (iNXT BrokerVerse for BDOI) is
built and traced against. Each BRD has its analysis in `docs/requirements/` and its FRS in
`docs/deliverables/src/frs/`.

| Document | Drop | Analysis |
|---|---|---|
| `00 - BRD BDOI Core Replacement v01.pdf` (umbrella BRD, 48 pages) | Programme | `docs/requirements/BDOI_CORE_BRD_SPEC.md`, `docs/architecture/CORE_REPLACEMENT_IMPACT.md` |
| `New Business (NB) BRD.pdf` (BRD-1) | Drop 1 | `docs/requirements/BDOI_NB_BRD_SPEC.md` |
| `Operations.pdf` (BRD-2) | Drop 1 (Production Reconciliation: Drop 2) | `docs/requirements/BDOI_OPS_BRD_SPEC.md` |
| `Product Maintenance.pdf` (BRD-3) | Drop 0 (quotation use: Drop 1) | `docs/requirements/BDOI_PM_BRD_SPEC.md` |
| `Collections (CLXN) BRD.pdf` (BRD-4) | Drop 1 (direct-payment commission); Drop 2 (Marketing Collection extraction) | `docs/requirements/BDOI_CLXN_BRD_SPEC.md` |
| `Accounting, Disbursement, and Accounting Controls and Subsidiary Ledger BRD.zip` (BRD-5) | Drop 1 (GL accounts: Drop 0) | `docs/requirements/BDOI_ACCT_BRD_SPEC.md` |
| `Renewal (RN) BRD.pdf` (BRD-6) | Drop 1 | `docs/requirements/BDOI_RN_BRD_SPEC.md` |
| `Concept Paper - Advance Implementation of Renewal Processing V1.0 (signed).pdf` (signed 06-Sep-2026) | Superseded (single go-live, January 2028) | `BDOI_DROP_PLAN.md` |
| `Claims (CLM).PDF` (BRD-7) | Drop 2 | `docs/requirements/BDOI_CLM_BRD_SPEC.md` |
| `Employee Benefits.pdf` (BRD-8) | Drop 2 (no portal; EB placement and reports: Drop 1) | `docs/requirements/BDOI_EB_BRD_SPEC.md` |
| `Customer Servicing Facility.PDF` (BRD-9) | Drop 1 (service requests proposed for Drop 2) | `docs/requirements/BDOI_CSF_BRD_SPEC.md` |
| `Sanction Screening and Risk Profiling BRD.pdf` (BRD-10) | Drop 1 (proposed, with client onboarding) | `docs/requirements/BDOI_SANC_BRD_SPEC.md` |
| `User Access Maintenance.pdf` (BRD-11) | Drop 0 | `docs/requirements/BDOI_UAM_BRD_SPEC.md` |
| `BRD - Submitted Policies (with e-sig MCM 4.24.2026).pdf` (BRD-12) | Drop 1 | `docs/requirements/BDOI_SP_BRD_SPEC.md` |
| `BRD - Data Migration - draft V0.01.pdf` (BRD-13, 16 pages) | Drop 0 | `docs/requirements/BDOI_DM_BRD_SPEC.md` |
| `ReInsurance (Phase 2).PDF` | Phase 2 (reinsurance transactions through Remittance in Drop 1) | Phase 2, not in the phase 1 build; seams in `docs/architecture/CORE_REPLACEMENT_IMPACT.md` |
| `Report List as of APR-27-2026.pdf` | Programme (each report with the drop of its module) | `docs/requirements/BDOI_REPORT_LIST.md` |
| `Annexure 2 (b) Reports_Book_Finance.pdf` | Drop 1 | `docs/requirements/FINANCE_REPORTS_SPEC.md` |
| `BDOI_UXD.docx` (BDO Insure UX design) | Programme | `docs/design/BDO_UX_GUIDELINES.md` |
| `BDOI Drop Plan - modules and integrations.webp`, `BDOI Programme Timeline.webp` | Programme | `BDOI_DROP_PLAN.md` |
| `BDOI Integration Systems 1.png`, `BDOI Integration Systems 2.png` | Programme | `docs/architecture/CORE_REPLACEMENT_IMPACT.md` |
| `BDOI_IER_Workbook_v20_iorta.xlsx` (infrastructure estimate) | Programme | `BDOI_DROP_PLAN.md` |

Drop: the BDOI drop in which the document's requirements are specified, built, tested and signed off (BDOI drop
plan, `BDOI_DROP_PLAN.md`; map in `docs/architecture/PROGRAMME_ALIGNMENT.md` section 2.3). The FRS, test plans and other
deliverables of each drop are indexed in `docs/deliverables/out/<drop folder>/README.md`.

## Adding a document

Keep one copy of each document, in this folder only. When BDOI issues a new version of a document, replace the file
(the earlier version stays in the version history) and update the analysis named above. Large files may be zipped or
split into parts (for example `Finance_Reports_Book_part1.pdf`).
