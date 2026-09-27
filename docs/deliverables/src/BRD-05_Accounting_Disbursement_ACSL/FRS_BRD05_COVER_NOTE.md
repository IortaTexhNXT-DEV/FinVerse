---
# Source of the cover note of the Functional Requirements Specification for BRD-5 (two volumes).
# Build: python tools/deliverables/bdoi_docx.py docs/deliverables/src/BRD-05_Accounting_Disbursement_ACSL/FRS_BRD05_COVER_NOTE.md
title: Accounting, Disbursement and ACSL - Cover Note
subtitle: BRD-5 Accounting, Disbursement, Accounting Controls and Subsidiary Ledger - Cover note to the two volumes of the FRS
doc_type: Functional Requirements Specification
doc_code: FRS
brd: BRD-05
name: Accounting Disbursement ACSL Cover Note
doc_id: BIBS-FRS-BRD-05-CN
version: "1.0"
date: 25 September 2026
status: Issued for BDOI review
header_title: FRS BRD-5 - Cover note
output: FRS/BIBS_FRS_BRD-05_Accounting_Disbursement_ACSL_Cover_Note_v1.0.docx
control:
  - version: "1.0"
    date: 25 Sep 2026
    author: iorta TechNXT Business Analysis
    reviewer: iorta TechNXT Project Manager
    approver: BDOI Comptrollership Head (pending)
    change: First issue with Volumes 1 and 2
distribution:
  - {name: "Head, Comptrollership", role: Approver, organisation: BDOI, purpose: Review and sign-off of both volumes}
  - {name: "FRBS, Disbursement, ACSL, Marketing, Business and System Administration", role: Business owners, organisation: BDOI, purpose: Review of their volume}
  - {name: Business Project Services, role: BRD owner, organisation: BDO Unibank ESG, purpose: Traceability check against the BRD}
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "Delivery, test and UAT preparation"}
---

# The two volumes

## Purpose of this note

The Functional Requirements Specification (FRS) of BRD-5 - Accounting, Disbursement, and Accounting Controls and Subsidiary Ledger - states how BIBS meets the 277 requirement IDs of the BRD and its two addenda. It is issued in two volumes. This note explains the split, lists what each volume contains, gives the references and conventions both volumes share, and sets out the sign-off route.

## Why two volumes

BRD-5 covers five business areas owned by different units: Financial Reporting and Budget (FRBS), business and system administration, Disbursement, Marketing refund and cash-advance requests, and ACSL. Written at the level of detail of the reference FRS (one FR per requirement group, with rules, validations, fields and acceptance criteria), one document would exceed 200 pages. The FRS is therefore split along the owners of the requirements, so that each unit reviews and signs one volume.

<!-- table: widths=3.4,6.4,3.2,2.2,1.4 caption="The documents of the BRD-5 FRS" -->
| Document | Content | BRD IDs | FRs | Pages |
|---|---|---|---|---|
| Cover note (this document) | Split, contents, shared references, sign-off route | - | - | - |
| Volume 1 - FRBS and Accounting | General ledger platform, chart and rates, journals, closing, revaluation, bank reconciliation, service fee, report pack, BIR outputs, business and system administration | FRBS 60, BASAU 25 (85) | 38 FR-AC | 74 |
| Volume 2 - Disbursement, Payment Requests and ACSL | Payees, request intake, disbursement vouchers, seven modes of payment, end of day, funding, tagging, CPC2 and early-incentive service invoice; Marketing refund, cash-advance and check-cancellation requests; ACSL reconciliations, investigations, corrections and remittance deductions | DIS 111, MKT 36, ACSL 45 (192) | 83 FR-DS, FR-PQ, FR-AS | 124 |

File names: `BIBS_FRS_BRD-05_Accounting_Disbursement_ACSL_Vol1_v1.0.docx` and `BIBS_FRS_BRD-05_Accounting_Disbursement_ACSL_Vol2_v1.0.docx`.

## Structure shared by both volumes

Both volumes follow the structure of the reference FRS of BRD-3:

<!-- table: widths=1,5,10.6 caption="Chapters of each volume" -->
| # | Chapter | Content |
|---|---|---|
| 1 | Introduction | Purpose, scope, references, definitions and how to read the FRs |
| 2 | Business context | Current and envisioned process; process overview with a figure |
| 3 | Personas and roles | Personas, permissions and the role-to-permission matrix |
| 4 | Functional requirements | One FR per requirement group, each citing the BRD IDs and pages |
| 5 | Workflows and statuses | A figure and a status table per workflow; accounting events |
| 6 | Reports and documents | Reports with their codes and status; document templates |
| 7 | Interfaces | Figure and table of the interfaces |
| 8 | Non-functional requirements | BRD values and the BIBS approach |
| 9 | Configuration | Parameters, jobs, lists of values, alerts, masters |
| 10 | Assumptions, dependencies, open questions | Items that need a BDOI answer (AQ numbers) |
| 11 | Traceability | Every BRD ID to its FR, screen and test cases, with a coverage summary |
| 12 | Proposed business rules and clarifications for confirmation | Each point where the proposed rule or screen differs from the BRD or needs a decision of BDOI |
| 13 | Sign-off | Signatories of the volume |

## Conventions

- **BRD references.** Each FR cites the BRD ID and the page of the BRD-5 PDF ("p.69"); "Add.1" is Addendum 1 (pp.28-39) and "Add.2" the Workshop Addendum (pp.1-15). Rows renumbered by Addendum 1 are used in their new form.
- **Messages and codes.** Validation messages, error codes, lists of values, parameters and permissions are those shown on the screens. A validation done by the screen or the platform shows "-" as code.
- **Clarifications.** Where the proposed rule or screen differs from the BRD text or needs a decision of BDOI, the FR says so in a note; chapter 12 of each volume lists each point with the decision requested.
- **Items waiting for BDOI data.** A function that waits for BDOI data (layouts, accounts, lists) names its open question. The answer is applied as configuration, without a change to the system, unless the FR says otherwise.
- **Cross-references.** A reference to the other volume names the FR and the volume, for example "FR-DS-041, Volume 2". The accounting events of BRD-5 and their seed entries are in Volume 1, section 5.5.

# Coverage and references

## Coverage

<!-- table: widths=5.2,1.9,1.9,1.9,2.4 caption="Coverage of BRD-5" size=8.5 -->
| Volume | BRD IDs | Covered by an FR | Out | Clarifications |
|---|---|---|---|---|
| Volume 1 | 85 | 85 | 0 | 14 |
| Volume 2 | 192 | 192 | 1 | 25 |
| **Total** | **277** | **277** | **1** | **39** |

The login to BDO Business Online Banking (DIS 2.17.1) is outside BIBS and marked OUT in the BRD baseline.

## Shared references

<!-- table: widths=1.2,11.4,4 caption="References common to both volumes" -->
| Ref. | Document | Version / date |
|---|---|---|
| R1 | Accounting, Disbursement and ACSL BRD (main BRD), pages 40-153; signed scan pp.154-267 | v1.0, 23-Jul-2025 |
| R2 | Addendum 1, pages 28-39 | v1.0, 18-Dec-2025; signed 13-Jan-2026 |
| R3 | Addendum 2 (Workshop), pages 1-15 | v1.0, 10-Apr-2026; signed 8 to 15-Apr-2026 |
| R4 | BRD-5 requirements baseline | current |
| R6 | Cross-BRD decisions and answered questions | current |

# Sign-off route

Each volume is reviewed and signed by the owners of its requirements. BDOI signs this note to confirm that the two volumes together cover BRD-5.

<!-- table: widths=4.4,8.2,4 caption="Review and sign-off" -->
| Document | Reviewed by | Signed by |
|---|---|---|
| Volume 1 | FRBS / GL team; Business and System Administrators; ACSL and Marketing for their parts | Head, Comptrollership; Head, FRBS; Business Administrator; BPS Program Manager; iorta TechNXT Project Manager |
| Volume 2 | Disbursement Section; Marketing; Human Resources; ACSL; Operations (Cashiering, Remittance) | Head, Comptrollership; Head, Disbursement Section; Head, ACSL; Head, Marketing; BPS Program Manager; iorta TechNXT Project Manager |
| Cover note | Comptrollership | Head, Comptrollership; BPS Program Manager |

Open questions stay open after sign-off; their answers are applied as configuration or through a change request.

```signoff
rows:
  - {name: "", role: "Head, Comptrollership", organisation: BDOI}
  - {name: "", role: "Program Manager, Business Project Services", organisation: BDO Unibank ESG}
  - {name: "", role: Project Manager, organisation: iorta TechNXT}
```
