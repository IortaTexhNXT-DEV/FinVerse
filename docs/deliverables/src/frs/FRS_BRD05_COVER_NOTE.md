---
# Source of the cover note of the Functional Requirements Specification for BRD-5 (two volumes).
# Build: python tools/deliverables/bdoi_docx.py docs/deliverables/src/frs/FRS_BRD05_COVER_NOTE.md
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
  - {name: Project team, role: Delivery, organisation: iorta TechNXT, purpose: "Build, test and UAT preparation"}
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
| 1 | Introduction | Purpose, scope, references, definitions, how to read the FRs, and the table of recorded differences between the built behaviour and the BRD |
| 2 | Business context | Current and envisioned process; process overview with a figure |
| 3 | Personas and roles | Personas, permissions and the role-to-permission matrix |
| 4 | Functional requirements | One FR per requirement group, each citing the BRD IDs and pages |
| 5 | Workflows and statuses | A figure and a status table per workflow; accounting events |
| 6 | Reports and documents | Reports with their codes and status; document templates |
| 7 | Interfaces | Figure and table of the interfaces |
| 8 | Non-functional requirements | BRD values and the BIBS approach |
| 9 | Configuration | Parameters, jobs, lists of values, alerts, masters |
| 10 | Assumptions, dependencies, open questions | Items that need a BDOI answer (AQ numbers) |
| 11 | Traceability | Every BRD ID to its FR, screen, API and build status, with a coverage summary |
| 12 | Sign-off | Signatories of the volume |

## Conventions

- **BRD references.** Each FR cites the BRD ID and the page of the BRD-5 PDF ("p.69"); "Add.1" is Addendum 1 (pp.28-39) and "Add.2" the Workshop Addendum (pp.1-15). Rows renumbered by Addendum 1 are used in their new form.
- **Messages and codes.** Validation messages, error codes, lists of values, parameters and permissions are those of the delivered system. A validation done by the screen or the platform shows "-" as code.
- **Differences.** Where the delivered behaviour differs from the BRD text, the FR describes the delivered behaviour and a note records the difference; section 1.7 of each volume lists them all.
- **Parked items.** A function that is built but waits for BDOI data (layouts, accounts, lists) is marked "parked" with its open question. The answer is applied as configuration, without a new build, unless the FR says otherwise.
- **Gaps.** Requirements not met by the delivered system carry a gap number, listed below.
- **Cross-references.** A reference to the other volume names the FR and the volume, for example "FR-DS-041, Volume 2". The accounting events of BRD-5 and their demo entries are in Volume 1, section 5.5.

# Coverage and references

## Coverage and gaps

<!-- table: widths=5.2,1.9,1.9,1.9,2,2,1.7 caption="Coverage of BRD-5" size=8.5 -->
| Volume | BRD IDs | Covered by an FR | Built | Built, parked | Built with gap / not built | Out |
|---|---|---|---|---|---|---|
| Volume 1 | 85 | 85 | 66 | 14 | 5 | 0 |
| Volume 2 | 192 | 192 | 165 | 23 | 3 | 1 |
| **Total** | **277** | **277** | **231** | **37** | **8** | **1** |

<!-- table: widths=1.4,7.4,4.2,3.6 caption="Gaps recorded in the FRS" size=8.5 -->
| Gap | Description | BRD IDs | Volume |
|---|---|---|---|
| G1 | The report platform has no Word renderer; the GARD, subsidiaries and Mancom schedules asked in Word are delivered in PDF and Excel | FRBS 3.2.0 | Volume 1, FR-AC-060 |
| G2 | Return, resubmit and bulk approval of access requests exist in the API only, not in the screens | BASAU 2.4.1, 2.5.3, 2.6.0, 2.6.1 | Volume 1, FR-AC-072 |
| G3 | ACSL aging and schedule reports per account family are not built; they need eight ageing slots in the ledger | ACSL 2.14.3, 2.14.4 | Volume 2, FR-AS-005 |
| G4 | The CPC2 incentive report is not built | DIS 3.29.0 | Volume 2, FR-DS-092 |

The login to BDO Business Online Banking (DIS 2.17.1) is outside BIBS and marked OUT in the BRD baseline.

## Shared references

<!-- table: widths=1.2,7.4,3.6,5.4 caption="References common to both volumes" -->
| Ref. | Document | Version / date | Location |
|---|---|---|---|
| R1 | Accounting, Disbursement and ACSL BRD (main BRD), pages 40-153; signed scan pp.154-267 | v1.0, 23-Jul-2025 | `docs/source-documents/Accounting, Disbursement, and Accounting Controls and Subsidiary Ledger BRD.zip` |
| R2 | Addendum 1, pages 28-39 | v1.0, 18-Dec-2025; signed 13-Jan-2026 | same file |
| R3 | Addendum 2 (Workshop), pages 1-15 | v1.0, 10-Apr-2026; signed 8 to 15-Apr-2026 | same file |
| R4 | BRD-5 requirements baseline and fit/gap | current | `docs/requirements/BDOI_ACCT_BRD_SPEC.md` |
| R5 | Accounting, Disbursement and ACSL build design with the as-built notes | current | `docs/architecture/ACCOUNTING_DISBURSEMENT_DESIGN.md` |
| R6 | Cross-BRD decisions and answered questions | current | `docs/requirements/BDOI_CROSS_BRD_DECISIONS.md` |

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
