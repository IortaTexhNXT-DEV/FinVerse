# BDOI Core Replacement (umbrella BRD) - Requirements Baseline, Coverage and Fit/Gap

Client: BDO Insurance and Reinsurance Brokers, Inc. (BDOI), Philippines. Platform: iNXT BrokerVerse (BIBS).

Status: **analysis of the umbrella BRD against the twelve BRDs, the Data Migration BRD, the ReInsurance BRD
(phase 2) and the code as built.** The impact on the build is in
[`CORE_REPLACEMENT_IMPACT.md`](../architecture/CORE_REPLACEMENT_IMPACT.md); the client document is the umbrella FRS
(`docs/deliverables/src/frs/FRS_BRD00_CORE_REPLACEMENT.md`, built as
`BIBS_FRS_BRD-00_Core_Replacement_v1.0.docx`).

## 1. Source document

Source: `docs/source-documents/00 - BRD BDOI Core Replacement v01.pdf` (48 pages), prepared by Enterprise Services
Group - Business Project Services (BDO Unibank). Every page was read; pages 26-29 (capability matrix) and 46-48
(approval) were read as images because their text layer is incomplete.

| Pages | Content |
|---|---|
| 1-2 | Cover; revision log (4-Aug-2025 to 20-Nov-2025, every row "version 1.0"; "Added Non-Package Management" on 20-Nov-2025) |
| 3 | Executive summary; list of ten BRDs with links (New Business, Renewal, Collection Management, Accounting / Disbursement / ACSL, Claims, Operations, Reinsurance, Customer Service Facility, Product Maintenance, Employee Benefits) |
| 4-5 | Business objective, benefits, current process (before) and envisioned process (after) |
| 6-12 | 21 business key capabilities with 184 bullets |
| 13-25 | Table of business requirements BR-000 to BR-208 (persona, requirement, priority; acceptance criteria deferred to the individual BRDs, footnote 1) |
| 26-29 | Capability matrix: end-to-end flow (p.26) and one box per capability item (p.26-29) |
| 30-41 | Requirement traceability (Level 1 / Level 2 to BRD IDs; source workbook "E2E BDOI Mapping.xlsx", not supplied) |
| 42-45 | Usage requirements: users per role, response time, transaction volumes; demand and availability questions ("Refer to BRD"); data retention and archiving |
| 46-48 | Anonymisation question ("No"); approval by 13 signatories (dates 21-Nov to 1-Dec-2025) |

Page references in this document ("p.14") are pages of this PDF.

## 2. Business summary

**What the BRD is.** An umbrella over the function BRDs. It states that each BDOI function has its own BRD and that
this document "does not replace the detailed specifications" (p.3). Every BR row defers its expected result,
acceptance criteria and negative scenarios to the individual BRD (footnote 1, p.13). The umbrella therefore adds
few requirements of its own; its value is the end-to-end scope, the cross-cutting requirements and one set of usage
figures.

**Objectives (p.4).** A lifecycle of non-life brokering and reinsurance operations from client onboarding to claims
and accounting; integration of departments for Motor, Fire, CARI, CTPL and other non-life products; retail and
wholesale business; BIR and Insurance Commission reportorial compliance.

**Envisioned process (p.5).**
- end-to-end automation of claims authorisation, policy validation, placement requests and renewals;
- automated reconciliation, posting and tagging across financial workflows;
- centralised dashboards for all roles, and unified client and account management;
- self-service for business units (renewals, PR), role-based permissions;
- system-generated and customised reports, real-time dashboards and audit logs;
- automated notifications, approvals and feedback tracking; integrated workflows for disbursements, refunds and ACSL
  corrections.

**The BRDs named by the umbrella (p.3) and their BIBS numbers.**

| Core BRD list (p.3) | BIBS BRD | Spec | Status in BIBS |
|---|---|---|---|
| 1 New Business | BRD-1 | `BDOI_NB_BRD_SPEC.md` | Built |
| 2 Renewal | BRD-6 | `BDOI_RN_BRD_SPEC.md` | Designed, not built |
| 3 Collection Management | BRD-4 | `BDOI_CLXN_BRD_SPEC.md` | Built |
| 4 Accounting, Disbursement and ACSL | BRD-5 | `BDOI_ACCT_BRD_SPEC.md` | Built |
| 5 Claims | BRD-7 | `BDOI_CLM_BRD_SPEC.md` | Being built (CL0, CL1-A merged: V1020-V1022) |
| 6 Operations | BRD-2 | `BDOI_OPS_BRD_SPEC.md` | Built |
| 7 Reinsurance | ReInsurance BRD (32 pages) | none (phase 2) | **Phase 2 - BRD received**; not in the phase 1 build |
| 8 Customer Service Facility | BRD-9 | `BDOI_CSF_BRD_SPEC.md` | Designed, not built |
| 9 Product Maintenance | BRD-3 | `BDOI_PM_BRD_SPEC.md` | Built |
| 10 Employee Benefits | BRD-8 | `BDOI_EB_BRD_SPEC.md` | Designed, not built |
| (not listed) | BRD-10 Sanction Screening and Risk Profiling | `BDOI_SANC_BRD_SPEC.md` | Built |
| (not listed) | BRD-11 User Access Maintenance | `BDOI_UAM_BRD_SPEC.md` | Built; BR-000 to BR-003 and the "User & Data Management" traceability (p.38) point to it |
| (not listed) | BRD-12 Submitted Policies | `BDOI_SP_BRD_SPEC.md` | Designed, not built |
| (not listed) | Data Migration BRD (draft v0.01, 16 pages) | none yet | Draft; the umbrella names only "Client Migration" volumes (p.43) |

The end-to-end flow of p.26 runs: Client Onboarding, Quotation or Proposal, Account Creation and Placement and
Booking (all "New Business Fire and Motor"), then Renewal (RMEL Phase 2), Collection (CMS), Cashiering, Remittance
and Accounting / GL, with Claims, Production Reconciliation, Adjustment / Cancellation, MIS and Analytics, Customer
Service Facility, Reinsurance and User & Data Management attached to Accounting.

## 3. Fit/gap summary

Each capability bullet of pp.6-12 is one row (CORE-nn.mm; the BRD has no bullet IDs). Eight items that appear only in
the capability matrix (pp.26-28) or only in the BR table are added with their page (CORE-03.06, 06.08, 06.09, 07.06,
15.07-15.10). Requirements of the umbrella that are not bullets (narrative pp.4-5, BR rows without a bullet, usage
pages) are the cross-cutting rows XC-01 to XC-23 (section 6). All 209 BR IDs are mapped (section 7).

The fit class is measured against the code **as built today**. For a capability whose BRD is designed but not
built (BRD-6, BRD-8, BRD-9), the class is the one in that BRD's spec, and the status column says "Designed". The
ReInsurance rows are OUT for phase 1 with the status "Phase 2".

| Fit | Meaning | Capability rows | Cross-cutting rows | Total |
|---|---|---|---|---|
| FIT | Works today | 135 | 12 | 147 |
| CONFIGURE | Set-up only | 1 | 2 | 3 |
| CHANGE | Extends an existing capability | 15 | 6 | 21 |
| NEW | New build (in BIBS, or in a designed BRD not yet built) | 29 | 1 | 30 |
| OUT | Out of phase 1 scope | 12 | 2 | 14 |
| **Total** | | **192** | **23** | **215** |

| Capability | Rows | FIT | CONFIGURE | CHANGE | NEW | OUT | Covered by | BRD status |
|---|---|---|---|---|---|---|---|---|
| CORE-01 Client Onboarding | 7 | 7 | 0 | 0 | 0 | 0 | BRD-1 | Built |
| CORE-02 Quotation or Proposal | 9 | 9 | 0 | 0 | 0 | 0 | BRD-1 | Built |
| CORE-03 Account Creation and Maintenance | 6 | 6 | 0 | 0 | 0 | 0 | BRD-1 | Built |
| CORE-04 Non-Package Management | 24 | 24 | 0 | 0 | 0 | 0 | BRD-1 | Built |
| CORE-05 Placement and Booking | 8 | 8 | 0 | 0 | 0 | 0 | BRD-1 | Built |
| CORE-06 Renewal | 9 | 0 | 0 | 3 | 6 | 0 | BRD-6 | Designed |
| CORE-07 Marketing Collection | 6 | 6 | 0 | 0 | 0 | 0 | BRD-4 | Built |
| CORE-08 Accounting / GL / Disbursement / ACSL | 22 | 22 | 0 | 0 | 0 | 0 | BRD-5, BRD-2, BRD-1 | Built |
| CORE-09 Cashiering (Payments Acceptance and Application) | 12 | 12 | 0 | 0 | 0 | 0 | BRD-2 | Built |
| CORE-10 Remittance | 7 | 7 | 0 | 0 | 0 | 0 | BRD-2 | Built |
| CORE-11 Production Reconciliation | 5 | 5 | 0 | 0 | 0 | 0 | BRD-2 | Built |
| CORE-12 Adjustment / Cancellation | 7 | 7 | 0 | 0 | 0 | 0 | BRD-2 | Built |
| CORE-13 Collection of Commission Receivables (Direct Payment) | 8 | 8 | 0 | 0 | 0 | 0 | BRD-2 | Built |
| CORE-14 Claims | 13 | 0 | 0 | 0 | 13 | 0 | BRD-7 | Being built |
| CORE-15 Reinsurance | 10 | 0 | 0 | 0 | 0 | 10 | ReInsurance BRD | Phase 2 |
| CORE-16 Customer Service Facility | 6 | 0 | 0 | 4 | 1 | 1 | BRD-9 | Designed |
| CORE-17 Data Management | 5 | 2 | 0 | 2 | 1 | 0 | none, BRD-1, BRD-3 | Built, No BRD |
| CORE-18 Emerging Capabilities | 1 | 0 | 0 | 0 | 0 | 1 | none | No BRD |
| CORE-19 Employee Benefits | 12 | 0 | 0 | 4 | 8 | 0 | BRD-8 | Designed |
| CORE-20 Product Maintenance | 10 | 9 | 1 | 0 | 0 | 0 | BRD-3 | Built |
| CORE-21 Report Generation | 5 | 3 | 0 | 2 | 0 | 0 | BRD-1, none | Built, No BRD |

**Reading the numbers.**
- The umbrella adds no business function that the BRD set lacks, apart from Reinsurance (phase 2). 135 of the 192
  capability rows are FIT because BRD-1 to BRD-5 are built; the NEW rows are Renewal, Claims and Employee Benefits,
  whose BRDs are designed or being built.
- The real gaps are cross-cutting and have no owning BRD: master data change logging, insurer management, MIS field
  definition (capability 17), report customisation and scheduling (capability 21), role dashboards (p.5, BR-125 /
  152 / 160) and the Invoice Master List (BR-175).
- Two bullets contradict their own BRD: case resolution in CSF (CORE-16.06) and the claims cheque hand-offs
  (CORE-14.08 to 14.10).

**Big-ticket items (in order of size).**
1. Role home dashboards for every persona (XC-02): today one finance dashboard and about a dozen section home pages.
2. Report customisation and user-scheduled reports with distribution (CORE-21.04, 21.05).
3. Master data change log with field-level old / new values (CORE-17.01).
4. Insurer management in one place (CORE-17.02) and MIS field definition (CORE-17.04).
5. Insurer invoice batch delivery by SFTP (XC-19) and the Invoice Master List columns (XC-13).
6. Dependencies outside this BRD: client migration and daily client batches (XC-20, Data Migration BRD), claims
   cheque custody (CRQ03), CSF case management (CRQ04).

## 4. Coverage matrix

"X" = the BRD specifies the capability; "s" = the BRD supports it (shared data, a seam or a feed); "P2" = ReInsurance
BRD received, phase 2; "DM" = depends on the Data Migration BRD. Columns: BRD-1 New Business, BRD-2 Operations,
BRD-3 Product Maintenance, BRD-4 Collections, BRD-5 Accounting / Disbursement / ACSL, BRD-6 Renewal, BRD-7 Claims,
BRD-8 Employee Benefits, BRD-9 CSF, BRD-10 Sanction Screening, BRD-11 User Access, BRD-12 Submitted Policies, DM Data
Migration, RI ReInsurance.

| Capability | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 | 12 | DM | RI | Gap |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 01 Client Onboarding | X | | | | | | | s | s | s | | s | X | | Delete of prospect (CRQ05) |
| 02 Quotation or Proposal | X | | s | | | s | | s | | | | | | | - |
| 03 Account Creation and Maintenance | X | | s | | | s | | s | | | | s | | | - |
| 04 Non-Package Management | X | | s | | | | | | | | | | | | No BR ID (CRQ02) |
| 05 Placement and Booking | X | s | | | s | s | | s | | | | s | | | SOA at booking (CRQ12) |
| 06 Renewal | s | | s | | | X | s | s | s | s | | s | X | | - |
| 07 Marketing Collection | | s | | X | s | | | s | | | | s | X | | - |
| 08 Accounting / GL / Disbursement / ACSL | s | s | | s | X | | | | | | | | X | | - |
| 09 Cashiering | | X | | s | s | | | | | | | s | X | | - |
| 10 Remittance | | X | | | s | | s | | | | | | X | | - |
| 11 Production Reconciliation | | X | | | | | | | | | | | X | | - |
| 12 Adjustment / Cancellation | | X | | | s | s | | | | | | | X | | - |
| 13 Collection of Commission Receivables | | X | s | s | s | | | | | | | | X | | - |
| 14 Claims | | s | | | | s | X | | s | | | | s | s | Cheque custody (CRQ03) |
| 15 Reinsurance | | | | | s | | s | | | | | | | P2 | Phase 2 - BRD received |
| 16 Customer Service Facility | s | s | | | | s | s | s | X | | | | | | Case resolution (CRQ04) |
| 17 Data Management | s | s | X | s | s | | s | | | s | s | | X | | Change log, insurer, MIS fields (CRQ07-09) |
| 18 Emerging Capabilities | | | | | | | | | | | | | | | Undefined (CRQ10) |
| 19 Employee Benefits | s | | | | | s | | X | | | s | | | | - |
| 20 Product Maintenance | s | | X | | | s | | | | | | | s | | - |
| 21 Report Generation | X | X | X | X | X | X | X | X | X | X | X | X | | | Customisation, scheduling (CRQ11) |

BRD-10 Sanction Screening and BRD-12 Submitted Policies are not named by the umbrella, but they feed capabilities 1,
3, 6 and 9 (screening of clients; submitted-policy intake, handling fee and renewal hand-off). BRD-11 is the "User"
half of "User & Data Management" (p.38).

## 5. Requirements and fit/gap

Columns: **Core BR** = the rows of the BR table (pp.13-25) that restate the bullet; **Covered by** = the BIBS BRD and
its requirement IDs; **FRS** = the functional requirements of that BRD's FRS that meet it (read from the FRS sources).
FR-CL is the prefix of BRD-4 Collections; BRD-7 Claims uses FR-CM (renumbered from FR-CL, section 14, O9 and
DCR-188). Module paths are under `backend/src/main/java/com/iortatechnxt/brokerverse/`.

### CORE-01 Client Onboarding (p.6; traceability p.30)

| ID | Capability | p. | Core BR | Covered by (BRD and IDs) | FRS | Status | BIBS module and files | Fit | Gap and proposal | Q |
|---|---|---|---|---|---|---|---|---|---|---|
| CORE-01.01 | Create prospect / record | 6 | BR-005 | BRD-1 BRNB.090, BRNB.101, BRNB.048 | FR-NB-034, FR-NB-031 | Built | `crm` ClientOnboardingService; /crm/clients/new | **FIT** | - | - |
| CORE-01.02 | Update prospect / record | 6 | BR-006 | BRD-1 BRNB.049, BRNB.047 | FR-NB-032, FR-NB-035 | Built | `crm` ClientService, ClientBulkHandler; /crm/clients/:id/edit | **FIT** | - | - |
| CORE-01.03 | Convert prospect into client record | 6 | BR-007 | BRD-1 BRNB.090, BRNB.101 | FR-NB-034 | Built | `crm` ClientOnboardingService (PROSPECT to CONFIRMED) | **FIT** | - | - |
| CORE-01.04 | Delete prospect / record | 6 | BR-008 | BRD-1 BRNB.019, BRNB.106 | FR-NB-012, FR-NB-137 | Built | `crm` ClientOnboardingService.deactivate (reason LOV); `nbadmin` retention purge | **FIT** | The Core traceability (p.30) cites no BRD ID. BIBS deactivates a client with a reason and purges it under the retention rules; nothing is deleted on request. | CRQ05 |
| CORE-01.05 | Upload and validate client documents | 6 | BR-009, 010, 011 | BRD-1 BRNB.030, BRNB.049, BRNB.026 | FR-NB-031, FR-NB-032, FR-NB-033, FR-NB-034 ... | Built | `crm` KycDocumentService; `attachment`; Documents tab | **FIT** | - | - |
| CORE-01.06 | Client search | 6 | BR-012 | BRD-1 BRNB.046 | FR-NB-030 | Built | `crm` ClientSearchService; /crm/clients | **FIT** | - | - |
| CORE-01.07 | Generate client code | 6 | BR-013 | BRD-1 BRNB.030, BRNB.101 | FR-NB-031, FR-NB-032, FR-NB-033, FR-NB-034 | Built | `crm` ClientOnboardingService (code on confirmation) | **FIT** | - | - |

### CORE-02 Quotation or Proposal (p.6; traceability p.30)

| ID | Capability | p. | Core BR | Covered by (BRD and IDs) | FRS | Status | BIBS module and files | Fit | Gap and proposal | Q |
|---|---|---|---|---|---|---|---|---|---|---|
| CORE-02.01 | Receive request for quotation / proposal | 6 | BR-014 | BRD-1 BRNB.041, BRNB.023 | FR-NB-040 | Built | `quotation` QuotationRequestService; /quotations/requests | **FIT** | Mailbox reading and HLS intake are parked seams (Q11, Q12). | - |
| CORE-02.02 | Create quotation / proposal (individual or bulk) | 6 | BR-015, 016 | BRD-1 BRNB.043, BRNB.042, BRNB.028 | FR-NB-041, FR-NB-044, FR-NB-046 | Built | `quotation` QuotationService; `bulk` QUOTATION_CREATE; /quotations/new | **FIT** | - | - |
| CORE-02.03 | Track proposal status | 6 | BR-017 | BRD-1 BRNB.022, BRNB.115 | FR-NB-010, FR-NB-122, FR-NB-120 | Built | `workflow` WorkflowViewService; `nbreport` NB-ACC-STATUS | **FIT** | - | - |
| CORE-02.04 | Edit quotation / proposal | 6 | BR-018 | BRD-1 BRNB.020 | FR-NB-042 | Built | `quotation` QuotationService (versions with diff) | **FIT** | - | - |
| CORE-02.05 | Approve quotation / proposal | 6 | BR-019 | BRD-1 BRNB.021, BRNB.014 | FR-NB-043, FR-NB-014, FR-NB-051 | Built | `quotation`, `workflow`; /quotations/:id | **FIT** | - | - |
| CORE-02.06 | Print quotation / proposal | 6 | BR-020 | BRD-1 BRNB.043 | FR-NB-041, FR-NB-044 | Built | `quotation` PDF / XLSX output; `docgen` templates | **FIT** | - | - |
| CORE-02.07 | Upload documents | 6 | BR-021 | BRD-1 BRNB.055, BRNB.026 | FR-NB-017 | Built | `attachment` DocumentService; Documents tabs | **FIT** | - | - |
| CORE-02.08 | Send quotation / proposal | 6 | BR-022 | BRD-1 BRNB.043, BRNB.042 | FR-NB-041, FR-NB-044, FR-NB-046 | Built | `quotation` QuotationDispatchService; `messaging` outbox | **FIT** | - | - |
| CORE-02.09 | Generate and customise report | 6 | BR-053 | BRD-1 BRNB.057, BRNB.075 | FR-NB-123, FR-NB-122 | Built | `nbreport`; `report` saved variants | **FIT** | Customisation beyond parameters and saved variants: see CORE-21.04. | - |

### CORE-03 Account Creation and Maintenance (p.6; traceability p.30-31)

| ID | Capability | p. | Core BR | Covered by (BRD and IDs) | FRS | Status | BIBS module and files | Fit | Gap and proposal | Q |
|---|---|---|---|---|---|---|---|---|---|---|
| CORE-03.01 | Create account (manual, bulk upload, system) | 6 | BR-023, 024, 025, 028 | BRD-1 BRNB.051, BRNB.066, BRNB.039 | FR-NB-061, FR-NB-062, FR-NB-063, FR-NB-065 ... | Built | `account` AccountService; `bulk` ACCOUNT_CREATE; /accounts/new | **FIT** | - | - |
| CORE-03.02 | Update account | 6 | BR-029 | BRD-1 BRNB.025, BRNB.053, BRNB.054 | FR-NB-064, FR-NB-065 | Built | `account` AccountService; `bulk` ACCOUNT_UPDATE | **FIT** | - | - |
| CORE-03.03 | Link to client record | 6 | BR-030 | BRD-1 BRNB.051, BRNB.099 | FR-NB-061, FR-NB-062, FR-NB-063, FR-NB-037 | Built | `account`, `crm` Client360Service | **FIT** | - | - |
| CORE-03.04 | Approve account | 6 | BR-031 | BRD-1 BRNB.022, BRNB.079 | FR-NB-010, FR-NB-122, FR-NB-133 | Built | `workflow` NB_ACCOUNT (Processing validation); `approval` | **FIT** | - | - |
| CORE-03.05 | Initiate placement request | 6 | BR-033 | BRD-1 BRNB.069 | FR-NB-080 | Built | `placement` PlacementSlipService | **FIT** | - | - |
| CORE-03.06 | Client confirmation (capability matrix p.26) | 26 | BR-038 | BRD-1 BRNB.045 | FR-NB-045, FR-NB-057 | Built | `quotation` QuotationAcceptanceService; `placement` PaymentGateService | **FIT** | Item of the capability matrix only; not in the bullet list of p.6. | - |

### CORE-04 Non-Package Management (p.7; traceability none)

| ID | Capability | p. | Core BR | Covered by (BRD and IDs) | FRS | Status | BIBS module and files | Fit | Gap and proposal | Q |
|---|---|---|---|---|---|---|---|---|---|---|
| CORE-04.01 | Create proposal request form (PRF) | 7 | - | BRD-1 BRNB.005 | FR-NB-014, FR-NB-050, FR-NB-051 | Built | `nonpackage` ProposalService; /proposals/new | **FIT** | - | CRQ02 |
| CORE-04.02 | Approve PRF | 7 | - | BRD-1 BRNB.005, BRNB.014 | FR-NB-014, FR-NB-050, FR-NB-051, FR-NB-043 | Built | `nonpackage`, `workflow` (Marketing TL / TH / UH chain) | **FIT** | - | - |
| CORE-04.03 | Submit PRF | 7 | - | BRD-1 BRNB.005 | FR-NB-014, FR-NB-050, FR-NB-051 | Built | `nonpackage` ProposalService | **FIT** | - | - |
| CORE-04.04 | Receive PRF (TSU) | 7 | - | BRD-1 BRNB.007 | FR-NB-052 | Built | `nonpackage`; /proposals/tsu | **FIT** | - | - |
| CORE-04.05 | Edit PRF | 7 | - | BRD-1 BRNB.007 | FR-NB-052 | Built | `nonpackage` ProposalService | **FIT** | - | - |
| CORE-04.06 | Create Quotation Slip | 7 | - | BRD-1 BRNB.008 | FR-NB-053 | Built | `nonpackage` QuotationSlipService | **FIT** | - | - |
| CORE-04.07 | Send Quotation Slip to insurers | 7 | - | BRD-1 BRNB.008 | FR-NB-053 | Built | `nonpackage` QuotationSlipService; `messaging` delivery log | **FIT** | - | - |
| CORE-04.08 | Input insurers' feedback in the comparative table | 7 | - | BRD-1 BRNB.009 | FR-NB-054, FR-NB-055 | Built | `nonpackage` InsurerResponseService | **FIT** | - | - |
| CORE-04.09 | Submit comparative table | 7 | - | BRD-1 BRNB.010 | FR-NB-055 | Built | `nonpackage` (Comparative Table tab) | **FIT** | - | - |
| CORE-04.10 | Finalise Proposal Slip | 7 | - | BRD-1 BRNB.017 | FR-NB-056, FR-NB-057 | Built | `nonpackage` ProposalSlipService | **FIT** | - | - |
| CORE-04.11 | Submit Proposal Slip | 7 | - | BRD-1 BRNB.017 | FR-NB-056, FR-NB-057 | Built | `nonpackage` ProposalSlipService | **FIT** | - | - |
| CORE-04.12 | Receive and print quotation | 7 | - | BRD-1 BRNB.017, BRNB.043 | FR-NB-056, FR-NB-057, FR-NB-041, FR-NB-044 | Built | `nonpackage`, `quotation` | **FIT** | - | - |
| CORE-04.13 | Send quotation | 7 | - | BRD-1 BRNB.043 | FR-NB-041, FR-NB-044 | Built | `quotation` QuotationDispatchService | **FIT** | - | - |
| CORE-04.14 | Convert quotation to account | 7 | - | BRD-1 BRNB.045 | FR-NB-045, FR-NB-057 | Built | `nonpackage` ProposalAcceptanceService; `quotation` QuotationAcceptanceService | **FIT** | - | - |
| CORE-04.15 | Edit account | 7 | - | BRD-1 BRNB.053 | FR-NB-064 | Built | `account` AccountService | **FIT** | - | - |
| CORE-04.16 | Approve account | 7 | - | BRD-1 BRNB.022 | FR-NB-010, FR-NB-122 | Built | `workflow` NB_ACCOUNT | **FIT** | - | - |
| CORE-04.17 | Initiate policy placement request | 7 | - | BRD-1 BRNB.069 | FR-NB-080 | Built | `placement` PlacementSlipService | **FIT** | - | - |
| CORE-04.18 | Track PRF / proposal placement slip / quotation / placement request status | 7 | - | BRD-1 BRNB.012, BRNB.022, BRNB.115 | FR-NB-120, FR-NB-010, FR-NB-122 | Built | `nbreport` NB dashboard; `workflow` StageTimeline | **FIT** | - | - |
| CORE-04.19 | Upload documents | 7 | - | BRD-1 BRNB.055 | FR-NB-017 | Built | `attachment` | **FIT** | - | - |
| CORE-04.20 | Generate and customise report | 7 | - | BRD-1 BRNB.011, BRNB.057 | FR-NB-121, FR-NB-123 | Built | `nbreport` NB-PLC-UPDATE; saved variants | **FIT** | See CORE-21.04 for field-level customisation. | - |
| CORE-04.21 | Automated reference number generation | 7 | - | BRD-1 BRNB.006 | FR-NB-047, FR-NB-050 | Built | `common.sequence` DocumentNumberService (MKT-yyyy-n) | **FIT** | - | - |
| CORE-04.22 | Password protection and encryption of shared documents | 7 | - | BRD-1 BRNB.013 | FR-NB-013, FR-NB-044, FR-NB-057 | Built | `messaging` DocumentProtector, DocumentPasswordPolicy | **FIT** | Password convention still open (Q07). | - |
| CORE-04.23 | System notification on status changes | 7 | - | BRD-1 BRNB.015 | FR-NB-015 | Built | `messaging` NotificationService; header bell | **FIT** | - | - |
| CORE-04.24 | Complete logging of all actions | 7 | - | BRD-1 BRNB.016 | FR-NB-016 | Built | `audit` AuditTrailService; History tabs | **FIT** | - | - |

### CORE-05 Placement and Booking (p.7; traceability p.31)

| ID | Capability | p. | Core BR | Covered by (BRD and IDs) | FRS | Status | BIBS module and files | Fit | Gap and proposal | Q |
|---|---|---|---|---|---|---|---|---|---|---|
| CORE-05.01 | Submit for placement and booking | 7 | BR-032 | BRD-1 BRNB.022, BRNB.096 | FR-NB-010, FR-NB-122, FR-NB-011, FR-NB-064 | Built | `workflow` NB_ACCOUNT (SUBMITTED) | **FIT** | - | - |
| CORE-05.02 | Generate placement slip | 7 | BR-036 | BRD-1 BRNB.069 | FR-NB-080 | Built | `placement` PlacementSlipService; /placement/slips | **FIT** | - | - |
| CORE-05.03 | Generate placement report | 7 | BR-039 | BRD-1 BRNB.011, BRNB.075 | FR-NB-121, FR-NB-122 | Built | `nbreport` PlacementSummaryReport, PlacementUpdateReport | **FIT** | - | - |
| CORE-05.04 | Send placement slip to insurer | 7 | BR-037 | BRD-1 BRNB.071 | FR-NB-081 | Built | `placement`, `messaging` | **FIT** | SFTP / API channel parked (Q06). | - |
| CORE-05.05 | Generate Insurance Advice | 7 | BR-039 | BRD-1 BRNB.070, BRNB.060 | FR-NB-103, FR-NB-104 | Built | `issuance` InsuranceAdviceService | **FIT** | - | - |
| CORE-05.06 | Receive e-policy (individual or batch) | 7 | BR-040 | BRD-1 BRNB.073 | FR-NB-100 | Built | `issuance` EpolicyUploadService | **FIT** | - | - |
| CORE-05.07 | Send e-policy to client (individual or batch) | 7 | BR-041 | BRD-1 BRNB.077 | FR-NB-105 | Built | `issuance` EpolicyDispatchService | **FIT** | - | - |
| CORE-05.08 | Update client record with the e-policy and policy number | 7 | BR-042 | BRD-1 BRNB.074 | FR-NB-101 | Built | `issuance` EpolicyService; `account` | **FIT** | - | - |

### CORE-06 Renewal (p.8; traceability p.32)

| ID | Capability | p. | Core BR | Covered by (BRD and IDs) | FRS | Status | BIBS module and files | Fit | Gap and proposal | Q |
|---|---|---|---|---|---|---|---|---|---|---|
| CORE-06.01 | Generate RMEL (list of expiring accounts) | 8 | BR-055 | BRD-6 BRRN.002, BRRN.030, BRRN.005 | FR-RN-011, FR-RN-010, FR-RN-112 | Designed | `renewal` (RENEWAL_DESIGN), not built | **NEW** | - | - |
| CORE-06.02 | Filter and distribute RMEL | 8 | BR-056 | BRD-6 BRRN.003, BRRN.011, BRRN.036 | FR-RN-012, FR-RN-040, FR-RN-102 | Designed | `renewal`, not built | **CHANGE** | - | - |
| CORE-06.03 | Rules-based sanitation of accounts (bulk / individual) | 8 | BR-026, 057 | BRD-6 BRRN.020, BRRN.023, BRRN.009 | FR-RN-020, FR-RN-103, FR-RN-004, FR-RN-022 ... | Designed | `renewal` RenewalCheckEngine, not built | **NEW** | Sanitation criteria not listed (RQ01). | - |
| CORE-06.04 | Provide disposition (online or by upload) | 8 | BR-058, 059 | BRD-6 BRRN.031, BRRN.018 | FR-RN-004, FR-RN-023, FR-RN-051, FR-RN-103 ... | Designed | `renewal`, not built | **NEW** | Online vs offline disposition (DCR-062). | - |
| CORE-06.05 | Process and generate renewal proposal | 8 | - | BRD-6 BRRN.033, BRRN.038 | FR-RN-048, FR-RN-064, FR-RN-084 | Designed | `renewal`; `quotation` renewal_ref (V1011) | **CHANGE** | - | - |
| CORE-06.06 | Generate and send Renewal Advice | 8 | BR-060, 061 | BRD-6 BRRN.010 | FR-RN-080, FR-RN-081, FR-RN-090 | Designed | `renewal` RA letters, not built | **NEW** | - | - |
| CORE-06.07 | Send No Advice Letter (NAL) and Non-Renewal Letter (NRL) | 8 | BR-060, 061 | BRD-6 BRRN.001, BRRN.009 | FR-RN-082, FR-RN-103, FR-RN-024, FR-RN-112 | Designed | `renewal` letters, not built | **NEW** | The Core BRD writes NRL; BRD-6 writes NFR (Not for Renewal Letter). | CRQ14 |
| CORE-06.08 | Check renewal payment (capability matrix p.27) | 27 | BR-062 | BRD-6 BRRN.027 | FR-RN-042 | Designed | `renewal` account history (reads `opsledger`) | **CHANGE** | - | - |
| CORE-06.09 | Track renewal status (BR table only) | 15 | BR-064 | BRD-6 BRRN.036 | FR-RN-102 | Designed | `renewal` listings | **NEW** | - | - |

### CORE-07 Marketing Collection (p.8; traceability p.31-32)

| ID | Capability | p. | Core BR | Covered by (BRD and IDs) | FRS | Status | BIBS module and files | Fit | Gap and proposal | Q |
|---|---|---|---|---|---|---|---|---|---|---|
| CORE-07.01 | Premium Receivable (PR) management | 8 | BR-044, 045 | BRD-4 BRCLXN.001, BRCLXN.011, BRCLXN.046 | FR-CL-010, FR-CL-015, FR-CL-016 | Built | `collections` WorklistQueryService, AccountViewService; Finance > Collections | **FIT** | - | - |
| CORE-07.02 | Disposition tracking and management | 8 | BR-048 | BRD-4 BRCLXN.016, BRCLXN.021, BRCLXN.023 | FR-CL-030, FR-CL-031, FR-CL-018 | Built | `collections` PrDispositionService, AccountTimelineService | **FIT** | - | - |
| CORE-07.03 | Tag CWT, premium / PR2307 | 8 | BR-118, 139 | BRD-4 BRCLXN.026, BRCLXN.027, CSHID.026 | FR-CL-032, FR-CL-081, FR-OP-026 | Built | `collections` ScheduledFileService; `cashiering` CwtService | **FIT** | - | - |
| CORE-07.04 | Unapplied payment disposition (excess payment) | 8 | BR-046, 047, 090 | BRD-4 BRCLXN.030, BRCLXN.034, BRCLXN.041 | FR-CL-074, FR-CL-070, FR-CL-077 | Built | `collections` UnappliedDispositionService, ApplicationFileService | **FIT** | - | - |
| CORE-07.05 | Reporting and audit | 8 | BR-053, 054 | BRD-4 BRCLXN.028, BRCLXN.043, BRCLXN.045 | FR-CL-082, FR-CL-003, FR-CL-083 | Built | `collections` ScheduledFileService; `report` ReportArchiveService | **FIT** | - | - |
| CORE-07.06 | Batch processing and automation (capability matrix p.27) | 27 | BR-049 | BRD-4 BRCLXN.013, BRCLXN.024, BRCLXN.041 | FR-CL-017, FR-CL-032, FR-CL-080, FR-CL-077 | Built | `collections` InboxService, ScheduledFileService (jobs) | **FIT** | - | - |

### CORE-08 Accounting / GL / Disbursement / ACSL (p.8-9; traceability p.32-33)

| ID | Capability | p. | Core BR | Covered by (BRD and IDs) | FRS | Status | BIBS module and files | Fit | Gap and proposal | Q |
|---|---|---|---|---|---|---|---|---|---|---|
| CORE-08.01 | Daily financial report reconciliation | 8 | BR-065 | BRD-5 ACSL 2.13.0, ACSL 2.13.2 | FR-AS-003, FR-AS-004 | Built | `acsl` GlSlReconciliationService | **FIT** | - | - |
| CORE-08.02 | Daily cash movement reconciliation | 8 | BR-067 | BRD-5 FRBS 3.3.0 | FR-AC-050 | Built | `receivables` BankReconciliationService | **FIT** | - | - |
| CORE-08.03 | Insurer's statement of accounts (SOA) reconciliation | 8 | BR-068 | BRD-5 ACSL 2.13.1, ACSL 2.14.1 | FR-AS-003 | Built | `acsl` SoaUploadService | **FIT** | - | - |
| CORE-08.04 | Generate automated journal entries | 8 | BR-069 | BRD-5 ACSL 2.15.0, FRBS 3.1.0 | FR-AS-024, FR-AC-030 | Built | `accounting` AccountingEventPublisher; `journal` | **FIT** | - | - |
| CORE-08.05 | Perform manual entries | 8 | BR-070 | BRD-5 FRBS 2.8.0, FRBS 2.8.5 | FR-AC-032 | Built | `journal`; Finance > General Ledger | **FIT** | - | - |
| CORE-08.06 | Perform manual / invoice adjustments | 8 | BR-071 | BRD-5 ACSL 2.9.0, ACSL 2.9.1 | FR-AS-021 | Built | `acsl` CorrectionService | **FIT** | - | - |
| CORE-08.07 | Perform accrual | 8 | BR-072 | BRD-5 FRBS 2.8.1 | FR-AC-032, FR-AC-033 | Built | `journal` (reversal date) | **FIT** | - | - |
| CORE-08.08 | Perform revaluation | 8 | BR-073 | BRD-5 FRBS 2.2.0, FRBS 3.5.0 | FR-AC-010, FR-AC-043 | Built | `closing` FxRevaluationService; `currency` | **FIT** | - | - |
| CORE-08.09 | Perform month-end and year-end closing | 8 | - | BRD-5 FRBS 3.4.0 | FR-AC-042 | Built | `closing` BrokingBooksCloseService, YearEndService | **FIT** | - | - |
| CORE-08.10 | Generate financial reports | 8 | BR-066 | BRD-5 FRBS 3.2.0, DIS 3.28.0 | FR-AC-060, FR-AC-061, FR-AC-062, FR-AC-063 ... | Built | `finreport`, `frbs` ReportPackService, `tax` | **FIT** | - | - |
| CORE-08.11 | Accounts analysis | 8 | BR-074 | BRD-5 ACSL 2.5.0, ACSL 2.5.3 | FR-AS-010 | Built | `acsl` CaseService, AcslQueryService | **FIT** | - | - |
| CORE-08.12 | Receive and release CWT (commission / supplier) | 8 | BR-075, 076 | BRD-5 DIS 2.11.0, DIS 2.12.0 | FR-DS-057, FR-DS-058 | Built | `disbursement` TagService; `tax` 2307 | **FIT** | - | - |
| CORE-08.13 | Disbursement to insurer, client, supplier, BDO subsidiaries | 8 | BR-077 | BRD-5 DIS 2.7.0 | FR-DS-033 | Built | `disbursement` VoucherService, InstrumentService | **FIT** | - | - |
| CORE-08.14 | Reporting and documentation | 8 | BR-078, 084 | BRD-5 DIS 3.28.0, FRBS 3.2.0 | FR-DS-081, FR-AC-060, FR-AC-061, FR-AC-062 ... | Built | `disbursement` EodService; `tax` BookOfAccountsReport | **FIT** | - | - |
| CORE-08.15 | Release BIR 2307 on premiums (PR2307) to insurer | 8 | BR-078 | BRD-2 CSHID.027 | FR-OP-026 | Built | `cashiering` CwtService; `remittance` | **FIT** | - | - |
| CORE-08.16 | Generate Direct Credit transactions file | 8 | BR-077 | BRD-5 DIS 2.7.0 | FR-DS-033 | Built | `disbursement` EodService (DCTF) | **FIT** | - | - |
| CORE-08.17 | Disbursement to government agencies and employees | 8 | BR-077 | BRD-5 DIS 2.7.0 | FR-DS-033 | Built | `disbursement`; `payrequest` | **FIT** | - | - |
| CORE-08.18 | Payee management | 8 | BR-079 | BRD-5 DIS 2.2.0 | FR-DS-010 | Built | `disbursement` PayeeService | **FIT** | - | - |
| CORE-08.19 | Check printing and series management | 8 | BR-080 | BRD-5 DIS 2.7.0 | FR-DS-033 | Built | `disbursement` InstrumentService (check series) | **FIT** | - | - |
| CORE-08.20 | Status tagging and tracking | 8 | BR-081, 082 | BRD-5 DIS 2.8.0, DIS 3.26.0 | FR-DS-050, FR-DS-052 | Built | `disbursement` StatusEditService, TagService | **FIT** | - | - |
| CORE-08.21 | Bank account operations | 9 | - | BRD-5 DIS 2.7.0 | FR-DS-033 | Built | `disbursement` FundingService (account funding, dual approval) | **FIT** | Not defined in the Core BRD; read as account funding and bank instruments of BRD-5. | CRQ15 |
| CORE-08.22 | Generate manual service invoice (Other Income) | 9 | BR-085 | BRD-1 BRNB.100 | FR-NB-117 | Built | `booking` ServiceInvoiceService.issueManual; /booking/service-invoices | **FIT** | - | - |

### CORE-09 Cashiering (Payments Acceptance and Application) (p.9; traceability p.33-34)

| ID | Capability | p. | Core BR | Covered by (BRD and IDs) | FRS | Status | BIBS module and files | Fit | Gap and proposal | Q |
|---|---|---|---|---|---|---|---|---|---|---|
| CORE-09.01 | Manual issuance of AR, OR, invoice, cash and cheque OTC payment | 9 | BR-086 | BRD-2 CSHID.001, CSHID.002 | FR-OP-011, FR-OP-013, FR-OP-014, FR-OP-012 | Built | `cashiering` CashReceiptService | **FIT** | - | - |
| CORE-09.02 | Automated accounting entries | 9 | BR-087 | BRD-2 CSHID.012, CSHID.014 | FR-OP-013, FR-OP-027 | Built | `cashiering` (events to `accounting`) | **FIT** | - | - |
| CORE-09.03 | Batch payment files processing (automatching) | 9 | BR-089 | BRD-2 CSHID.008 | FR-OP-015, FR-OP-016 | Built | `cashiering` PaymentIntakeService | **FIT** | - | - |
| CORE-09.04 | Batch processing and automation (automatch re-run) | 9 | BR-092 | BRD-2 CSHID.020 | FR-OP-018, FR-OP-132 | Built | `cashiering` AutomatchService (job and manual trigger) | **FIT** | - | - |
| CORE-09.05 | Batch reversal processing | 9 | - | BRD-2 CSHID.012, CSHID.016 | FR-OP-013, FR-OP-027, FR-OP-023 | Built | `cashiering` ReceiptReversalService, MinimalBalanceService | **FIT** | - | - |
| CORE-09.06 | Unapplied payment management (excess payment) | 9 | BR-090, 093 | BRD-2 CSHID.024, CSHID.025 | FR-OP-022 | Built | `cashiering` UnappliedService, DispositionService | **FIT** | - | - |
| CORE-09.07 | Payment auto matching | 9 | BR-091 | BRD-2 CSHID.020 | FR-OP-018, FR-OP-132 | Built | `cashiering` AutomatchService | **FIT** | - | - |
| CORE-09.08 | Generate acknowledgement receipt, official receipt, invoice | 9 | BR-094, 095, 096 | BRD-2 CSHID.001, CSHID.006, CSHID.019 | FR-OP-011, FR-OP-013, FR-OP-014, FR-OP-010 ... | Built | `cashiering` ReceiptSeriesService, BatchPrintService | **FIT** | - | - |
| CORE-09.09 | Premium payment monitoring | 9 | BR-097 | BRD-2 CSHID.023, CSHID.017 | FR-OP-028, FR-OP-009 | Built | `cashiering` reports; CashieringHomePage | **FIT** | - | - |
| CORE-09.10 | Commission fee collection | 9 | BR-098, 099 | BRD-2 CSHID.007, CMRID.002 | FR-OP-021, FR-OP-091 | Built | `cashiering` CommissionOrService | **FIT** | - | - |
| CORE-09.11 | PDC management | 9 | BR-100 | BRD-2 CSHID.008 | FR-OP-015, FR-OP-016 | Built | `cashiering` PdcWarehouseService | **FIT** | - | - |
| CORE-09.12 | Traceability and auditability | 9 | - | BRD-2 CSHID.011 | FR-OP-024 | Built | `audit` | **FIT** | - | - |

### CORE-10 Remittance (p.9; traceability p.34)

| ID | Capability | p. | Core BR | Covered by (BRD and IDs) | FRS | Status | BIBS module and files | Fit | Gap and proposal | Q |
|---|---|---|---|---|---|---|---|---|---|---|
| CORE-10.01 | Extraction and remittance processing (scheduled or manual) | 9 | BR-102, 103 | BRD-2 RMTID.001, RMTID.003, RMTID.004 | FR-OP-030 | Built | `remittance` ExtractionService, BatchService | **FIT** | - | - |
| CORE-10.02 | Sending and uploading of files | 9 | BR-104, 105 | BRD-2 RMTID.011, RMTID.013 | FR-OP-034, FR-OP-037 | Built | `remittance` InsurerOrService; `opsledger` FileDropPort | **FIT** | - | - |
| CORE-10.03 | Validation and filtering criteria | 9 | BR-106, 107, 108 | BRD-2 RMTID.014, RMTID.017, RMTID.020 | FR-OP-031 | Built | `remittance` ExtractionService | **FIT** | - | - |
| CORE-10.04 | Search and view | 9 | BR-109 | BRD-2 RMTID.025, RMTID.026 | FR-OP-039, FR-OP-005 | Built | `remittance` RemittanceQueryService | **FIT** | - | - |
| CORE-10.05 | Hold remittance management | 9 | BR-110, 112, 113, 114, 115, 116 | BRD-2 RMTID.020, RMTID.031, RMTID.032 | FR-OP-031, FR-OP-005 | Built | `remittance` HoldService | **FIT** | - | - |
| CORE-10.06 | Special remittance request and processing | 9 | BR-117 | BRD-2 RMTID.030 | FR-OP-040 | Built | `remittance` SpecialRemittanceService | **FIT** | - | - |
| CORE-10.07 | Notifications and tracking | 9 | BR-111 | BRD-2 RMTID.033, RMTID.035, RMTID.036 | FR-OP-040, FR-OP-031, FR-OP-036 | Built | `remittance`; `messaging` NotificationService | **FIT** | - | - |

### CORE-11 Production Reconciliation (p.9; traceability p.34)

| ID | Capability | p. | Core BR | Covered by (BRD and IDs) | FRS | Status | BIBS module and files | Fit | Gap and proposal | Q |
|---|---|---|---|---|---|---|---|---|---|---|
| CORE-11.01 | Data extraction and file management | 9 | BR-119, 120 | BRD-2 PRCID.001, PRCID.009 | FR-OP-070, FR-OP-074 | Built | `prodrecon` ProductionExtractService, ReconUploadService | **FIT** | - | - |
| CORE-11.02 | Production register viewing and filtering | 9 | BR-121 | BRD-2 PRCID.012, PRCID.021 | FR-OP-071, FR-OP-077 | Built | `prodrecon` ReconItemService | **FIT** | - | - |
| CORE-11.03 | Matching and automation | 9 | BR-122, 123 | BRD-2 PRCID.023, PRCID.024, PRCID.033 | FR-OP-076, FR-OP-075 | Built | `prodrecon` ReconMatchingService | **FIT** | - | - |
| CORE-11.04 | Tracking and monitoring | 9 | BR-124 | BRD-2 PRCID.029, PRCID.030, PRCID.032 | FR-OP-079, FR-OP-075, FR-OP-074 | Built | `prodrecon` ProdReconHomePage | **FIT** | - | - |
| CORE-11.05 | Reports generation | 9 | - | BRD-2 PRCID.034, PRCID.035, PRCID.039 | FR-OP-071, FR-OP-080 | Built | `prodrecon` reports | **FIT** | - | - |

### CORE-12 Adjustment / Cancellation (p.9-10; traceability p.34)

| ID | Capability | p. | Core BR | Covered by (BRD and IDs) | FRS | Status | BIBS module and files | Fit | Gap and proposal | Q |
|---|---|---|---|---|---|---|---|---|---|---|
| CORE-12.01 | Transaction management (endorsements, cancellations) | 9 | BR-126 | BRD-2 ADJID.001, ADJID.003, ADJID.005 | FR-OP-050, FR-OP-053 | Built | `adjustment` EndorsementRequestService | **FIT** | - | - |
| CORE-12.02 | Automated accounting entries | 9 | BR-127, 132 | BRD-2 ADJID.011, ADJID.012 | FR-OP-056, FR-OP-057 | Built | `adjustment` AdjustmentPostingService | **FIT** | - | - |
| CORE-12.03 | Traceability and auditability | 10 | BR-128, 129 | BRD-2 ADJID.020, ADJID.022 | FR-OP-050, FR-OP-061 | Built | `adjustment`; `audit` | **FIT** | - | - |
| CORE-12.04 | Reporting and monitoring | 10 | - | BRD-2 ADJID.016, ADJID.019, ADJID.021 | FR-OP-062, FR-OP-061 | Built | `adjustment` reports | **FIT** | - | - |
| CORE-12.05 | Search and document management | 10 | BR-130, 131 | BRD-2 ADJID.024, ADJID.025 | FR-OP-005, FR-OP-061, FR-OP-052 | Built | `adjustment` AdjustmentQueryService; `attachment` | **FIT** | - | - |
| CORE-12.06 | Sending and uploading of files | 10 | - | BRD-2 ADJID.026 | FR-OP-059 | Built | `adjustment` WriteOffService (minimal-balance file) | **FIT** | - | - |
| CORE-12.07 | Batch posting | 10 | - | BRD-2 ADJID.006 | FR-OP-056 | Built | `adjustment` PostingBatchService | **FIT** | - | - |

### CORE-13 Collection of Commission Receivables (Direct Payment) (p.10; traceability p.34-35)

| ID | Capability | p. | Core BR | Covered by (BRD and IDs) | FRS | Status | BIBS module and files | Fit | Gap and proposal | Q |
|---|---|---|---|---|---|---|---|---|---|---|
| CORE-13.01 | Automated incentive calculation | 10 | BR-166, 167 | BRD-2 CMRID.005 | FR-OP-095 | Built | `commission` IncentiveEngine, IncentiveRunService | **FIT** | - | - |
| CORE-13.02 | Motor Mania incentive plan | 10 | BR-168 | BRD-2 CMRID.006 | FR-OP-095 | Built | `commission` IncentiveSchemeService | **FIT** | - | - |
| CORE-13.03 | Production data validation and exclusion handling | 10 | BR-169 | BRD-2 CMRID.003 | FR-OP-095 | Built | `commission` | **FIT** | - | - |
| CORE-13.04 | Automated commission receivables processing | 10 | BR-133 | BRD-2 CMRID.007 | FR-OP-091 | Built | `commission` DpCollectionService | **FIT** | - | - |
| CORE-13.05 | Comprehensive production reporting | 10 | BR-134 | BRD-2 CMRID.014, PRCID.035 | FR-OP-097, FR-OP-080 | Built | `commission`, `prodrecon` reports | **FIT** | - | - |
| CORE-13.06 | Risk mitigation and error handling | 10 | BR-135 | BRD-2 CMRID.008 | FR-OP-091, FR-OP-093 | Built | `commission` DpFeedbackService | **FIT** | - | - |
| CORE-13.07 | Collection of commission receivables (direct payment) | 10 | BR-052, 136, 137, 140 | BRD-2 CMRID.002, CMRID.004 | FR-OP-091, FR-OP-098 | Built | `commission` DpIntakeService, DpBillingService | **FIT** | - | - |
| CORE-13.08 | Auto-match reversals | 10 | BR-138, 139 | BRD-2 CMRID.007, CSHID.027 | FR-OP-091, FR-OP-026 | Built | `commission`, `cashiering` CwtService | **FIT** | - | - |

### CORE-14 Claims (p.10; traceability p.33)

| ID | Capability | p. | Core BR | Covered by (BRD and IDs) | FRS | Status | BIBS module and files | Fit | Gap and proposal | Q |
|---|---|---|---|---|---|---|---|---|---|---|
| CORE-14.01 | Process claims advice from BDOI Marketing | 10 | BR-141 | BRD-7 BRCLM.003, BRCLM.016 | FR-CM-010, FR-CM-011, FR-CM-015, FR-CM-014 | Being built | `brokerclaims` ClaimRecordingService (CL1-A) | **NEW** | - | - |
| CORE-14.02 | Process claims advice from client | 10 | BR-141 | BRD-7 BRCLM.003, BRCLM.006 | FR-CM-010, FR-CM-011, FR-CM-015, FR-CM-002 ... | Being built | `brokerclaims` ClaimRecordingService, LossAdviceService | **NEW** | - | - |
| CORE-14.03 | Process claims advice from insurer | 10 | BR-141 | BRD-7 BRCLM.041, BRCLM.043 | FR-CM-003, FR-CM-011, FR-CM-022, FR-CM-024 ... | Being built | `brokerclaims` InsurerUpdateService, InsurerClaimService (V1022) | **NEW** | - | - |
| CORE-14.04 | Process LOA from insurer | 10 | BR-142 | BRD-7 BRCLM.014, BRCLM.010 | FR-CM-043, FR-CM-040, FR-CM-046 | Being built | `brokerclaims` settlement type LOV | **NEW** | Repair monitoring after LOA is offline (CLQ05). | - |
| CORE-14.05 | Process settlement offer from insurer | 10 | BR-143 | BRD-7 BRCLM.010, BRCLM.014 | FR-CM-040, FR-CM-046, FR-CM-043 | Being built | `brokerclaims` claim status | **NEW** | - | - |
| CORE-14.06 | Tag permanent closure | 10 | BR-144 | BRD-7 BRCLM.035, BRCLM.005 | FR-CM-045, FR-CM-002 | Being built | `brokerclaims` (CL1-B) | **NEW** | - | - |
| CORE-14.07 | Tag temporary closure | 10 | BR-145 | BRD-7 BRCLM.035 | FR-CM-045 | Being built | `brokerclaims` (CL1-B) | **NEW** | - | - |
| CORE-14.08 | Unclaimed checks safekeeping | 10 | BR-146 | BRD-7 | - | Being built | None | **NEW** | BRD-7 has no claim money through BDOI (CLQ10). The Core traceability (p.33) maps it only to status values. | CRQ03 |
| CORE-14.09 | Handover of settlement checks to Cashiering | 10 | BR-147 | BRD-7 | - | Being built | None; `cashiering` check handling could host it | **NEW** | As CORE-14.08. | CRQ03 |
| CORE-14.10 | Retrieval of checks from Cashiering for release | 10 | BR-148 | BRD-7 | - | Being built | None | **NEW** | As CORE-14.08. | CRQ03 |
| CORE-14.11 | Maintain full claims history for audit and compliance | 10 | BR-151 | BRD-7 BRCLM.004, BRCLM.022 | FR-CM-012, FR-CM-052, FR-CM-054 | Being built | `brokerclaims`; `audit` | **NEW** | - | - |
| CORE-14.12 | Reports and analytics viewing | 10 | BR-149, 152 | BRD-7 BRCLM.026, BRCLM.029, BRCLM.030, BRCLM.031, BRCLM.032 | FR-CM-060, FR-CM-044, FR-CM-061, FR-CM-062 | Being built | `brokerclaims` reports (CL1-B) | **NEW** | - | - |
| CORE-14.13 | Encode / override the next follow-up date | 10 | BR-150 | BRD-7 BRCLM.019 | FR-CM-002, FR-CM-050, FR-CM-054 | Being built | `brokerclaims` (CL1-B) | **NEW** | - | - |

### CORE-15 Reinsurance (p.10-11; traceability p.35)

| ID | Capability | p. | Core BR | Covered by (BRD and IDs) | FRS | Status | BIBS module and files | Fit | Gap and proposal | Q |
|---|---|---|---|---|---|---|---|---|---|---|
| CORE-15.01 | Placement request initiation | 10 | BR-153 | ReInsurance BRD FRID-001-FRID-010 | - | Phase 2 | Phase 2 (ReInsurance BRD) | **OUT** | Phase 2 - BRD received. | - |
| CORE-15.02 | Placement slip management | 10 | BR-154 | ReInsurance BRD FRID-017-FRID-028 | - | Phase 2 | Phase 2 | **OUT** | Phase 2 - BRD received. | - |
| CORE-15.03 | Statement of Account (SOA) generation | 10 | BR-155, 156 | ReInsurance BRD FRID-033-FRID-041 | - | Phase 2 | Phase 2 | **OUT** | Phase 2 - BRD received. | - |
| CORE-15.04 | Claims reporting and settlement | 10 | BR-157 | ReInsurance BRD FRID-101-FRID-105 | - | Phase 2 | Phase 2 | **OUT** | Phase 2 - BRD received. | - |
| CORE-15.05 | Claims payment processing | 10 | BR-158 | ReInsurance BRD FRID-058-FRID-066 | - | Phase 2 | Phase 2 | **OUT** | Phase 2 - BRD received. | - |
| CORE-15.06 | Direct client claims payment | 11 | BR-159 | ReInsurance BRD FRID-075-FRID-082 | - | Phase 2 | Phase 2 | **OUT** | Phase 2 - BRD received. | - |
| CORE-15.07 | Receive and validate file from stakeholders (capability matrix p.28) | 28 | - | ReInsurance BRD | - | Phase 2 | Phase 2 | **OUT** | Phase 2 - BRD received. | - |
| CORE-15.08 | Notification to stakeholders (capability matrix p.28) | 28 | - | ReInsurance BRD | - | Phase 2 | Phase 2 | **OUT** | Phase 2 - BRD received. | - |
| CORE-15.09 | Log, audit and history for traceability (capability matrix p.28) | 28 | - | ReInsurance BRD | - | Phase 2 | Phase 2 | **OUT** | Phase 2 - BRD received. | - |
| CORE-15.10 | Net settlement (capability matrix p.28) | 28 | - | ReInsurance BRD | - | Phase 2 | Phase 2 | **OUT** | Phase 2 - BRD received. | - |

### CORE-16 Customer Service Facility (p.11; traceability p.35-36)

| ID | Capability | p. | Core BR | Covered by (BRD and IDs) | FRS | Status | BIBS module and files | Fit | Gap and proposal | Q |
|---|---|---|---|---|---|---|---|---|---|---|
| CORE-16.01 | Search, retrieve and display client contact and insurance account details | 11 | BR-161 | BRD-9 BRCSF-002, BRCSF-003, BRCSF-008 | FR-CSF-011, FR-CSF-021, FR-CSF-022, FR-CSF-010 | Designed | `csf` (CUSTOMER_SERVICING_DESIGN), not built | **CHANGE** | - | - |
| CORE-16.02 | View, add and update client contact information | 11 | BR-162 | BRD-9 BRCSF-004 | FR-CSF-020, FR-CSF-021 | Designed | `csf`; `crm` ClientService.updateContact; ContactSyncGateway | **CHANGE** | - | - |
| CORE-16.03 | View mode of payment (history) and current status | 11 | BR-163 | BRD-9 BRCSF-005 | FR-CSF-012, FR-CSF-013 | Designed | `csf` (reads `opsledger` paymentsOfClient) | **CHANGE** | - | - |
| CORE-16.04 | View and resend RA (and e-policy) | 11 | BR-164 | BRD-9 BRCSF-006, BRCSF-009 | FR-CSF-030, FR-CSF-033 | Designed | `csf`; document type RENEWAL_ADVICE | **NEW** | BR-164 adds the e-policy; BRCSF-006 names the RA only. | CRQ16 |
| CORE-16.05 | Upload supporting documents | 11 | - | BRD-9 BRCSF-007 | FR-CSF-032 | Designed | `csf`; `attachment` | **CHANGE** | - | - |
| CORE-16.06 | Case resolution: add / edit case details and status | 11 | BR-165 | BRD-9 | - | Designed | None (case logging stays in SharePoint) | **OUT** | BRD-9 e-mail item 10 (CSF-EM10) puts case management out of scope; the Core BRD and BR-165 keep it. | CRQ04 |

### CORE-17 Data Management (p.11; traceability p.38-41)

| ID | Capability | p. | Core BR | Covered by (BRD and IDs) | FRS | Status | BIBS module and files | Fit | Gap and proposal | Q |
|---|---|---|---|---|---|---|---|---|---|---|
| CORE-17.01 | Master data change logging (user, product, insurer, LOVs) | 11 | BR-170 | None (BRNB.016, BRNB.083, BRPM.024, BRCLXN.043 related) | FR-NB-016, FR-NB-132, FR-PM-005, FR-CL-003 | No BRD | `audit` AuditTrailService (summary text only); `lov` approval; `catalog` CatalogApprovalSource; `nbadmin` UserAccessHistory | **CHANGE** | Changes are audited per module, but no single log shows field, old value, new value and approver for every master type. | CRQ07 |
| CORE-17.02 | Insurer management | 11 | BR-171 | None (BRNB.008 related) | FR-NB-053 | No BRD | `catalog` InsurerService, InsurerProfile, InsurerBranch, CommissionRate; `party`; `remittance` rules | **CHANGE** | Insurer data is spread over the panel, commission tables, remittance rules and the chart of accounts; contacts, payment terms, remittance schedule and risk participation are not held on the insurer. | CRQ08 |
| CORE-17.03 | LOV maintenance | 11 | BR-004, 172 | BRD-1 BRNB.083, BASAU 2.2.0 | FR-NB-132, FR-AC-070 | Built | `lov` LovService (effectivity, approval); /broking-setup/lists | **FIT** | BR-004 has no priority level. | - |
| CORE-17.04 | MIS field definition | 11 | BR-173, 174 | None (BRNB.108 related) | FR-NB-110, FR-NB-119 | No BRD | `dimension` (cost centre, profit centre, department, business line); sales organisation on accounts | **NEW** | No facility to define MIS fields per entity; MIS attributes are fixed columns. | CRQ09 |
| CORE-17.05 | Product maintenance | 11 | - | BRD-3 BRPM.003, PMADD01 | FR-PM-012, FR-PM-010 | Built | `catalog`, `productmaint` | **FIT** | - | - |

### CORE-18 Emerging Capabilities (p.11; traceability none)

| ID | Capability | p. | Core BR | Covered by (BRD and IDs) | FRS | Status | BIBS module and files | Fit | Gap and proposal | Q |
|---|---|---|---|---|---|---|---|---|---|---|
| CORE-18.01 | Include any other / additional system capabilities | 11 | BR-176 | None | - | No BRD | None | **OUT** | Not a testable requirement. | CRQ10 |

### CORE-19 Employee Benefits (p.11; traceability p.36-37)

| ID | Capability | p. | Core BR | Covered by (BRD and IDs) | FRS | Status | BIBS module and files | Fit | Gap and proposal | Q |
|---|---|---|---|---|---|---|---|---|---|---|
| CORE-19.01 | Automated renewal notifications | 11 | BR-184, 196 | BRD-8 BRID-001 | FR-EB-022 | Designed | `eb` (EMPLOYEE_BENEFITS_DESIGN), not built | **NEW** | - | - |
| CORE-19.02 | Manual and system-based proposal generation | 11 | BR-185, 197 | BRD-8 BRID-003 | FR-EB-024 | Designed | `eb` | **NEW** | - | - |
| CORE-19.03 | Document and data upload management | 11 | BR-186, 198 | BRD-8 BRID-005, BRID-005.01, BRID-014, BRID-025 | FR-EB-001, FR-EB-010, FR-EB-011, FR-EB-014 ... | Designed | `eb`; `portal` (insurer uploads) | **NEW** | - | - |
| CORE-19.04 | Broker on record management | 11 | BR-187 | BRD-8 BRID-008 | FR-EB-031 | Designed | `eb` BorValidator | **NEW** | - | - |
| CORE-19.05 | Terms of Reference generation and distribution | 11 | BR-188 | BRD-8 BRID-007, BRID-009 | FR-EB-004, FR-EB-030, FR-EB-035 | Designed | `eb`; `messaging` protected e-mail | **CHANGE** | - | - |
| CORE-19.06 | Comparative report management | 11 | BR-189 | BRD-8 BRID-010, BRID-011 | FR-EB-040, FR-EB-041, FR-EB-015, FR-EB-043 | Designed | `eb` | **CHANGE** | - | - |
| CORE-19.07 | Client feedback, change, additional or amendment request capture and relay | 11 | BR-190 | BRD-8 BRID-002, BRID-012, BRID-013, BRID-015 | FR-EB-023, FR-EB-044, FR-EB-054, FR-EB-055 ... | Designed | `eb` | **NEW** | - | - |
| CORE-19.08 | Automated and manual sending of proposals | 11 | BR-191 | BRD-8 BRID-003, BRID-009 | FR-EB-024, FR-EB-035 | Designed | `eb` | **NEW** | - | - |
| CORE-19.09 | Approval workflow based on defined thresholds | 11 | BR-192 | BRD-8 BRID-016 | FR-EB-042 | Designed | `eb`; `workflow` | **CHANGE** | - | - |
| CORE-19.10 | Placement and booking management | 11 | BR-193 | BRD-8 BRID-017, BRID-019, BRID-020 | FR-EB-046, FR-EB-051, FR-EB-052 | Designed | `eb`; `placement`, `booking` | **CHANGE** | - | - |
| CORE-19.11 | Centralised reporting and analytics | 11 | BR-194 | BRD-8 BRID-022, BRID-023, BRID-024 | FR-EB-060, FR-EB-062, FR-EB-001, FR-EB-003 | Designed | `eb` reports | **NEW** | - | - |
| CORE-19.12 | Manage franchise requests and approvals | 11 | BR-195 | BRD-8 BRID-026, BRID-027, BRID-029 | FR-EB-032, FR-EB-034, FR-EB-033 | Designed | `eb` | **NEW** | BRID-028 (high-risk documents) is out of scope in BRD-8. | - |

### CORE-20 Product Maintenance (p.12; traceability p.37-38)

| ID | Capability | p. | Core BR | Covered by (BRD and IDs) | FRS | Status | BIBS module and files | Fit | Gap and proposal | Q |
|---|---|---|---|---|---|---|---|---|---|---|
| CORE-20.01 | Automated request handling | 12 | BR-199 | BRD-3 BRPM.011, BRPM.008 | FR-PM-020, FR-PM-024, FR-PM-045, FR-PM-021 | Built | `productmaint` PackageRequestService | **FIT** | - | - |
| CORE-20.02 | Automatic reference numbers | 12 | BR-200 | BRD-3 BRPM.008 | FR-PM-020, FR-PM-021 | Built | `productmaint` (PKR-yyyy-n) | **FIT** | - | - |
| CORE-20.03 | Quotation / proposal management | 12 | BR-201 | BRD-3 BRPM.012, BRPM.013 | FR-PM-030, FR-PM-031, FR-PM-034, FR-PM-036 | Built | `productmaint` NegotiationService | **FIT** | - | - |
| CORE-20.04 | Automatic comparison tables | 12 | BR-202 | BRD-3 BRPM.014, PMADD03 | FR-PM-034, FR-PM-035 | Built | `productmaint` ComparativeService | **FIT** | - | - |
| CORE-20.05 | Real-time dashboard / reports | 12 | BR-203 | BRD-3 BRPM.019, BRPM.018 | FR-PM-070, FR-PM-071 | Built | ProductMaintenanceHomePage; PM-PKG-STATUS | **FIT** | - | - |
| CORE-20.06 | Built-in checks / approvals | 12 | BR-204 | BRD-3 BRPM.021, PMADD06 | FR-PM-021, FR-PM-073, FR-PM-043 | Built | `workflow` PM_PACKAGE_REQUEST | **FIT** | - | - |
| CORE-20.07 | Secure document sharing | 12 | BR-205 | BRD-3 BRPM.020, BRPM.002 | FR-PM-004, FR-PM-002 | Built | `messaging` DocumentProtector | **FIT** | - | - |
| CORE-20.08 | Works with existing systems | 12 | BR-206 | BRD-3 BRPM.022 | FR-PM-072 | Built | ProductMasterFeed port (logs only) | **CONFIGURE** | Target systems not named (PQ16). | - |
| CORE-20.09 | Track all changes | 12 | BR-207 | BRD-3 BRPM.024 | FR-PM-005 | Built | `audit` | **FIT** | - | - |
| CORE-20.10 | Expiring packages monitoring | 12 | BR-208 | BRD-3 BRPM.017, BRPM.006 | FR-PM-044, FR-PM-060, FR-PM-061, FR-PM-071 ... | Built | `productmaint` PackageExpiryService | **FIT** | - | - |

### CORE-21 Report Generation (p.12; traceability none)

| ID | Capability | p. | Core BR | Covered by (BRD and IDs) | FRS | Status | BIBS module and files | Fit | Gap and proposal | Q |
|---|---|---|---|---|---|---|---|---|---|---|
| CORE-21.01 | Generate standard reports for operational and analytical purposes | 12 | BR-054 | BRD-1 BRNB.057, BRNB.075 | FR-NB-123, FR-NB-122 | Built | `report` ReportRegistry (174 report definitions); /reports | **FIT** | - | - |
| CORE-21.02 | Extract, download and print reports in multiple formats | 12 | BR-078 | BRD-1 BRNB.031, BRNB.037 | FR-NB-124, FR-NB-125 | Built | `report` ExportFormat (XLSX, PDF, CSV, ODS, XML, DOCX); print with metadata | **FIT** | - | - |
| CORE-21.03 | Create tailored reports by parameters, filters and business requirements | 12 | BR-053 | BRD-1 BRNB.057 | FR-NB-123 | Built | `report` parameters; `nbreport` ReportVariantService (saved variants, all reports) | **FIT** | Q40 is partly answered: saved variants and column filters are enough for EB and the Report List. | - |
| CORE-21.04 | Dynamic customisation: data fields, charts and summaries | 12 | BR-053 | None | - | No BRD | `report` ReportTable (fixed columns, no chart) | **CHANGE** | The user cannot choose columns, group or summarise, or add a chart to a report. | CRQ11 |
| CORE-21.05 | Scheduled or on-demand report generation | 12 | BR-054, 103 | None (DIS 3.28.0, BRCLXN.028 related) | FR-DS-081, FR-CL-082 | No BRD | `report` ReportService (on demand), ReportArchiveService and ReportBatchService; jobs in `system` JobScheduler | **CHANGE** | Only reports wired to a system job run on a schedule; a user cannot schedule a report and its recipients. | CRQ11 |


## 6. Cross-cutting requirements (not in the bullet list)

| ID | Requirement | p. | Core BR | Covered by | FRS | Status | BIBS module and files | Fit | Gap and proposal | Q |
|---|---|---|---|---|---|---|---|---|---|---|
| XC-01 | Log-in, password reset, several types of user access, user administration | 13 | BR-000, BR-001, BR-002, BR-003 | BRD-11 BRNB.040, BRNB.084 | FR-NB-130, FR-NB-134 | Built | `security`, `nbadmin` (V1060-V1063); PasswordResetLinkRequest | **FIT** | Directory sign-in parked (D6, UQ04). | - |
| XC-02 | Centralised, real-time dashboards for all roles; details invoked from the dashboard | 5, 20-21 | BR-125, BR-152, BR-160, BR-203 | None (BRNB.012, BRQID.003, BRPM.019 related) | FR-NB-120, FR-OP-003, FR-PM-070 | No BRD | `dashboard` DashboardService (finance widgets); NbDashboardPage; section home pages (Operations, Cashiering, Remittance, Collections, ACSL, FRBS, Disbursement, Product Maintenance, Screening) | **CHANGE** | No role-based landing page; the Dashboard at / shows finance widgets to every holder of DASHBOARD_VIEW; the dashboard contents are 'to be defined' in BR-125, 152, 160. | CRQ06 |
| XC-03 | Automated notifications, approvals and feedback tracking | 5, 19, 24 | BR-083, BR-111, BR-206 | None (BRNB.015, RMTID.033 related) | FR-NB-015, FR-OP-040 | Built | `messaging` NotificationService, NotificationPreferenceService, MailDispatchJob; `workflow`; `alert` | **FIT** | - | - |
| XC-04 | Audit logs and full transaction history | 4-5, 20, 25 | BR-124, BR-129, BR-151, BR-207 | None (BRNB.016, BRNB.086, BRNB.089 related) | FR-NB-016, FR-NB-136 | Built | `audit` AuditTrailService; /admin/audit; AuditTrailReport | **FIT** | - | - |
| XC-05 | Workflow maintained at the back end; data flows to the next process by rules | 15, 18, 23 | BR-051, BR-101, BR-177, BR-182 | None (BRNB.096, BRNB.022 related) | FR-NB-011, FR-NB-064, FR-NB-010, FR-NB-122 | Built | `workflow` WorkflowService (definitions in tables, e.g. PM_PACKAGE_REQUEST V755); `events` outbox | **CONFIGURE** | Forwarding files to other BDOI systems waits for the target systems (Q08). | - |
| XC-06 | Batch and individual processing; concurrent users and high-volume bulk uploads | 4, 16 | BR-063 | None (BRNB.064, BRQID.006 related) | FR-NB-019, FR-NB-065, FR-OP-008 | Built | `bulk` framework; job lock (V34) | **FIT** | - | - |
| XC-07 | Currency selection and multi-currency support | 15, 18 | BR-027, BR-088 | None (BRCLM.009 related) | FR-CM-011 | Built | `currency`; account currency (default PHP); FX revaluation | **FIT** | - | - |
| XC-08 | 30-day hold cover request, assigned to a role | 14 | BR-034 | BRD-1 BRNB.072, BRNB.103 | FR-NB-082, FR-NB-083 | Built | `placement` HoldCoverService | **FIT** | - | - |
| XC-09 | Tag direct payment (DP) accounts | 14 | BR-035 | BRD-1 BRNB.114 | FR-NB-069, FR-NB-092 | Built | `account` AccountTaggingService; /accounts/direct-payment | **FIT** | - | - |
| XC-10 | SOA generated with placement report and Insurance Advice; service invoice to insurer issued with the SOA at booking | 14-15 | BR-039, BR-043 | BRD-1 BRNB.100, BRCLXN.058 | FR-NB-117, FR-CL-060, FR-CL-061 | Built | `booking` ServiceInvoiceTriggers; `collections` BillingStatementService (client SOA per billing cycle) | **CHANGE** | The service invoice is issued at booking; no SOA is issued at booking. | CRQ12 |
| XC-11 | Billing reports with premium and loan details; payment reports matched to accounts | 15 | BR-049, BR-050 | BRD-1 BRNB.067, BRNB.068 | FR-NB-090, FR-NB-091, FR-NB-092 | Built | `placement` BillingService, PaymentReportService | **FIT** | - | - |
| XC-12 | BIR standard books (sales, purchase, cash receipts, cash disbursements, general journal) | 17 | BR-084 | BRD-5 FRBS 3.2.0 | FR-AC-060, FR-AC-061, FR-AC-062, FR-AC-063 | Built | `tax` BookOfAccountsReport (V703 tax_book_def) | **FIT** | CAS / DAT format open (AQ07). | - |
| XC-13 | Invoice Master List across workflows | 23 | BR-175 | None (BRCLXN.001, ACSL 2.16.0 related) | FR-CL-010, FR-AS-026 | No BRD | `opsledger` InvoiceLedgerQueryService; Operations > Invoice Ledger | **CHANGE** | Cover number, version number, Marketing / Processing assignees, placement status and approval dates are not all columns of the ledger. | CRQ13 |
| XC-14 | BDO brand colours, logos, icons and design system | 23 | BR-178, BR-179, BR-180 | None | - | No BRD | BDO_UX_GUIDELINES; brand tokens; screen-alignment pass (deliverable 18) | **CHANGE** | BDO's prescribed icon and illustration set has not been supplied. | CRQ17 |
| XC-15 | Identified user journeys and customisable interaction flows (with a demo) | 23 | BR-181, BR-182 | None | - | No BRD | Persona deck (deliverable 7); process deck (deliverable 41); `workflow` | **FIT** | - | - |
| XC-16 | Vendor uses Figma for design execution | 23 | BR-183 | None | - | No BRD | Not a system function | **OUT** | Vendor qualification, answered in the RFI. | - |
| XC-17 | Regulatory compliance: BIR and Insurance Commission reportorial requirements | 4 | - | BRD-5 FRBS 3.2.0 | FR-AC-060, FR-AC-061, FR-AC-062, FR-AC-063 | Built | `tax` (BIR forms, 2307, alphalists), IC schedules | **CHANGE** | The IC broker's annual statement layout is open (AQ07). | - |
| XC-18 | Retail and wholesale business | 4 | - | BRD-1 BRNB.001 | FR-NB-001 | Built | Market segment on sales organisation and accounts | **FIT** | - | - |
| XC-19 | Invoice batch printing, delivery to insurers by SFTP and delivery report (daily, per insurer) | 43 | - | None (BRNB.100 related) | FR-NB-117 | No BRD | `booking` ServiceInvoiceDispatch (e-mail per invoice); `opsledger` FileDropPort | **CHANGE** | No daily batch file per insurer and no SFTP transport. | CRQ18 |
| XC-20 | Client migration (one-time, 2020 to present) and daily midday / EOD client batches with modification report | 43 | - | Data Migration BRD | - | No BRD | None; `crm` bulk CLIENT_CREATE is the load path | **NEW** | Depends on the Data Migration BRD (BRID 2.1); the daily batches imply a coexistence feed. | CRQ19 |
| XC-21 | ALeA e-mail address encoding (as needed) | 43 | - | None | - | No BRD | None | **OUT** | System and purpose not stated. | CRQ20 |
| XC-22 | Daily synchronisation from source systems (CMS, Reinsurance) | 42-43 | - | BRD-4 BRCLXN.013, BRCLXN.014, BRCLXN.015 | FR-CL-017 | Built | `collections` InboxService (EBIX feed seam) | **CONFIGURE** | Needed only while legacy invoices remain (Data Migration BRID 6-9). | - |
| XC-23 | MIS LOV, QPS insurer, LGT rates and insurer branch maintenance (MILB, 24 a year) | 45 | - | BRD-1 BRNB.083 | FR-NB-132 | Built | `lov`; `catalog` InsurerBranch (LGT rate) | **FIT** | - | - |

## 7. BR table concordance (BR-000 to BR-208)

Every BR ID of pp.13-25 and the row that covers it. BR-004 is the only row without a priority level. BR-196, BR-197
and BR-198 repeat BR-184, BR-185 and BR-186 word for word (section 14, O3).

| BR ID | Row | BR ID | Row | BR ID | Row |
|---|---|---|---|---|---|
| BR-000 (p.13) | XC-01 | BR-070 (p.16) | CORE-08.05 | BR-140 (p.21) | CORE-13.07 |
| BR-001 (p.13) | XC-01 | BR-071 (p.16) | CORE-08.06 | BR-141 (p.21) | CORE-14.01, CORE-14.02, CORE-14.03 |
| BR-002 (p.13) | XC-01 | BR-072 (p.16) | CORE-08.07 | BR-142 (p.21) | CORE-14.04 |
| BR-003 (p.13) | XC-01 | BR-073 (p.16) | CORE-08.08 | BR-143 (p.21) | CORE-14.05 |
| BR-004 (p.13) | CORE-17.03 | BR-074 (p.16) | CORE-08.11 | BR-144 (p.21) | CORE-14.06 |
| BR-005 (p.13) | CORE-01.01 | BR-075 (p.16) | CORE-08.12 | BR-145 (p.21) | CORE-14.07 |
| BR-006 (p.13) | CORE-01.02 | BR-076 (p.16) | CORE-08.12 | BR-146 (p.21) | CORE-14.08 |
| BR-007 (p.13) | CORE-01.03 | BR-077 (p.16) | CORE-08.13, CORE-08.16, CORE-08.17 | BR-147 (p.21) | CORE-14.09 |
| BR-008 (p.13) | CORE-01.04 | BR-078 (p.17) | CORE-08.14, CORE-08.15, CORE-21.02 | BR-148 (p.21) | CORE-14.10 |
| BR-009 (p.13) | CORE-01.05 | BR-079 (p.17) | CORE-08.18 | BR-149 (p.21) | CORE-14.12 |
| BR-010 (p.13) | CORE-01.05 | BR-080 (p.17) | CORE-08.19 | BR-150 (p.21) | CORE-14.13 |
| BR-011 (p.13) | CORE-01.05 | BR-081 (p.17) | CORE-08.20 | BR-151 (p.21) | CORE-14.11, XC-04 |
| BR-012 (p.13) | CORE-01.06 | BR-082 (p.17) | CORE-08.20 | BR-152 (p.21) | CORE-14.12, XC-02 |
| BR-013 (p.13) | CORE-01.07 | BR-083 (p.17) | XC-03 | BR-153 (p.21) | CORE-15.01 |
| BR-014 (p.13) | CORE-02.01 | BR-084 (p.17) | CORE-08.14, XC-12 | BR-154 (p.21) | CORE-15.02 |
| BR-015 (p.13) | CORE-02.02 | BR-085 (p.18) | CORE-08.22 | BR-155 (p.22) | CORE-15.03 |
| BR-016 (p.13) | CORE-02.02 | BR-086 (p.18) | CORE-09.01 | BR-156 (p.22) | CORE-15.03 |
| BR-017 (p.13) | CORE-02.03 | BR-087 (p.18) | CORE-09.02 | BR-157 (p.22) | CORE-15.04 |
| BR-018 (p.13) | CORE-02.04 | BR-088 (p.18) | XC-07 | BR-158 (p.22) | CORE-15.05 |
| BR-019 (p.13) | CORE-02.05 | BR-089 (p.18) | CORE-09.03 | BR-159 (p.22) | CORE-15.06 |
| BR-020 (p.13) | CORE-02.06 | BR-090 (p.18) | CORE-07.04, CORE-09.06 | BR-160 (p.22) | XC-02 |
| BR-021 (p.13) | CORE-02.07 | BR-091 (p.18) | CORE-09.07 | BR-161 (p.22) | CORE-16.01 |
| BR-022 (p.14) | CORE-02.08 | BR-092 (p.18) | CORE-09.04 | BR-162 (p.22) | CORE-16.02 |
| BR-023 (p.14) | CORE-03.01 | BR-093 (p.18) | CORE-09.06 | BR-163 (p.22) | CORE-16.03 |
| BR-024 (p.14) | CORE-03.01 | BR-094 (p.18) | CORE-09.08 | BR-164 (p.22) | CORE-16.04 |
| BR-025 (p.14) | CORE-03.01 | BR-095 (p.18) | CORE-09.08 | BR-165 (p.22) | CORE-16.06 |
| BR-026 (p.14) | CORE-06.03 | BR-096 (p.18) | CORE-09.08 | BR-166 (p.22) | CORE-13.01 |
| BR-027 (p.14) | XC-07 | BR-097 (p.18) | CORE-09.09 | BR-167 (p.22) | CORE-13.01 |
| BR-028 (p.14) | CORE-03.01 | BR-098 (p.18) | CORE-09.10 | BR-168 (p.22) | CORE-13.02 |
| BR-029 (p.14) | CORE-03.02 | BR-099 (p.18) | CORE-09.10 | BR-169 (p.22) | CORE-13.03 |
| BR-030 (p.14) | CORE-03.03 | BR-100 (p.18) | CORE-09.11 | BR-170 (p.22) | CORE-17.01 |
| BR-031 (p.14) | CORE-03.04 | BR-101 (p.18) | XC-05 | BR-171 (p.22) | CORE-17.02 |
| BR-032 (p.14) | CORE-05.01 | BR-102 (p.18) | CORE-10.01 | BR-172 (p.22) | CORE-17.03 |
| BR-033 (p.14) | CORE-03.05 | BR-103 (p.18) | CORE-10.01, CORE-21.05 | BR-173 (p.23) | CORE-17.04 |
| BR-034 (p.14) | XC-08 | BR-104 (p.19) | CORE-10.02 | BR-174 (p.23) | CORE-17.04 |
| BR-035 (p.14) | XC-09 | BR-105 (p.19) | CORE-10.02 | BR-175 (p.23) | XC-13 |
| BR-036 (p.14) | CORE-05.02 | BR-106 (p.19) | CORE-10.03 | BR-176 (p.23) | CORE-18.01 |
| BR-037 (p.14) | CORE-05.04 | BR-107 (p.19) | CORE-10.03 | BR-177 (p.23) | XC-05 |
| BR-038 (p.14) | CORE-03.06 | BR-108 (p.19) | CORE-10.03 | BR-178 (p.23) | XC-14 |
| BR-039 (p.14) | CORE-05.03, CORE-05.05, XC-10 | BR-109 (p.19) | CORE-10.04 | BR-179 (p.23) | XC-14 |
| BR-040 (p.14) | CORE-05.06 | BR-110 (p.19) | CORE-10.05 | BR-180 (p.23) | XC-14 |
| BR-041 (p.14) | CORE-05.07 | BR-111 (p.19) | CORE-10.07, XC-03 | BR-181 (p.23) | XC-15 |
| BR-042 (p.14) | CORE-05.08 | BR-112 (p.19) | CORE-10.05 | BR-182 (p.23) | XC-05, XC-15 |
| BR-043 (p.15) | XC-10 | BR-113 (p.19) | CORE-10.05 | BR-183 (p.23) | XC-16 |
| BR-044 (p.15) | CORE-07.01 | BR-114 (p.19) | CORE-10.05 | BR-184 (p.23) | CORE-19.01 |
| BR-045 (p.15) | CORE-07.01 | BR-115 (p.19) | CORE-10.05 | BR-185 (p.23) | CORE-19.02 |
| BR-046 (p.15) | CORE-07.04 | BR-116 (p.19) | CORE-10.05 | BR-186 (p.24) | CORE-19.03 |
| BR-047 (p.15) | CORE-07.04 | BR-117 (p.19) | CORE-10.06 | BR-187 (p.24) | CORE-19.04 |
| BR-048 (p.15) | CORE-07.02 | BR-118 (p.19) | CORE-07.03 | BR-188 (p.24) | CORE-19.05 |
| BR-049 (p.15) | CORE-07.06, XC-11 | BR-119 (p.19) | CORE-11.01 | BR-189 (p.24) | CORE-19.06 |
| BR-050 (p.15) | XC-11 | BR-120 (p.20) | CORE-11.01 | BR-190 (p.24) | CORE-19.07 |
| BR-051 (p.15) | XC-05 | BR-121 (p.20) | CORE-11.02 | BR-191 (p.24) | CORE-19.08 |
| BR-052 (p.15) | CORE-13.07 | BR-122 (p.20) | CORE-11.03 | BR-192 (p.24) | CORE-19.09 |
| BR-053 (p.15) | CORE-02.09, CORE-07.05, CORE-21.03, CORE-21.04 | BR-123 (p.20) | CORE-11.03 | BR-193 (p.24) | CORE-19.10 |
| BR-054 (p.15) | CORE-07.05, CORE-21.01, CORE-21.05 | BR-124 (p.20) | CORE-11.04, XC-04 | BR-194 (p.24) | CORE-19.11 |
| BR-055 (p.15) | CORE-06.01 | BR-125 (p.20) | XC-02 | BR-195 (p.24) | CORE-19.12 |
| BR-056 (p.15) | CORE-06.02 | BR-126 (p.20) | CORE-12.01 | BR-196 (p.24) | CORE-19.01 |
| BR-057 (p.15) | CORE-06.03 | BR-127 (p.20) | CORE-12.02 | BR-197 (p.24) | CORE-19.02 |
| BR-058 (p.15) | CORE-06.04 | BR-128 (p.20) | CORE-12.03 | BR-198 (p.24) | CORE-19.03 |
| BR-059 (p.16) | CORE-06.04 | BR-129 (p.20) | CORE-12.03, XC-04 | BR-199 (p.24) | CORE-20.01 |
| BR-060 (p.16) | CORE-06.06, CORE-06.07 | BR-130 (p.20) | CORE-12.05 | BR-200 (p.24) | CORE-20.02 |
| BR-061 (p.16) | CORE-06.06, CORE-06.07 | BR-131 (p.20) | CORE-12.05 | BR-201 (p.24) | CORE-20.03 |
| BR-062 (p.16) | CORE-06.08 | BR-132 (p.20) | CORE-12.02 | BR-202 (p.25) | CORE-20.04 |
| BR-063 (p.16) | XC-06 | BR-133 (p.20) | CORE-13.04 | BR-203 (p.25) | CORE-20.05, XC-02 |
| BR-064 (p.16) | CORE-06.09 | BR-134 (p.20) | CORE-13.05 | BR-204 (p.25) | CORE-20.06 |
| BR-065 (p.16) | CORE-08.01 | BR-135 (p.20) | CORE-13.06 | BR-205 (p.25) | CORE-20.07 |
| BR-066 (p.16) | CORE-08.10 | BR-136 (p.20) | CORE-13.07 | BR-206 (p.25) | CORE-20.08, XC-03 |
| BR-067 (p.16) | CORE-08.02 | BR-137 (p.21) | CORE-13.07 | BR-207 (p.25) | CORE-20.09, XC-04 |
| BR-068 (p.16) | CORE-08.03 | BR-138 (p.21) | CORE-13.08 | BR-208 (p.25) | CORE-20.10 |
| BR-069 (p.16) | CORE-08.04 | BR-139 (p.21) | CORE-07.03, CORE-13.08 |  |  |

## 8. Gaps: capabilities with no BRD in the set

| # | Gap | Rows | What exists in BIBS | Proposal | Q |
|---|---|---|---|---|---|
| G1 | **Reinsurance** (capability 15) | CORE-15.01-15.10 | The finance-suite `reinsurance` module (`reinsurance/**`, `features/reinsurance`: treaties, cessions, FAC placements, claim recoveries, SOA with `SoaDialog`) is an **insurer-side** (cedant) model. BDOI acts as a reinsurance broker between cedant and reinsurers; the models do not match | **Phase 2 - BRD received** (ReInsurance BRD, 32 pages). Not built in phase 1. Phase-1 seams to keep open are in the impact document, section 6 | - |
| G2 | Claims cheque custody: safekeeping, hand-over to and retrieval from Cashiering | CORE-14.08-14.10 | `cashiering` receives and releases cheques for premium; `brokerclaims` tracks status values only | Parked until CRQ03 (with CLQ10). If yes: a cheque custody register in `brokerclaims` (Claims range V1026-V1029; V1025 is held for the legacy migration) using the cashiering check hand-over | CRQ03 |
| G3 | CSF case resolution | CORE-16.06 | Nothing; BRD-9 CSF-EM10 keeps case logging in SharePoint | Out until CRQ04 is answered; if in, a CSF case entity in the CSF range (V1043-V1049) | CRQ04 |
| G4 | Master data change logging | CORE-17.01 | `audit_log` summary rows; approvals of LOV, catalog and access changes | Field-level change log for registered master entities, with a report (FR-CR-031; V1091) | CRQ07 |
| G5 | Insurer management | CORE-17.02 | Insurer panel (`catalog` InsurerProfile, InsurerBranch, CommissionRate), `party`, remittance rules, GL control accounts | One Insurer page with the missing attributes (contacts, payment terms, remittance schedule, risk participation) (FR-CR-061; V1095) | CRQ08 |
| G6 | MIS field definition | CORE-17.04 | Fixed MIS columns; `dimension` (4 types) | MIS field catalogue that names each field per entity and feeds report filters, grouping and dashboards (FR-CR-062; V1092) | CRQ09 |
| G7 | Report customisation (fields, charts, summaries) | CORE-21.04 | Fixed report columns; saved parameter variants | Layout in the saved variant: columns, grouping, subtotals, one chart (FR-CR-042; V1093) | CRQ11 |
| G8 | User-scheduled reports | CORE-21.05 | System jobs that archive files (`ReportArchiveService`), report batches | Report subscriptions: variant, schedule, format, recipients (FR-CR-043; V1093) | CRQ11 |
| G9 | Emerging capabilities | CORE-18.01 | - | No build; BDOI lists them or the row is removed | CRQ10 |
| G10 | ALeA e-mail address encoding | XC-21 | - | No build until the system is named | CRQ20 |
| G11 | Role home dashboards | XC-02 | Finance dashboard, NB dashboard, section homes | Role home page composed of widgets per role (FR-CR-010, 011; V1094) | CRQ06 |
| G12 | Invoice Master List | XC-13 | Operations invoice ledger | Add the missing columns and a saved "Invoice Master List" view (FR-CR-090) | CRQ13 |
| G13 | Insurer invoice batch printing and SFTP delivery | XC-19 | E-mail of each service invoice | Daily batch per insurer through an outbound file port (FR-CR-081; V1096) | CRQ18 |
| G14 | Client migration and daily client batches | XC-20 | `crm` bulk client load | Belongs to the Data Migration programme (FR-CR-082) | CRQ19 |

## 9. Conflicts with the per-BRD specs

| # | Umbrella says | BRD / spec says | Effect | Q |
|---|---|---|---|---|
| CF-01 | Case resolution, add / edit case details and status (capability 16, BR-165 p.22) | BRD-9: case management out of scope, SharePoint interim (CSF-EM10, `BDOI_CSF_BRD_SPEC.md` §5) | Build of a case module or not | CRQ04 |
| CF-02 | Unclaimed checks safekeeping, hand-over to and retrieval from Cashiering (BR-146-148 p.21) | BRD-7: no claim money through BDOI; CLQ10 open (`BDOI_CLM_BRD_SPEC.md` §3, §11) | Cheque custody in Claims and Cashiering | CRQ03 |
| CF-03 | Response time under 5 seconds for every role (p.42) | BRD-1 and BRD-6 10 s (reports and RA 20 s), BRD-11 10 / 20 s, BRD-12 2 s, BRD-9 3 s, BRD-4 reports 3-15 min, BRD-7 reports 5 min | NFR set and performance tests | CRQ21 |
| CF-04 | Users: EB 13 / 13, Claims 67 / 26, Product Maintenance 13 / 13, total 1,344 named / 429 concurrent (p.42) | BRD-8 20 internal; BRD-7 47 / 25; BRD-3 Marketing 485 / 145, MBS 5 / 5, TSU 13 / 13; register sizing 150 concurrent + 20% | Sizing (429 concurrent, 388 without Reinsurance) | CRQ21, CRQ24 |
| CF-05 | Retention 5 years online, 15 years archive, backup every 4 hours kept 5 years, for every log and historical data (p.45) | BRD-7 10 / 15; BRD-5, BRD-10, BRD-12 5 / 5; backups daily (BRD-5, 10, 12) or every 15 minutes (BRD-9); backup retention 7 years (BRD-2, BRD-8) | Retention rules and backup policy | CRQ22 |
| CF-06 | "Non-Renewal Letter (NRL)" (p.8) | BRD-6: "Not for Renewal Letter (NFR)" | Letter names and templates | CRQ14 |
| CF-07 | Ten BRDs (p.3); a "User & Data Management" BRD in the traceability (p.38-41) | Twelve BRDs; no "User & Data Management" BRD; BRD-10, BRD-11, BRD-12 not listed | Ownership of capability 17 | CRQ01 |
| CF-08 | Product Maintenance traced to BRQID.004-025 (p.37-38) | BRD-3 final numbering is BRPM.001-024 and PMADD01-08; BRQID is the superseded 16-Nov-2025 version | Traceability | CRQ23 |
| CF-09 | "Generate SOA, placement report, insurance advise" (BR-039); service invoice to the insurer "simultaneously with SOA during booking" (BR-043) | BRD-1 books with a service invoice (BRNB.100); the client SOA is a Collections billing statement per cycle (BRCLXN.058) | Which SOA at booking | CRQ12 |
| CF-10 | "View and resend RA / ePolicy" (BR-164) | BRD-9 BRCSF-006: RA only; BRCSF-009 lists quotations, RAs and claims reports | CSF resend scope | CRQ16 |
| CF-11 | Client migration "from Broker and source, 2020 to present", plus daily midday and EOD client batches (p.43) | Data Migration BRD: selective migration of clean master data; legacy read-only for history (BRID 2.1, 11.1) | Migration scope and coexistence feeds | CRQ19 |
| CF-12 | Anonymisation of data over time: "No" (p.46) | Hosting appendix: non-production data masked | No conflict: masking applies to non-production copies only; recorded for the security mapping (deliverable 26) | - |
| CF-13 | Delete of prospect / record is Must have (BR-008) | BRD-1 has no delete; the traceability row has no BRD ID (p.30) | Deactivation vs deletion | CRQ05 |

## 10. Requirement traceability pages (pp.30-41): observations

The traceability cites the BRD versions that preceded the ones BIBS is built from:

| Area | IDs cited by the umbrella | IDs in the BIBS spec | Mapping |
|---|---|---|---|
| New Business | "BRD 1.x.x / 2.x.x / 3.x.x" of NB Fire & Motor (legacy) | BRNB.001-115 | `BDOI_NB_BRD_SPEC.md` keeps the legacy IDs next to each BRNB |
| Renewal | "BRD 1.003.1 ... 4.09.7" of RMEL Phase 2 | BRRN.001-040 and the main-BRD IDs | `BDOI_RN_BRD_SPEC.md` §5 lists both |
| Collections | CMS FRID-001-047 | BRCLXN.001-064 (FRID in brackets) | One to one for 001-048 |
| Claims | RQID-008-024 and FRID-010-035 | BRCLM.001-043 (FRID and RQID in brackets) | One to one |
| Operations | CSHID, RMTID, PRCID, ADJID, CMRID, BRQID | Same | - |
| Product Maintenance | BRQID.004-025 | BRPM.001-024, PMADD01-08 | Superseded numbering (CF-08) |
| Employee Benefits | BRID-001-029 | Same | - |
| CSF | BRID.001-011 | BRCSF-001-011 | Same numbers, other prefix |
| Reinsurance | FRID-001-105 | ReInsurance BRD FRID | Phase 2 |

Other points: the Level 2 names of the traceability differ from the bullets in places ("Check Renewal Payment",
"Client Confirmation", "Batch Processing and Automation" appear only in the matrix and traceability); "Delete of
prospect / record" has no BRD ID (p.30); claims items "Unclaimed Checks Safekeeping", "Handover to Cashiering" and
"Retrieval from Cashiering" are traced only to status values ("may relate", "potentially", p.33); Product Maintenance
"Automatic reference numbers" has no BRD ID (p.37).

## 11. Non-functional requirements

### 11.1 Users and response time (p.42)

| Role in the umbrella | Max / concurrent (p.42) | Per-BRD value | Response (p.42) | Per-BRD response |
|---|---|---|---|---|
| System Admin, BU Admin | blank | BRD-5 admin 7; BRD-11 requestors 14 / 5, approvers 8 / 3 | < 5 s | BRD-11 10 s, reports 20 s |
| New Business Fire and Motor | 485 / 145 | BRD-1 485 / 145 | < 5 s | BRD-1 10 s |
| Collection Management System | 130 / 56 | BRD-4 130 / 56 | < 5 s | BRD-4 5 s; reports 3-15 min |
| RMEL Phase 2 - Online Dispositioning | 75 / 23 | BRD-6 Marketing TL 75 / 23 (plus AO 319 / 50, Processing 50 / 15 and 73 / 22, reports 485 / 145) | < 5 s | BRD-6 10 s; reports and RA 20 s |
| Accounting / Disbursement / ACSL | 5 / 5; 8 / 8; 394 / 40 | BRD-5 Accounting 5, Disbursement 8, ACSL 6, Marketing 394 / 40 | < 5 s | BRD-5 load 5-10 s, save 2-5 s |
| Claims | 67 / 26 | BRD-7 HO 28 / 15, branches 9 / 5, Marketing 10 / 5 (47 / 25) | < 5 s | BRD-7 5 s; reports 5 min |
| Operations (5 units) | 16 / 16, 4 / 4, 4 / 4, 6 / 6, 5 / 5 | BRD-2 about 50 named by unit | < 5 s | BRD-2 under 5 s |
| Reinsurance | 95 / 41 | ReInsurance BRD (phase 2) | < 5 s | - |
| Customer Service Facility | 24 / 24 | BRD-9 24 concurrent | < 5 s | BRD-9 under 3 s |
| Employee Benefits | 13 / 13 | BRD-8 20 internal; portal users not sized | < 5 s | BRD-8 blank |
| Product Maintenance | 13 / 13 | BRD-3 Marketing 485 / 145, MBS 5 / 5, TSU 13 / 13 | < 5 s | BRD-3 under 5 s |
| (not listed) Sanction Screening, Submitted Policies | - | BRD-10 about 300 named; BRD-12 38 / 21 | - | BRD-10 3-5 s; BRD-12 2 s |
| **Total** | **1,344 / 429** (388 concurrent without Reinsurance) | Register proposal: 150 concurrent + 20% a year | | Register proposal: p95 under 2 s for screens |

The umbrella adds the rows of every BRD as if the users were different people. Marketing users appear in New
Business, Renewal, Collections, Accounting (394 Marketing requestors) and Product Maintenance, so 429 concurrent
sessions is an upper bound, not a sizing figure. It is still well above the 150 concurrent users the register
proposes (NFR comparison, theme "Users"). CRQ21 asks BDOI which figure governs; the performance plan (deliverable 28)
should test 429 concurrent sessions as the peak case.

### 11.2 Transaction volumes (pp.42-45)

| Source | Transaction | Frequency | Volume | BIBS note |
|---|---|---|---|---|
| Claims | Booking and validation 55; status and settlement updates 1,096; activity logs 1,206; reports 1-5; e-mails 50 | Daily / weekly / monthly | as listed | Same as BRD-7 |
| Collection Management | Dispositions 1,200; reports 1,200 daily, 60,000 weekly, 26,400 monthly; autopay files 1,200; reversals 12,000; daily sync 1,200 | Daily / weekly / monthly | as listed | The 60,000 weekly report runs are per unit and branch (BRCLXN.028) |
| Reinsurance | Placement, slip, payment and claims requests 50; e-mails 100; uploads 100; reports 20 daily / weekly / monthly; sync 1,200 | Weekly / daily | as listed | Phase 2 |
| ALeA | E-mail address encoding | As needed | Not specified | Unknown system (CRQ20) |
| Invoice Batch Printing | Batch invoice file printing; delivery to insurers by SFTP; delivery report | Daily | Per insurer | XC-19 |
| Client Migration | One-time migration "from Broker and source"; midday batch (new transactions); EOD batch (new clients); client modification report | One-time / daily | 2020 to present | XC-20, Data Migration |
| Operations | Receipts, validation, searches, reports, AR / OR batch printing, remittance, extracts, endorsements | Daily / as needed | blank | Same as BRD-2 |
| New Business Fire and Motor | 13 transaction types at 21,200 a month; endorsements 1,300 | Monthly | as listed | Same as BRD-1 |
| RMEL Phase 2 | 14 transaction types at 25,800 a month | Monthly | as listed | Same as BRD-6 |
| Customer Service Facility | CSF | Annual | 144,400 | About 12,000 a month |
| MILB | MIS LOV, QPS insurer, LGT rates and insurer branch maintenance | Annual | 24 | XC-23 |
| Accounting, Disbursement and ACSL | Accounting 150-400; Disbursement 1,000; Marketing refund and cash advance 302 | Annual | as listed | BRD-5 values |

### 11.3 Demand, availability, retention (pp.44-46) and the hosting appendix

| Topic | Umbrella | Per-BRD (register NFR comparison) | Hosting appendix | Assessment |
|---|---|---|---|---|
| Peak and demand | "Refer to BRD" | Month-end; 08:00-10:00, 15:00-17:00 (BRD-6); 15th and 30th (BRD-3) | - | No new value |
| Availability, hours, downtime, maintenance, BCP | "Refer to BRD" | 99.9% to 99.99%; hours from 06:00-22:00 to business hours; maintenance 00:00-04:00 to 19:00-07:00 | AWS ap-southeast-1 | No new value; register proposal stands (99.9% in service hours, 06:00-22:00 Mon-Sat, window 00:00-04:00) |
| Retention (application, database, audit logs, historical data) | 5 years online, 15 years offline | 5 / 15 (BRD-1, 2, 4, 8, 9); 10 / 15 (BRD-7); 5 / 5 (BRD-5, 10, 12); "follow QPS" (BRD-6, 11) | Migration staging purged within 5 days | The umbrella confirms the register default (5 / 15) as the platform rule; BRD-7's 10 years online and the 5 / 5 BRDs remain exceptions (CRQ22) |
| Backup frequency and retention | Every 4 hours; kept 5 years | Daily (BRD-5, 10, 12); every 15 minutes (BRD-9); kept 7 years (BRD-2, BRD-8) | - | Register proposal (continuous log archiving plus a base backup every 4 hours; 7 years) meets the umbrella |
| Anonymisation | "No" | Not asked elsewhere | Non-production data masked | Consistent: production data is not anonymised; copies outside production are masked |
| Data residency and access | - | - | Hosted in AWS ap-southeast-1; access restricted to personnel in the Philippines | Nothing in the umbrella contradicts it |

## 12. Questions answered by this BRD

| Q# | Question | Answer from the umbrella | Status |
|---|---|---|---|
| XQ08 (and OQ44, CQ25, AQ27, PQ18) | One BIBS-wide NFR set | Retention 5 / 15, backup every 4 hours kept 5 years, response under 5 s for every role, users per role (pp.42-45); availability still "Refer to BRD" | partial |
| Q39 | Retention per record type | 5 years online and 15 years offline for application, database and audit logs and historical data (p.45) | partial |
| Q40 | Dynamic / customised reports | Customised (ad hoc) reports with fields, charts and summaries; scheduled or on demand (BR-053, BR-054, capability 21) | partial: requires more than saved variants (CRQ11) |
| PQ19 | Non-package products | "Non-Package Management" (p.7) is the non-package placement of New Business (PRF, QS, proposal slip), not product maintenance | partial (CRQ02) |
| Q44 | Reinsurance and insurer-side modules | Reinsurance is a BDOI business line (goal 1, capability 15); the ReInsurance BRD is phase 2 | answered for phase 1 |

## 13. New open questions (Core Replacement)

| ID | Topic | Question | Rows |
|---|---|---|---|
| CRQ01 | BRD set and ownership | The umbrella lists ten BRDs (p.3) and cites a "User & Data Management" BRD (pp.38-41). Is it the umbrella over all twelve BRDs, the Data Migration BRD and the ReInsurance BRD? Who owns Data Management (capability 17)? | CF-07, CORE-17 |
| CRQ02 | Non-Package Management | Capability 4 (added 20-Nov-2025) has no BR ID and no traceability row. Please confirm it is the non-package placement of New Business (PRF, quotation slip, comparative table, proposal slip) and not the maintenance of non-package products (PQ19). | CORE-04 |
| CRQ03 | Claims cheques | BR-146 to BR-148 ask for safekeeping of unclaimed cheques and hand-over to / retrieval from Cashiering. BRD-7 has no claim money through BDOI (CLQ10). Does BDOI hold settlement cheques? If yes, which unit keeps them, and what is recorded at hand-over and release? | CORE-14.08-14.10 |
| CRQ04 | CSF case management | BR-165 asks for case resolution; BRD-9 e-mail item 10 keeps case logging in SharePoint. Which applies in phase 1? | CORE-16.06 |
| CRQ05 | Delete of prospect | Is deactivation with a reason, followed by the retention purge, acceptable for "delete of prospect / record" (BR-008), or must a prospect be erased on request (for example under the Data Privacy Act)? | CORE-01.04 |
| CRQ06 | Role dashboards | Which figures does each role see on its home page (BR-125, 152, 160 say "to be defined")? Is one landing page per role expected, or the existing section homes? | XC-02 |
| CRQ07 | Master data change log | Which master data is in scope (users, products, insurers, LOVs, others)? Must the log show old and new values per field and the approver? Who views it? | CORE-17.01 |
| CRQ08 | Insurer management | What are "risk participation", "payment terms" and "remittance schedules" on the insurer record (BR-171)? Which unit maintains insurer records, and do changes need approval? | CORE-17.02 |
| CRQ09 | MIS field definition | Is BR-173 a fixed list of MIS fields per entity (as in its example) or a facility for users to add fields? Who maintains it? | CORE-17.04 |
| CRQ10 | Emerging capabilities | Capability 18 and BR-176 ("any other / additional system capabilities") cannot be tested. Please list the capabilities or withdraw the row. | CORE-18.01 |
| CRQ11 | Report customisation and scheduling | Which reports need user-chosen fields, charts and summaries? Who may schedule a report, how often, in which formats, and how is it delivered (e-mail, folder)? Is Word needed for all reports (EBQ21)? | CORE-21.04, 21.05 |
| CRQ12 | SOA at booking | Which SOA is generated with the placement report and Insurance Advice (BR-039) and with the insurer's service invoice at booking (BR-043): the client billing SOA or an insurer SOA? | XC-10 |
| CRQ13 | Invoice Master List | Is the Invoice Master List (BR-175) the Operations invoice ledger? Please confirm the columns (cover number, version number, assignees, placement status, approval dates) and the owner. | XC-13 |
| CRQ14 | Letter names | The umbrella writes "Non-Renewal Letter (NRL)", BRD-6 "Not for Renewal Letter (NFR)". Which name is printed on the letter? | CORE-06.07 |
| CRQ15 | Bank account operations | What does "Bank Account Operations" (p.9) cover beyond the account funding and bank instruments of BRD-5? | CORE-08.21 |
| CRQ16 | CSF resend | Must CSF agents also resend e-policies (BR-164), or only RAs (BRCSF-006)? | CORE-16.04 |
| CRQ17 | BDO design assets | Please supply the BDO icon and illustration set and the design system or Figma files (BR-179, 180, 183). | XC-14 |
| CRQ18 | Insurer invoice delivery | For "Invoice Batch Printing" (p.43): what is the batch file (one PDF per insurer, a ZIP, a manifest), the SFTP endpoint per insurer, and does it replace the e-mail of each service invoice? | XC-19 |
| CRQ19 | Client migration and feeds | "One-time migration from Broker and source, 2020 to present" and the daily midday and EOD client batches (p.43): which source system, and for how long do the daily batches run? How does this fit the selective migration of the Data Migration BRD? | XC-20, CF-11 |
| CRQ20 | ALeA | What is ALeA, and what is "e-mail address encoding" (p.43)? | XC-21 |
| CRQ21 | Response time and sizing | Response under 5 seconds for every role (p.42) differs from nine BRDs; 429 concurrent users is the sum of the BRD rows. Which values govern the performance tests? | CF-03, CF-04 |
| CRQ22 | Retention and backup | Do the umbrella values (5 / 15 years, backup every 4 hours kept 5 years) replace the BRD-specific ones (BRD-7 10 years online; BRD-5, 10, 12 five years archive; BRD-9 backup every 15 minutes; BRD-2 and BRD-8 backup kept 7 years)? | CF-05 |
| CRQ23 | Traceability workbook | Please send "E2E BDOI Mapping.xlsx" (p.30) and confirm that the final IDs of each BRD (BRPM, BRRN, BRCLXN, BRCLM, BRCSF) govern where the umbrella cites earlier ones. | CF-08, §10 |
| CRQ24 | User counts | Employee Benefits 13 / 13 (umbrella) or 20 internal users (BRD-8)? Claims 67 / 26 or 47 / 25 (BRD-7)? Product Maintenance 13 / 13 or TSU 13 plus MBS 5? | CF-04 |
| CRQ25 | Approval of the umbrella | The Product Owner for FRBS and ACSL is marked "On Mandatory Leave" without a signature; the Analytics and Risk Management head marks "sign-off not applicable"; four approvals are by e-mail only (pp.46-48). Is the document approved? | §14 |

## 14. Observations on the BRD pack

| # | Observation | Page |
|---|---|---|
| O1 | Every revision row is "version 1.0" although the content changed seven times; the file name says v01 | p.2 |
| O2 | The BRD list names ten BRDs; Sanction Screening, User Access Maintenance, Submitted Policies and Data Migration are missing, and the links are not active in the PDF | p.3 |
| O3 | BR-196, BR-197 and BR-198 repeat BR-184, BR-185 and BR-186; BR-187 to BR-198 have their IDs on the line below the text | p.23-24 |
| O4 | BR-004 has no priority level; every other row is "Must have" and no row has a process step or acceptance criteria | p.13 |
| O5 | BR-176 and capability 18 ("include any other / additional system capabilities") are not testable | p.11, p.23 |
| O6 | BR-178 to BR-183 are vendor-qualification questions from the RFI (brand, icons, design system, journeys, Figma), not system requirements | p.23 |
| O7 | The capability matrix (pp.26-29) differs from the list: it adds "Client Confirmation" and "Maintenance" (account), "Check Renewal Payment", "Receive and Validate File from Stakeholders", "Notification to Stakeholders", "Log Audit, History for Traceability" and "Net Settlement" (Reinsurance), and names the Data Management column "User & Data Management" with "User access and record" | p.26-28 |
| O8 | Capability 20 (Product Maintenance) restates benefits ("Easier quotation/proposal management", "Works with existing systems") rather than functions | p.12 |
| O9 | (BIBS documents, found during this analysis) The FRS of BRD-4 Collections and BRD-7 Claims both numbered their requirements FR-CL-nnn; 28 IDs existed in both documents. Claims now uses FR-CM-nnn (DCR-188) | FRS sources |
| O10 | Availability, demand and peak questions are answered "Refer to BRD"; the only new NFR values are users, response time and retention | p.44-45 |
| O11 | Approval: one approver on leave without signature, one "sign-off not applicable", four by e-mail (CRQ25) | p.46-48 |
