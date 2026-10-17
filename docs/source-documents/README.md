# Source Documents (Requirements Inputs)

BDOI's requirement documents and programme inputs. They are the baseline that BIBS (iNXT BrokerVerse for BDOI) is
built and traced against. Each BRD has its analysis in `docs/requirements/` and its FRS in
`docs/deliverables/src/frs/`.

| Document | Drop | Analysis |
|---|---|---|
| `00 - BRD BDOI Core Replacement v01.pdf` (umbrella BRD, 48 pages) | Programme | `docs/requirements/BDOI_CORE_BRD_SPEC.md`, `docs/architecture/CORE_REPLACEMENT_IMPACT.md` |
| `New Business (NB) BRD 05132026.pdf` (BRD-1, with the addendum of 13-May-2026) | Drop 1 | `docs/requirements/BDOI_NB_BRD_SPEC.md` |
| `Operations_WS Addendum.pdf` (BRD-2: topic annexes, re-issued BRD v1.01 and the original BRD) | Drop 1 (Production Reconciliation: Drop 2) | `docs/requirements/BDOI_OPS_BRD_SPEC.md` |
| `Product Maintenance.pdf` (BRD-3) | Drop 0 (quotation use: Drop 1) | `docs/requirements/BDOI_PM_BRD_SPEC.md` |
| `Collections (CLXN)_WS Addendum.pdf` (BRD-4, with the workshop minutes) | Drop 1 (direct-payment commission); Drop 2 (Marketing Collection extraction) | `docs/requirements/BDOI_CLXN_BRD_SPEC.md` |
| `Accounting, Disbursement, and Accounting Controls and Subsidiary Ledger_WS Addendum v04152026.zip` (BRD-5, with the workshop minutes) | Drop 1 (GL accounts: Drop 0) | `docs/requirements/BDOI_ACCT_BRD_SPEC.md` |
| `Renewal (RN) BRD.pdf` (BRD-6, with the Renewal Addendum of May 2026) | Drop 1 | `docs/requirements/BDOI_RN_BRD_SPEC.md` |
| `Concept Paper - Advance Implementation of Renewal Processing V1.0 (signed).pdf` (signed 06-Sep-2026) | Superseded (single go-live, January 2028) | `BDOI_DROP_PLAN.md` |
| `Claims (CLM)_WS Addendum.pdf` (BRD-7, with the workshop minutes) | Drop 2 | `docs/requirements/BDOI_CLM_BRD_SPEC.md` |
| `Employee Benefits.pdf` (BRD-8) | Drop 2 (no portal; EB placement and reports: Drop 1) | `docs/requirements/BDOI_EB_BRD_SPEC.md` |
| `Customer Servicing Facility_Case Management.PDF` (BRD-9, with the Case Management addendum) | Drop 1 (service requests proposed for Drop 2) | `docs/requirements/BDOI_CSF_BRD_SPEC.md` |
| `Sanction Screening and Risk Profiling BRD v04172026.pdf` (BRD-10) | Drop 1 (proposed, with client onboarding) | `docs/requirements/BDOI_SANC_BRD_SPEC.md` |
| `User Access Maintenance.pdf` (BRD-11) | Drop 0 | `docs/requirements/BDOI_UAM_BRD_SPEC.md` |
| `Business Requirements Document (BRD) Template - Submitted Policies.pdf` (BRD-12, approved re-issue) | Drop 1 | `docs/requirements/BDOI_SP_BRD_SPEC.md` |
| `BDO Insure Core Modernization - Data Migration - Business Requirements Document (BRD) V0.03.pdf` (BRD-13, signed, 20 pages) | Drop 0 | `docs/requirements/BDOI_DM_BRD_SPEC.md` |
| `ReInsurance (Phase 2).PDF` | Phase 2 (reinsurance transactions through Remittance in Drop 1) | Phase 2, not in the phase 1 build; seams in `docs/architecture/CORE_REPLACEMENT_IMPACT.md` |
| `BDOI - CoreModernization Report List 05132026.pdf` | Programme (each report with the drop of its module) | `docs/requirements/BDOI_REPORT_LIST.md` |
| `Annexure 2 (b) Reports_Book_Finance.pdf` | Drop 1 | `docs/requirements/FINANCE_REPORTS_SPEC.md` |
| `BDOI_UXD.docx` (BDO Insure UX design) | Programme | `docs/design/BDO_UX_GUIDELINES.md` |
| `BDOI Drop Plan - modules and integrations.webp`, `BDOI Programme Timeline.webp` | Programme | `BDOI_DROP_PLAN.md` |
| `BDOI Integration Systems 1.png`, `BDOI Integration Systems 2.png` | Programme | `docs/architecture/CORE_REPLACEMENT_IMPACT.md` |
| `BDOI_IER_Workbook_v20_iorta.xlsx` (infrastructure estimate) | Programme | `BDOI_DROP_PLAN.md` |

Drop: the BDOI drop in which the document's requirements are specified, built, tested and signed off (BDOI drop
plan, `BDOI_DROP_PLAN.md`; map in `docs/architecture/PROGRAMME_ALIGNMENT.md` section 2.3). The FRS, test plans and other
deliverables of each drop are indexed in `docs/deliverables/out/<drop folder>/README.md`.

## Versions received on 8 October 2026

BDOI issued new versions of ten documents on 8 October 2026. The new files above replace the earlier ones; the
earlier versions remain in the version history (commit `35278e7`). The comparison of each old and new version is
in the source-document comparison workbooks, and the FRS of each affected BRD is being re-based on the new version
(FRS v2.1). Analyses and registers written before that date cite the earlier file names and page numbers.
`ReInsurance.PDF` and `Sanction Screening and Risk Profiling BRD v04172026.pdf` were identical to the files
already held; one copy of each is kept.

## Adding a document

Keep one copy of each document, in this folder only. When BDOI issues a new version of a document, replace the file
(the earlier version stays in the version history) and update the analysis named above. Large files may be zipped or
split into parts (for example `Finance_Reports_Book_part1.pdf`).
