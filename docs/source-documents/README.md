# Source Documents (Requirements Inputs)

BDOI's requirement documents and programme inputs. They are the baseline that BIBS (iNXT BrokerVerse for BDOI) is
built and traced against. Each BRD has its analysis in `docs/requirements/` and its FRS in
`docs/deliverables/src/frs/`.

| Document | Analysis |
|---|---|
| `00 - BRD BDOI Core Replacement v01.pdf` (umbrella BRD, 48 pages) | `docs/requirements/BDOI_CORE_BRD_SPEC.md`, `docs/architecture/CORE_REPLACEMENT_IMPACT.md` |
| `New Business (NB) BRD.pdf` (BRD-1) | `docs/requirements/BDOI_NB_BRD_SPEC.md` |
| `Operations.pdf` (BRD-2) | `docs/requirements/BDOI_OPS_BRD_SPEC.md` |
| `Product Maintenance.pdf` (BRD-3) | `docs/requirements/BDOI_PM_BRD_SPEC.md` |
| `Collections (CLXN) BRD.pdf` (BRD-4) | `docs/requirements/BDOI_CLXN_BRD_SPEC.md` |
| `Accounting, Disbursement, and Accounting Controls and Subsidiary Ledger BRD.zip` (BRD-5) | `docs/requirements/BDOI_ACCT_BRD_SPEC.md` |
| `Renewal (RN) BRD.pdf` (BRD-6) | `docs/requirements/BDOI_RN_BRD_SPEC.md` |
| `Concept Paper - Advance Implementation of Renewal Processing V1.0 (signed).pdf` (signed 06-Sep-2026) | `BDOI_DROP_PLAN.md` |
| `Claims (CLM).PDF` (BRD-7) | `docs/requirements/BDOI_CLM_BRD_SPEC.md` |
| `Employee Benefits.pdf` (BRD-8) | `docs/requirements/BDOI_EB_BRD_SPEC.md` |
| `Customer Servicing Facility.PDF` (BRD-9) | `docs/requirements/BDOI_CSF_BRD_SPEC.md` |
| `Sanction Screening and Risk Profiling BRD.pdf` (BRD-10) | `docs/requirements/BDOI_SANC_BRD_SPEC.md` |
| `User Access Maintenance.pdf` (BRD-11) | `docs/requirements/BDOI_UAM_BRD_SPEC.md` |
| `BRD - Submitted Policies (with e-sig MCM 4.24.2026).pdf` (BRD-12) | `docs/requirements/BDOI_SP_BRD_SPEC.md` |
| `BRD - Data Migration - draft V0.01.pdf` (BRD-13, 16 pages) | `docs/requirements/BDOI_DM_BRD_SPEC.md` |
| `ReInsurance (Phase 2).PDF` | Phase 2, not in the phase 1 build; seams in `docs/architecture/CORE_REPLACEMENT_IMPACT.md` |
| `Report List as of APR-27-2026.pdf` | `docs/requirements/BDOI_REPORT_LIST.md` |
| `Annexure 2 (b) Reports_Book_Finance.pdf` | `docs/requirements/FINANCE_REPORTS_SPEC.md` |
| `BDOI_UXD.docx` (BDO Insure UX design) | `docs/design/BDO_UX_GUIDELINES.md` |
| `BDOI Drop Plan - modules and integrations.webp`, `BDOI Programme Timeline.webp` | `BDOI_DROP_PLAN.md` |
| `BDOI Integration Systems 1.png`, `BDOI Integration Systems 2.png` | `docs/architecture/CORE_REPLACEMENT_IMPACT.md` |
| `BDOI_IER_Workbook_v20_iorta.xlsx` (infrastructure estimate) | `BDOI_DROP_PLAN.md` |

## How to upload from the browser

1. Open this folder on GitHub (branch `iortatechnxt/zen-einstein-ew7cvm`).
2. Click **Add file → Upload files**.
3. Drag the file in (GitHub web upload accepts files up to 25 MB; for larger files zip it or
   split it into parts, e.g. `Finance_Reports_Book_part1.pdf`).
4. Commit directly to the same branch.

Keep one copy of each document, in this folder only. When BDOI issues a new version of a document, replace the file
(git keeps the earlier version) and update the analysis named above.
