# BDOI Cross-BRD Decisions (BRD-6 to BRD-12)

Status: **binding for the builds of BRD-6 to BRD-12**. Seven BRDs were analysed in parallel:

| BRD | Area | Spec | Design |
|---|---|---|---|
| BRD-6 | Renewal (RN) | [`BDOI_RN_BRD_SPEC.md`](BDOI_RN_BRD_SPEC.md) | [`RENEWAL_DESIGN.md`](../architecture/RENEWAL_DESIGN.md) |
| BRD-7 | Claims (CLM) | [`BDOI_CLM_BRD_SPEC.md`](BDOI_CLM_BRD_SPEC.md) | [`CLAIMS_BROKING_DESIGN.md`](../architecture/CLAIMS_BROKING_DESIGN.md) |
| BRD-8 | Employee Benefits (EB) | [`BDOI_EB_BRD_SPEC.md`](BDOI_EB_BRD_SPEC.md) | [`EMPLOYEE_BENEFITS_DESIGN.md`](../architecture/EMPLOYEE_BENEFITS_DESIGN.md) |
| BRD-9 | Customer Servicing Facility (CSF) | [`BDOI_CSF_BRD_SPEC.md`](BDOI_CSF_BRD_SPEC.md) | [`CUSTOMER_SERVICING_DESIGN.md`](../architecture/CUSTOMER_SERVICING_DESIGN.md) |
| BRD-10 | Sanction Screening and Risk Profiling (SANC) | [`BDOI_SANC_BRD_SPEC.md`](BDOI_SANC_BRD_SPEC.md) | [`SANCTION_SCREENING_DESIGN.md`](../architecture/SANCTION_SCREENING_DESIGN.md) |
| BRD-11 | User Access Maintenance (UAM) | [`BDOI_UAM_BRD_SPEC.md`](BDOI_UAM_BRD_SPEC.md) | [`USER_ACCESS_DESIGN.md`](../architecture/USER_ACCESS_DESIGN.md) |
| BRD-12 | Submitted Policies (SP) and the BDOI Report List | [`BDOI_SP_BRD_SPEC.md`](BDOI_SP_BRD_SPEC.md), [`BDOI_REPORT_LIST.md`](BDOI_REPORT_LIST.md) | [`SUBMITTED_POLICIES_DESIGN.md`](../architecture/SUBMITTED_POLICIES_DESIGN.md) |

Two later BRDs were analysed afterwards and are consolidated here as well (decision D8, the BRD-00 and BRD-13 tables
of section 3, the ports and columns of section 4, and XQ12-XQ13):

| BRD | Area | Spec | Design |
|---|---|---|---|
| BRD-00 | BDOI Core Replacement (umbrella over the BRDs; 21 key capabilities) | [`BDOI_CORE_BRD_SPEC.md`](BDOI_CORE_BRD_SPEC.md) | [`CORE_REPLACEMENT_IMPACT.md`](../architecture/CORE_REPLACEMENT_IMPACT.md) |
| BRD-13 | Data Migration (draft v0.01, unsigned) | [`BDOI_DM_BRD_SPEC.md`](BDOI_DM_BRD_SPEC.md) | [`DATA_MIGRATION_DESIGN.md`](../architecture/DATA_MIGRATION_DESIGN.md) |

This document holds:
1. the decisions that settle the conflicts between those designs (section 1);
2. the changes for documents that are being edited by build agents and are therefore not edited here (section 2);
3. one table of every earlier question that BRD-6 to BRD-12 answer (section 3);
4. the overlaps removed from the designs (section 4);
5. the cross-BRD conflicts that remain, as questions (section 5);
6. the prerequisite work items that more than one build needs, and the recommended build order (section 6).
7. a glossary of names that mean different things in BDOI's programme documents and in the specs (section 7).

**Question IDs.** Submitted Policies (SQ01-SQ25) and Sanction Screening (SQ01-SQ20) use the same prefix. In this
document they are written **SP SQnn** and **SANC SQnn** (see XQ07). Other prefixes are unique: Q / BRNB (BRD-1),
OQ (BRD-2), PQ (BRD-3), CQ (BRD-4), AQ (BRD-5), RQ (BRD-6), CLQ (BRD-7), EBQ (BRD-8), CSQ (BRD-9), UQ (BRD-11),
UX (UX guidelines), XQ (this document).

## 1. Decisions (binding)

| # | Decision | Recorded in |
|---|---|---|
| **D1** | **Account business type (NEW_BUSINESS / RENEWAL) is one shared change** in the account module's own range, `V822__account_business_type.sql`: the prerequisite work item **BT0** (section 6.1). Renewal, Submitted Policies and Employee Benefits refer to it and none creates it separately. EB's business-type column in V1031 and its "required `businessType` on every `NewAccount`" are dropped. BT0 is owned by the first of the three builds to start | RENEWAL_DESIGN §2.3, §13, §14; SUBMITTED_POLICIES_DESIGN §2, §9, §13, §14; EMPLOYEE_BENEFITS_DESIGN §2, §3, §11, §13; DEVELOPER_GUIDE §4 |
| **D2** | **The Renewal module owns `RenewalHandOff`**, i.e. the renewal of submitted policies from the hand-off on: the renewal candidate, the renewal account, the hold cover request and the RA / NRNS / NAL / SFU (and renewal reminder) letters. Submitted Policies calls the port and keeps intake, classification, buckets, the expiry scan, the insurer rules, the hold-cover watch and re-assignment, and the masterlist status. SP SQ10 is closed | RENEWAL_DESIGN §2.3; SUBMITTED_POLICIES_DESIGN §1, §2.2, §3.5, §4.4, §8, §9, §14-16 |
| **D3** | **Employee Benefits in renewal.** EB programmes are excluded from the general renewal candidate lists (`RNW_EXCLUDED_LINES` = the EB benefit lines). Every renewal advice (Renewal, EB, Submitted Policies) is stored as an attachment of document type **`RENEWAL_ADVICE`**, linked to the account and the client. Claims reports are stored as document type **`CLAIM_REPORT`** (linked to the claim, the account and the client) | RENEWAL_DESIGN §2.3; EMPLOYEE_BENEFITS_DESIGN §8.1, §11; SUBMITTED_POLICIES_DESIGN §3.5; CLAIMS_BROKING_DESIGN §3.1, §4, §12.2; CUSTOMER_SERVICING_DESIGN §11 |
| **D4** | **Claims and Renewal.** Renewal reads loss experience only through `brokerclaims.service.ClaimExperienceQueryService` (Claims design, built in CL1-B). The claims part of Renewal's Account History tab (and the CLAIMS check) waits for the Claims build. The total-loss indicator stays open (CLQ28) | RENEWAL_DESIGN §2.3; CLAIMS_BROKING_DESIGN §3.1, §12.2 |
| **D5** | **CQ23 (answered by BRD-11).** The lockout after 3 failed attempts applies to all users (`LOGIN_MAX_FAILED_ATTEMPTS` = 3; portal users `PORTAL_MAX_FAILED_LOGINS` = 3). Users may hold several roles, so **no single-role check is built** anywhere. Single session per device is still open (UQ09) | USER_ACCESS_DESIGN §3, §8, §9; section 2 below (Collections) |
| **D6** | **Q42 (answered by BRD-11).** Sign-in by directory authentication (BDO EUA / Windows ID, LDAP / AD / SSO) is required. It is built as a parked port (`security.service.DirectoryAuthenticator`, `AUTH_MODE` LOCAL / DIRECTORY); local sign-in stays until BDO supplies the interface (UQ04) | USER_ACCESS_DESIGN §1, §10; RENEWAL_DESIGN §10; CLAIMS_BROKING_DESIGN §13; CUSTOMER_SERVICING_DESIGN §12; BROKING_ARCHITECTURE §6; BDO_UX_GUIDELINES §5, §6 (UX-4) |
| **D7** | **Portal users (EB)** are provisioned through User Access Maintenance requests, as an **external user type** (`nba_access_request.user_type` = EXTERNAL, with party kind, party code and portal role). EB's own `ptl_user_request` is dropped. On approval `nbadmin` calls the port `ExternalUserProvisioner`, implemented by `portal`. The User Access design does not contradict this: it already makes an approved request the only way to change access | USER_ACCESS_DESIGN §2, §3, §4.2, §4.4, §9, §11.2, §13; EMPLOYEE_BENEFITS_DESIGN §3, §4.1, §6.1, §6.3, §10.1, §11, §13 |
| **D8** | **Legacy invoices post by ledger context (BRD-13).** A legacy invoice is an `ops_invoice` row with origin LEGACY and `ledger_context` LEGACY, created by `opsledger.service.LegacyInvoiceIntake` (not by the booking feed). **Every posting module emits `LG_`-prefixed amount components when the invoice (or the UPP item) is in the LEGACY context**, through the shared helper `opsledger.domain.LedgerContext.component(base)`: cashiering (`CashieringPosting`, `ApplicationService`, `DispositionExecutor`, `CwtPostings`, `MinimalBalanceService`), remittance (`RemittancePostings`), commission (`DpPostings`), adjustment (`LedgerEffects`) and booking (`BookingEvents`, for endorsement invoices whose root is a legacy invoice). No new accounting event is created for legacy flows: Comptrollership adds the `LG_` lines to the existing rules, which route them to the legacy control accounts. `LedgerContext` and the `LG_` names are committed first by wave DM1-A | DATA_MIGRATION_DESIGN §14.2, §14.4, §14.5; OPERATIONS_DESIGN §4.1, §5; ACCOUNTING_DISBURSEMENT_DESIGN (GL-SL ledger context) |
| **D9** | **Documents and files are stored in S3** (option C of `docs/architecture/DOCUMENT_STORAGE_DECISION.md`, approved by BDOI 26-Sep-2026): file content in S3 with SSE-KMS using BDOI-owned keys; metadata in PostgreSQL (`stored_file`); downloads through 5-minute presigned links issued after the BIBS permission check and audit; GuardDuty malware scanning of uploads; Object Lock in governance mode with legal hold per the BDOI DOA; final issued records are archived to ECM. Every module writes through the `FileStore` port; no new `bytea` columns | DOCUMENT_STORAGE_DECISION.md; DATA_MIGRATION_DESIGN (staging bucket, 5-day expiry); build steps ST0 / ST1 |

Design choices made while applying the decisions (they follow from a decision; reject them if the decision owner
disagrees):
- **D2, default adapter.** Before Renewal wave R3 is deployed, the default `SubmittedRenewalHandOff` only records
  the hand-off as PENDING (no account, hold cover or letter); R3 replays the PENDING hand-offs, idempotent on the SBM
  number. The earlier text let the default adapter create accounts and letters, which was a second renewal path.
- **D2, outcome back to the masterlist.** `submitted` learns the renewal outcome from `AccountStatusChanged` and
  `InvoiceBooked` (as designed) and, for a renewal that never books (not renewed, lost, expired), from a status
  query on its own port `RenewalHandOff`, which the Renewal adapter answers from the candidate's closure. `submitted`
  never depends on `renewal`.
- **D2, letters.** `sbm_letter` keeps only letters that are not renewal letters (the policy-review REMINDER, and
  RENEWAL_NOTICE / RENEWAL_PROPOSAL until XQ03 is answered). The SBM RA / NRNS / NAL / SFU templates and the parameter
  `SBM_RA_DAYS_BEFORE_EXPIRY` move to the Renewal module (templates `RNW_*`, hand-off parameters in V1017).
- **D3, access classes.** `RENEWAL_ADVICE` and `CLAIM_REPORT`, and their access-class rows, are seeded by EB V1031
  with `on conflict do nothing`. V1010 (Renewal) and V1020 (Claims) seed the document types themselves the same way,
  because V1020 runs before V1031 on a fresh database.
- **D7, dormant (26-Sep-2026).** The BDOI drop plan places "Employee Benefits (no portal feature)" in Drop 2 (DCR-211, IQ22): no `portal` module is built, so there are no external users; the EXTERNAL request type stays refused by the built default adapter. The EB design, FRS and test plan are updated by the EB build team (`docs/deliverables/PENDING_EDITS.md`).
- **D7, permissions.** EB's `PORTAL_USER_REQUEST` and `PORTAL_USER_APPROVE` become the type-specific permissions of
  EXTERNAL requests in the UAM flow; the UAM segregation rules apply. Lock / unlock and the portal logs stay in
  `portal` (`PORTAL_ADMIN`).

## 2. Changes for documents not edited here

Build agents are working on these documents, so they are not changed by this consolidation. Their owners apply the
items below when they next edit them.

| Document | Item | Source |
|---|---|---|
| `COLLECTIONS_DESIGN.md` §6.2 ("The single-role NFR is CQ23") | **CQ23 answered (D5):** drop the single-role policy check from the Collections plan; several roles per user are allowed. Single session per device stays open (UQ09) | BRD-11 (p.13-14, NFR 1.c-d) |
| `COLLECTIONS_DESIGN.md` (security notes, `LOGIN_MAX_FAILED_ATTEMPTS`) | The lockout at 3 failed attempts applies to all BIBS users (the "CQ23 decides" note is settled); UAM V1060 updates the parameter description | BRD-11 |
| `COLLECTIONS_DESIGN.md` (UPP dispositions) | LOV value `HANDLING_FEE` in `CLX_UPP_DISPOSITION` (requires_invoice = false); the unapplied list shows "Handling fee (auto)" when the ticket source is SUBMITTED | SUBMITTED_POLICIES_DESIGN §9 |
| `BDOI_CLXN_BRD_SPEC.md` | CQ23: answered (D5), except single session per device (UQ09). CQ06: partial, CSF agents see all accounts, CBG and non-CBG (BRD-9 p.4); other roles open. CQ09: partial, "Reports per Unit per Branch" is daily / as needed with production-style columns (Report List #59). CQ25: more NFR variants, not resolved (OQ44) | BRD-9, BRD-11, BRD-12 |
| `BDOI_ACCT_BRD_SPEC.md` | AQ05: partial, fields of the FRBS reports are listed in the Report List #1-31 ("summaries are derived values"). AQ21: partial, for EB the direct-billed indicator comes from the AO's upload of the insurer's direct billing (BRID-025). AQ27: more NFR variants, not resolved. AQ28: partial, role matrices per new area (each design section 6 / 7) | BRD-8, BRD-12 |
| `ACCOUNTING_DISBURSEMENT_DESIGN.md` | FRBS service-fee pack: include handling-fee income (4115) and No Touch service-fee income (4110, SI type `SERVICE_FEE_NO_TOUCH`), no contract change. ACSL "account status Renewal" reads the business type from `InvoiceBooked` (BT0) or from `ops_invoice.business_type` once the Operations owner agrees it. A claimant payee class is needed only if CLQ10 is answered yes | SUBMITTED_POLICIES_DESIGN §9; RENEWAL_DESIGN §5, §13; CLAIMS_BROKING_DESIGN §3.1 |
| `BDOI_REPORT_LIST.md` (mapping rows) | Refresh when the Report List is next edited: #76, #78, #79 are designed by Renewal (`RNW-EXPIRY-LIST`, `RNW-STATUS`, `RNW-RA-DISPATCH`); #171 by EB (`EB-RENEWAL`); #135 general persistency has no report yet (XQ06) | RENEWAL_DESIGN §11; EMPLOYEE_BENEFITS_DESIGN §9 |

## 3. Questions answered by BRD-6 to BRD-12

One row per earlier question. **Status**: *answered* (closed) or *partial* (the rest of the question stays open).
Questions examined and still open are listed after the table. The same answers are recorded, without the design
column, in the original specs: `BDOI_NB_BRD_SPEC.md` §9.1, `BDOI_NB_TRACEABILITY.md` ("Answered by later BRDs"),
`BDOI_OPS_BRD_SPEC.md` §10.1 and `BDOI_PM_BRD_SPEC.md` §7.1; the CQ and AQ items are in section 2.

| Q# | Original BRD | Answering BRD and ID | Answer (short) | Status | Design impact |
|---|---|---|---|---|---|
| Q06 | BRD-1 | BRD-8 EB, BRID-005 / 005.01-005.02 / 009 | For EB, insurers use a secure portal or API | partial | `portal` module; `cat_insurer.placement_channel` unchanged for now |
| Q07 | BRD-1 | BRD-8 EB, BRID-007 | Standard assigned password or system-generated with a defined syntax, sent separately; convention still missing (EBQ09) | partial | Existing `DocumentPasswordPolicy`; DOCX protection (EB E0) |
| Q08 | BRD-1 | BRD-9 CSF, p.3 footnote 3 | QPS and EBIX must receive client contact updates during coexistence | partial | `csf` port `ContactSyncGateway` with outbox (CSQ01) |
| Q10 | BRD-1 | BRD-6 RN, personas / tabs / 34 counters / BRRN.040 | Renewal stages and statuses; counter mapping to confirm (RQ10) | partial | Workflow `RNW_CASE` |
| Q11 | BRD-1 | BRD-12 SP, p.4-5, Report List #138 / #151 | Sources named (LFS, HLS, CIU, SPI, LAMD, Loan Booking Report, IA masterlist), all Excel; layouts and transport open (SP SQ01) | partial | One `submitted` bulk handler per source; `SubmittedSourceFeed` port |
| Q14 | BRD-1 | BRD-6 RN, BRRN.020; BRD-12 SP, Report List #151 | Renewal sanitation is rule-based, criteria not listed (RQ01); submitted-policy criteria listed (PN vs LAMD, exclusions, loan status, duplicates, PN checks) | partial | `RenewalCheckEngine`; `sbm_rule` seeds; NB sanitiser unchanged |
| Q15 | BRD-1 | BRD-6 RN, BRRN.022 | Mandatory unique renewal reference as the matching key; no format | partial | `RNW-<yyyy>-nnnnnn` (`RNW_REFERENCE_PREFIX`) |
| Q18 | BRD-1 | BRD-10 SANC, SNSRP-302/303 | `PEP` / high-risk tags are set by rules and trigger reviews, not blocks | partial | `crm.service.ClientRiskService`; block only behind a parameter (SANC SQ07) |
| Q21 | BRD-1 | BRD-6 RN, BRRN.028; BRD-10 SANC, SNSRP-102/303 | KYC due is visibility only in renewal; risk categories defined in the system; high-risk / PEP clients with an active policy get a KYC review / EDD case; frequency not given | partial | Renewal check `KYC_DUE` (INFO); screening cases |
| Q23 | BRD-1 | BRD-8 EB, BRID-002 / 005.01 / 014; BRD-9 CSF, BRCSF-007; BRD-10 SANC, SNSRP-601 | File types listed (EB, CSF); naming syntax for screening documents only | partial | `AllowedFileType` + 7 types (CSF S0); `DocumentNamingService` named pattern (SANC S0) |
| Q24 | BRD-1 | BRD-12 SP, BRIDSP-02 | Submitted policy documents are extracted and confirmed by a user; scanned input implies OCR | partial | `PolicyDataExtractor` kind SUBMITTED_POLICY (V861); `OcrEngine` port |
| Q27 | BRD-1 | BRD-6 RN, 1.009.3.1.33-34; BRD-12 SP, p.5 / BRIDSP-24/32 | Hold covers apply to renewals; 30-day hold cover for submitted renewals, accepted within 3-5 days else follow up / re-assign; unbooked hold covers alerted; expiry handling open | partial | `HoldCoverService.reassign` (V851); `SBM_HOLD_COVER_WATCH` |
| Q28 | BRD-1 | BRD-12 SP, Report List #65-67 | Three CLPC billing variants with sample files | partial | Variants of `NB-CLPC-BILLING` |
| Q31 | BRD-1 | BRD-8 EB, BRID-005.01 | EB insurers upload policy forms and billing through the portal | partial | Portal tasks feed `EpolicyService.receive` |
| Q36 | BRD-1 | BRD-12 SP, BRIDSP-13/14 | FFY-specific RA template at renewal; FFY bucket conflicting (SP SQ05); payer not stated | partial | RA template GENERIC / FFY travels in the hand-off; `RNW_RA_FFY` |
| Q37 | BRD-1 | BRD-7 CLM, BRCLM.007/039; BRD-9 CSF, p.3 / BRCSF-002 | Policy number flows from the policy system, never re-keyed; QPS / EBIX are systems of record for contacts during coexistence | partial | Claims keeps a cover snapshot; CSF outbox |
| Q39 | BRD-1 | BRD-7 CLM, p.41; BRD-8 EB, NFR; BRD-9 CSF, NFR; BRD-10 SANC, p.25 | Claims 10 / 15 years; EB 5 / 15; CSF 5 / 15; screening 5 / 5; BRD-11 "follow QPS" (UQ12) | partial | Retention rules per record type in each design |
| Q40 | BRD-1 | BRD-8 EB, BRID-022 AC9; BRD-12 SP, Report List #74 | Customisable filters and exports; no defined template | partial | Saved variants and column filters; no query builder |
| Q42 | BRD-1 | BRD-11 UAM, p.13-14, 17 (UAM-NFR-11, 17, 33) | Directory sign-in required (EUA / Windows ID, LDAP / AD / SSO); inactivity log-out at 30 minutes with a 15-minute warning | answered | D6: `DirectoryAuthenticator` port, LOCAL default, EUA adapter parked (UQ04) |
| Q44 | BRD-1 | BRD-7 CLM, BRCLM.023/024 | Insurer reserve and settlement are information only; BDOI does no reserving | answered (claims) | `brokerclaims` posts no journal; insurer-side `claims` hidden from BDOI roles |
| OOS-1 | BRD-1 | BRD-6 RN, 1.011.1, BRRN.023/031/035 | Overrides: TL override of outstanding balance; authorised override of disposition / bucket with remarks; no approval request | answered | `RNW_OVERRIDE`, `rnw_override` |
| BRNB.097 | BRD-1 | BRD-6 RN, BRRN.033; BRD-8 EB, BRID-022.01; BRD-12 SP, BRIDSP-26/27 | NB / Renewal classification required at creation and a filter on reports; renewal with changes takes the NB path but stays a renewal | answered | D1: work item BT0 (V822) |
| BRNB.085 | BRD-1 | BRD-11 UAM, sections A-B | Requestor persona, drafts, return, cancel, chosen approver, bulk | answered (extended) | `nbadmin` request lifecycle (V1062) |
| OQ01 | BRD-2 | BRD-7 CLM, p.23; BRD-8 EB, BRID-021 | Claims is a BIBS module; EB billing / payment tracking is done by Collection inside BIBS; the other systems open | partial | No external Claims interface; `ClaimsFeed` in-app |
| OQ12 | BRD-2 | BRD-12 SP, BRIDSP-31; BRD-6 RN, BRRN.029/039 | Handling fee: PN for CLPC, Location Reference for OTC; PN is the LAMD matching key | partial | Handling-fee tagger; general pre-booked key open |
| OQ15 | BRD-2 | BRD-12 SP, Report List #105, BRIDSP-31 | Seven unapplied dispositions plus Handling Fee; approvals open | partial | Disposition type `HANDLING_FEE` (cashiering), `CLX_UPP_DISPOSITION` (collections) |
| OQ17 | BRD-2 | BRD-12 SP, p.6; BRD-7 CLM, NFR 15.09-15.13 | Target is a single master database in place of shared-drive masterlists; Claims still names a shared drive for report files (CLQ21) | partial | In-system repository; `FileDropPort` unchanged |
| OQ25 | BRD-2 | BRD-7 CLM, BRCLM.010 | Claims condition: invoice of the cover of an open claim in "With BDOI - For Premium Remittance"; approvers / SLA open | partial | `InAppClaimsFeed` feed `CLAIMS_SPECIAL_REMIT` |
| OQ29 | BRD-2 | BRD-8 EB, BRID-005 | A secure insurer portal will exist | partial | `portal` reusable by Prod Recon / Remittance later |
| OQ32 | BRD-2 | BRD-6 RN, BRRN.032 | Endorsements must be on the mother policy before renewal approval; ownership open | partial | Renewal check `ENDORSEMENT_PENDING` reads `adjustment` |
| OQ38 | BRD-2 | BRD-8 EB, BRID-025 | EB direct billing uploaded by the AO; direct-payment arrangement (EBQ17) | partial | `eb_member_change.direct_billed` |
| OQ39 | BRD-2 | BRD-12 SP, Report List #164 | No Touch = submitted CBG Motor accounts billed a service fee (gross + 12% VAT - 15% WTax); targets open | partial | `SBM-NO-TOUCH`, SI type `SERVICE_FEE_NO_TOUCH` |
| OQ42 | BRD-2 | BRD-12 SP, Report List #85-96, #103 | Key fields of Cashiering 9-18, 20-21 and Remittance 8 listed | partial | Cashiering / remittance owners align columns |
| OQ45 | BRD-2 | BRD-12 SP, BRIDSP-31 | Handling-fee disposition of UPP items is a Marketing activity inside BIBS | partial | Collections unapplied view |
| OQ46 | BRD-2 | BRD-7 CLM, BRCLM.001/010; BRD-6 RN, BRRN.027/031/034 | Claims blocks on unpaid premium and implements the special-remittance feed; no claim money through BDOI (CLQ10 open); Renewal needs claims per expiring policy | partial | `ClaimsFeed` adapter; `ClaimExperienceQueryService` (D4) |
| OQ48 | BRD-2 | BRD-6 to BRD-12 (personas and matrices per area) | Per-area personas and function maps; the BIBS-wide matrix is still a BDOI deliverable | partial | Roles in each design |
| PQ10 | BRD-3 | BRD-6 RN, BRRN.038 AC 3 | Acceptance evidence required when a renewal has a financial impact | partial | `rnw_acceptance` evidence mandatory |
| PQ11 | BRD-3 | BRD-6 RN, BRRN.030/033/035/038 | Renew As Is keeps the expiring terms and version; repricing takes the NB path; EXPIRED version open (RQ30) | partial | BT0 overload `NewAccount.renewal(..., productVersionNo)`; `AccountPricing` purpose RENEWAL |
| PQ17 | BRD-3 | BRD-11 UAM, p.6 | Group-profile changes are requested, approved, then implemented by the System Administrator | answered | FOR_IMPLEMENTATION step; `UAM_DIRECT_ROLE_EDIT` emergency path |
| PQ21 | BRD-3 | BRD-8 EB, BRID-007 | TOR, master list and utilization to insurers are password protected, password sent separately | partial | EB outbound documents always protected |
| CQ06 | BRD-4 | BRD-9 CSF, p.4 | CSF agents see all accounts (CBG and non-CBG) | partial | No segment scoping for CSF roles |
| CQ09 | BRD-4 | BRD-12 SP, Report List #59 | "Reports per Unit per Branch": daily / as needed, production-style columns | partial | Collections owner |
| CQ23 | BRD-4 | BRD-11 UAM, p.13-14 (NFR 1.c-d) | Lockout at 3 for all users; several roles per user allowed; single session per device open (UQ09) | answered (except UQ09) | D5: no single-role check; session log prepares UQ09 |
| AQ05 | BRD-5 | BRD-12 SP, Report List #1-31 | Fields of the FRBS reports listed | partial | FRBS owners align columns |
| AQ21 | BRD-5 | BRD-8 EB, BRID-025 | For EB, the direct-billed indicator comes from the AO's upload | partial | `eb_member_change.direct_billed` |
| AQ28 | BRD-5 | BRD-6 to BRD-12 | As OQ48 | partial | Roles in each design |
| UX-2 | UX guidelines | BRD-6 RN, BRRN.002/023/030, 1.005 | Expiry List, Generate Expiry List, Assign Disposition, Re-assign Officer, Clean / Review / Exception are in the BRD | answered | RENEWAL_DESIGN §12 |
| UX-3 | UX guidelines | BRD-8 EB; BRD-9 CSF | Employee Benefits and Customer Service Facility sections | answered | Sections in Client & Policy |
| UX-4 | UX guidelines | BRD-11 UAM (Q42) | Windows ID sign-in required | answered | D6 |
| SP SQ10 | BRD-12 | Decision D2 (Renewal design, from the RN BRD scope) | Renewal owns the renewal of submitted policies; the users who place and book submitted renewals are part of the role matrix (SP SQ15, OQ48) | answered | `renewal` implements `RenewalHandOff` (wave R3) |
| EBQ28 | BRD-8 | BRD-6 RN (scope has no group benefits); decision D3 | EB renewals stay in EB; general renewal lists exclude EB lines; one RA document type | answered (boundary) | `RNW_EXCLUDED_LINES`; `RENEWAL_ADVICE`. A shared RA template is not decided (EB and Renewal keep their own) |
| EBQ13 | BRD-8 | Decision D7 (BRD-11 request flow) | Who creates and approves portal users: UAM requests, external user type | partial | `nba_access_request` user type EXTERNAL; hosting, MFA and IdP still open |

**Answered by BRD-00 (Core Replacement umbrella, `BDOI_CORE_BRD_SPEC.md` §12).**

| Q# | Original BRD | Answering BRD and ID | Answer (short) | Status | Design impact |
|---|---|---|---|---|---|
| XQ08 (OQ44, CQ25, AQ27, PQ18) | Cross-BRD | BRD-00, usage tables p.42-45 | Retention 5 years online / 15 years offline; backup every 4 hours kept 5 years; response under 5 s for every role; users per role (1,344 named / 429 concurrent, the sum of the BRD rows). Availability, hours and maintenance still "Refer to BRD" | partial | BRD-00 column in the register NFR comparison; the performance plan tests 429 concurrent sessions as the peak case until CRQ21 / CRQ24 are answered |
| Q39 | BRD-1 | BRD-00 p.45 | 5 years online and 15 years offline for application, database and audit logs and historical data; BRD-7 (10 / 15) and the 5 / 5 BRDs remain exceptions (CRQ22) | partial | Default 5 / 15 in `nba_retention_rule`; exceptions per record type |
| Q40 | BRD-1 | BRD-00 BR-053, BR-054, capability 21 | Customised (ad hoc) reports with fields, charts and summaries, scheduled or on demand: more than saved variants (CRQ11) | partial | Report layout in variants and report subscriptions (Core wave CR-W2, V1093) |
| PQ19 | BRD-3 | BRD-00 capability 4 "Non-Package Management" (p.7) | Read as the non-package placement of New Business (PRF, quotation slip, proposal slip), not maintenance of non-package products (CRQ02) | partial | None until CRQ02 is answered |
| Q44 | BRD-1 | BRD-00 goal 1, capability 15 | Reinsurance is a BDOI business line; the ReInsurance BRD is phase 2 | answered for phase 1 | Insurer-side `reinsurance` stays hidden from BDOI roles in phase 1 |

**Answered by BRD-13 (Data Migration draft v0.01, `BDOI_DM_BRD_SPEC.md` §6).** The BRD is an unsigned draft; the
answers hold until it is signed.

| Q# | Original BRD | Answering BRD and ID | Answer (short) | Status | Design impact |
|---|---|---|---|---|---|
| CQ13 | BRD-4 | BRD-13 BRID 6.1, 6.2 | Legacy invoices keep being processed in BIBS after cutover, so both invoice-number formats are valid until the legacy invoices run off | answered | `CLX_INVOICE_NO_PATTERN` (V1000) stays; QPS numbers added when their format is known (DMQ11) |
| CQ07 | BRD-4 | BRD-13 p.3, BRID 5.1, 6.1-6.4, 11.1 | Outstanding receivables and UPP are carried forward and processed in BIBS; history stays read-only or archived. Open dispositions, promises and assignments are not mentioned (DMQ34) | partial | Legacy invoices become Collections items through the worklist refresh; `CLX_LEGACY_ITEMS` narrowed to open dispositions and promises (V1007) |
| CQ22 | BRD-4 | BRD-13 BRID 6.1 | Legacy invoices keep their legacy identifiers; the field mapping is not given | partial | `ops_invoice.legacy_invoice_no`, `legacy_ref`, `source_system` (V1086) |
| CLQ13 | BRD-7 | BRD-13 BRID 11.1 | History stays in legacy or the archive; open claims are not mentioned | partial | `bcl_claim.legacy_ref` only if open claims are migrated (DMQ30) |
| CLQ14 | BRD-7 | BRD-13 p.3, BRID 11.1 | Historical claims are not migrated (read-only legacy or archive); open claims are not addressed | partial | V1025 stays held; archive record type CLAIM |
| RQ27 | BRD-6 | BRD-13 BRID 4.1, 12.1, p.5 | Renewal-driven transition aligned to RMEL; in-force headers migrated when required; renewals recreate clean records in BIBS | partial | `migration` implements `LegacyPolicySource` from the migrated headers and RMEL cohorts; `RNW_LEGACY_POLICIES` stays as the fallback |
| CSQ01 | BRD-9 | BRD-13 p.5, BRID 2.1 | Legacy is read-only after cutover; the new Core is the system of record for the migrated client master; no write-back required by this BRD | partial | `ContactSyncGateway` stays NOT_CONFIGURED; `LegacyAccountLookup` implemented by `migration` |
| CSQ02 | BRD-9 | BRD-13 BRID 4.1, 11.1 | Legacy references are retained | partial | Search by legacy reference through `mig_key_xref` |
| CSQ06 | BRD-9 | BRD-13 BRID 11.1 | Historical documents stay in read-only legacy or the archive | partial | Archive record type RENEWAL_ADVICE, read through `LegacyAccountLookup` |
| Q08 | BRD-1 | BRD-13 p.5 | No feed to QPS / EBIX required after cutover (legacy read-only) | partial | None beyond CSQ01 |
| Q37 | BRD-1 | BRD-13 p.5 | From cutover the new Core holds the trusted client master and the governed reference data | partial | Client and reference masters owned by BIBS from go-live |
| Q39 | BRD-1 | BRD-13 p.14 (defers to the consolidated NFR) | No value of its own; see the BRD-00 row above | partial | Archive records follow `nba_retention_rule` |
| AQ01 | BRD-5 | BRD-13 BRID 5-8 | Legacy sub-ledgers for Premium Receivable, Commission Receivable, DTIP and UPP, so the chart needs legacy control accounts; the mapping is not given (DMQ18) | partial | Code map set GL_ACCOUNT; legacy control accounts and a migration clearing account (D8) |
| OQ44 / XQ08 | BRD-2, cross-BRD | BRD-13 p.13-14 | Refers to "the consolidated NFR requirements for BDO Insure Core Modernization project", which is not in the pack (DMQ29) | partial | Ask for the document |

Still open after BRD-13: **AQ11** (payee migration file; `DISB_PAYEE_MIGRATION` runs under the migration framework as
object R09) and **SP SQ16** (Excel masterlists; `SBM_MIGRATION` runs as object P04).

**Examined and still open:** Q09 (late renewal requests report; RQ29), Q16 (BDO CIF; restated by BRD-9 and BRD-10),
OQ44 / CQ25 / AQ27 / PQ18 (NFR alignment: every later BRD adds a variant), OQ02, OQ07 (no BRD-6 to BRD-12 content),
CLQ28 / RQ13 (total-loss indicator), UQ09 (single session per device), RQ05 (grid without pagination, now UX-6).

## 4. Overlaps removed

| # | Overlap | Designs before | Now |
|---|---|---|---|
| O1 | Account business type | SP: `V822` (account range); EB: `acc_account.business_type` in V1031 and a required `businessType` on every `NewAccount`; Renewal: referenced V822 but asked for EB to fold in | One change, BT0 (`V822`), with the NB report filter and `InvoiceBooked.businessType`. EB V1031 no longer touches `acc_account`; EB and SP no longer list the booking / nbreport business-type edits as their own. Flyway tables corrected: SP `account` V822, `booking` none (code only), `placement` V851, `issuance` V861; EB `account` V822 |
| O2 | Renewal of submitted policies | SP default adapter created the renewal account, priced it, requested the hold cover and sent RA / NRNS / NAL / SFU letters (`sbm_letter`, `SBM_RA_*` templates); Renewal's adapter did the same | Renewal owns it (D2). SP default adapter records PENDING only; `sbm_letter` limited to non-renewal letters; SBM RA / NRNS / NAL / SFU templates and `SBM_RA_DAYS_BEFORE_EXPIRY` removed; RA register of submitted renewals is `RNW-RA-DISPATCH` |
| O3 | Renewal advice document | Renewal, EB and CSF each described the RA convention | One document type `RENEWAL_ADVICE`, linked to the account and client (D3); seeded idempotently by V1010, V1030 / V1031 |
| O4 | Claims read by Renewal | The first Renewal draft declared its own port `PolicyClaimsSource` (fed through `ClaimsFeed`) next to the Claims read API | One read API, `ClaimExperienceQueryService` (D4; already applied in the Renewal design, confirmed in the Claims design) |
| O5 | Portal user requests | EB: `ptl_user_request` (AO requests, Business Administrator approves); UAM: `nba_access_request` for every access change | One request flow, UAM, with user type EXTERNAL (D7); `ptl_user_request` removed from V1032 |
| O6 | Directory sign-in | Renewal, Claims and CSF designs each parked "BDO SSO" on the existing login; UAM designed the port | One decision (D6): the UAM `DirectoryAuthenticator` port; the other designs point to it |
| O7 | Access-class rows of shared document types | Renewal V1010 and Claims V1020 would have seeded rows in a table that V1031 creates later on a fresh database | V1031 seeds the access rows of `RENEWAL_ADVICE` and `CLAIM_REPORT` (on conflict do nothing) |

Check after the edits:
- **Columns.** No two designs create the same column: `acc_account.business_type` / `renewal_of_ref` only in V822;
  `bkg_invoice.insurer_billing_no` only in EB V1031; `quo_quotation.renewal_ref` / `npk_proposal.renewal_ref` only in
  Renewal V1011; `ops_invoice.business_type` only by the Operations owner (requested by Renewal); `sec_user` and
  `sec_role` columns only in UAM V1061; `nba_access_request` columns only in UAM V1062; the legacy columns of
  `ops_invoice` (`origin`, `ledger_context`, `source_system`, `legacy_invoice_no`, `legacy_ref`, `migration_batch_no`)
  only in V1086 (Data Migration, on behalf of the `opsledger` owner); `bkg_invoice.ledger_context` only in V873;
  `acsl_glsl_control.ledger_context` only in V1087.
- **Ports.** Each port has one declaring module: `RenewalHandOff`, `MailHouseGateway`, `SubmittedSourceFeed`,
  `SignatureProvider` (`submitted`); `OcrEngine`, `PolicyDataExtractor` (`issuance`); `LegacyPolicySource`,
  `RecipientPolicy` (`renewal`); `ClaimsFeed` (`opsledger`, implemented by `brokerclaims`); `PortalUploadTarget`,
  `PortalTaskSource`, `PortalHomeCounts` (`portal`); `BorValidator` (`eb`); `ContactSyncGateway`,
  `LegacyAccountLookup` (`csf`); `WatchlistFeed`, `StrFileSink`, `ActivePolicyQuery` (`screening`),
  `ClientComplianceGate` (`crm`, only if SANC SQ07 says block); `DirectoryAuthenticator` (`security`);
  `ExternalUserProvisioner` (`nbadmin`, implemented by `portal`). After BRD-13: `LegacyInvoiceSource` (`booking`,
  implemented by `opsledger`); `LegacyPolicySource` (`renewal`) and `LegacyAccountLookup` (`csf`) implemented by
  `migration`. No module depends on `migration`.
- **Flyway.** Every design's table matches the Developer Guide ranges: Renewal V1010-V1017 / V1910-V1911, Claims
  V1020-V1024 / V1920-V1921, EB and portal V1030-V1036 / V1930-V1932, CSF V1040-V1042 / V1940, Screening
  V1050-V1055 / V1950-V1952, UAM V1060-V1062 / V1960, SP V1070-V1076 / V1970-V1972; owner-range changes V822 (BT0),
  V851, V861 (listed in the Developer Guide range table); Data Migration V1080-V1089 / V1980-V1989 with owner versions
  V766, V786, V803, V823, V873, V1007; Core Replacement items V1090-V1099 / V1990-V1999.

## 5. Remaining cross-BRD conflicts (questions)

| # | Conflict | Designs | Question | Proposal (not decided) |
|---|---|---|---|---|
| XQ01 | **Two LAMD intakes.** Renewal uploads LAMD paid-off / RMU reports (`RNW_LAMD_REPORT`, `rnw_lamd_report` / `rnw_lamd_line`, role `LAMD`); Submitted Policies uploads a LAMD loan snapshot (`SBM_LAMD`, `sbm_lamd_loan`). Both match on the PN | RENEWAL_DESIGN §4.4, §9; SUBMITTED_POLICIES_DESIGN §4.1, §8 | Is it one LAMD report (one file, one owner) or two different reports? Who uploads it? (RQ20, SP SQ06) | One intake owned by one module, read by the other through a query service |
| XQ02 | **Two Contact Center roles.** Renewal's `CONTACT_CENTER` (follow-ups, remarks, documents on candidates, BRRN.026) and CSF's `CSF_AGENT` / `CSF_SUPERVISOR` (servicing). Renewal restricts Contact Center to a read-only projection without premium columns; CSF agents see all accounts | RENEWAL_DESIGN §6; CUSTOMER_SERVICING_DESIGN §6 | Are they the same people (BDO Contact Center vs BDOI staff)? One role or two? (RQ21, CSQ10) | Keep both roles; one user may hold both (D5 allows several roles) |
| XQ03 | **Submitted-policy renewal notices.** `sbm_letter` types RENEWAL_NOTICE and RENEWAL_PROPOSAL are renewal-related, but D2 names only RA / NRNS / NAL / SFU | SUBMITTED_POLICIES_DESIGN §4.4 | Are the renewal notice and renewal proposal letters part of the renewal (Renewal module) or of the masterlist follow-up (Submitted Policies)? (SP SQ09) | Kept in `sbm_letter` until answered |
| XQ04 | **Access classes of shared documents.** The EB spec gives `RENEWAL_ADVICE` the classes MARKETING and PROCESSING; CSF agents must view and resend RAs and claims reports; claims documents may be confidential | EMPLOYEE_BENEFITS_DESIGN §3, §11; CUSTOMER_SERVICING_DESIGN §11, §14 | Final confidentiality matrix per document type, including CSF (CSQ07, EBQ15) | V1031 seeds CSF view rows for `RENEWAL_ADVICE` and `CLAIM_REPORT` |
| XQ05 | **What is a "claims report" (BRCSF-009).** Claims keeps its own document-type LOV (`BCL_DOCUMENT_TYPE`: PLA, CRF, estimate, offer, LOA, release papers ...); CSF expects `CLAIM_REPORT` | CLAIMS_BROKING_DESIGN §3.1, §9.3; CUSTOMER_SERVICING_DESIGN §11 | Which claim documents does the contact centre retrieve, and should claim documents use the platform `DOCUMENT_TYPE` list or `BCL_DOCUMENT_TYPE`? (CLQ26, CSQ07) | The loss advice and insurer claim reports are stored as `CLAIM_REPORT` |
| XQ06 | **Renewal persistency report.** Report List #135: SP builds `SBM-PERSISTENCY` for submitted accounts and says the general report belongs to Renewal; the Renewal design has no persistency report | SUBMITTED_POLICIES_DESIGN §11; RENEWAL_DESIGN §11 | Layout and definition of the general Renewal Persistency Report | A later `renewal` report over closed candidates |
| XQ07 | **Question-ID collision.** SP SQ01-SQ25 and SANC SQ01-SQ20 share the prefix | BDOI_SP_BRD_SPEC §10; BDOI_SANC_BRD_SPEC §10 | Rename one series (e.g. SANC to SNQ) or always cite with the BRD prefix? | Cite as "SP SQnn" / "SANC SQnn" until the spec owners agree |
| XQ08 | **NFR alignment.** Every later BRD adds an NFR set: CLM Mon-Fri 06:00-20:00, RPO 4 h; EB 99.99%, 08:30-19:00, RPO 24 h; CSF 06:00-22:00, backup every 15 minutes; SANC 08:00-18:00, 5 + 5 years; UAM and RN "follow QPS"; SP 2 s response, 5 + 5 years | all specs, NFR sections | One BIBS-wide NFR set (OQ44, CQ25, AQ27, PQ18, CLQ24, EBQ25, CSQ11, SP SQ21 / SQ22, RQ28) | Infrastructure decision; the strictest RPO (CSF 15 minutes) drives WAL archiving |
| XQ09 | **Late renewal requests report** (BRNB.018, Q09). Neither the RN BRD nor the Report List (#182 is a package report) defines it | RENEWAL_DESIGN §13; BROKING_ARCHITECTURE §16.6 | Is it still needed, and is it a Renewal listing variant? (RQ29) | Variant of `RNW-LISTING` |
| XQ10 | **Total-loss indicator** (CLQ28, RQ13) | RENEWAL_DESIGN §2.3, §10; CLAIMS_BROKING_DESIGN §12.2 | Should Claims record a total-loss indicator (settlement type, loss nature or flag)? | Manual non-renewal reason until answered (D4) |
| XQ11 | **Single session per device** (UQ09, rest of CQ23) | USER_ACCESS_DESIGN §4.1, §14 | Does it apply to all users? | Session log built; enforcement parked |
| XQ12 | **CSF case management** (Core CF-01). The umbrella asks for case resolution, adding and editing case details and status (capability 16, BR-165, p.22); BRD-9 puts case management out of scope, with SharePoint as the interim (CSF-EM10) | CUSTOMER_SERVICING_DESIGN; CORE_REPLACEMENT_IMPACT §5 | Is CSF case resolution in phase 1? (CRQ04) | Out of phase 1 until answered; if in, a CSF case entity in the CSF range (V1043-V1049) |
| XQ13 | **Claims cheque custody** (Core CF-02). The umbrella asks for safekeeping of unclaimed cheques and hand-over to and retrieval from Cashiering (BR-146 to BR-148, p.21); BRD-7 has no claim money through BDOI | CLAIMS_BROKING_DESIGN §3.1; CORE_REPLACEMENT_IMPACT §5 | Does BDOI hold settlement cheques, which unit keeps them, and what is recorded at hand-over and release? (CRQ03, CLQ10) | Parked; if yes, a cheque custody register in `brokerclaims` (V1026-V1029) using the cashiering cheque hand-over |

## 6. Prerequisite work items and build order

### 6.1 Work items that more than one build needs

| # | Work item | Needed by | Scope | Owner |
|---|---|---|---|---|
| **BT0** | **Account business type** (D1) | Renewal, Employee Benefits, Submitted Policies; also Operations (`ops_invoice` column) and ACSL | `V822__account_business_type.sql`: `acc_account.business_type` (required, NEW_BUSINESS / RENEWAL, default NEW_BUSINESS), `renewal_of_ref` varchar(40), origin value SUBMITTED_POLICY. Code: `Account.getBusinessType()`, `AccountResponse`, `AccountSearch` filter, bulk `ACCOUNT_CREATE` optional column; `NewAccount.renewal(...)` and the overload `NewAccount.renewal(..., Integer productVersionNo)` (Renewal); `AccountPricing` rating with `RatingQuery.Purpose.RENEWAL` when the business type is RENEWAL (Renewal); booking `InvoiceBuilder` reads the account's type instead of the hard-coded NEW_BUSINESS (line 129) and `InvoiceBooked.businessType`; the Business Type filter on `NB-BOOKED-REG`, `NB-PRODUCTION`, `NB-PLC-UPDATE`. The existing `NewAccount` factories keep NEW_BUSINESS, so current callers do not change | The first of Submitted Policies S0, Renewal R0 or Employee Benefits E0 to start, as one commit agreed with the account, booking and nbreport owners; the others only check it is merged |
| **P2** | **Report DOCX export** | Employee Benefits (BRID-022.01, BRID-007); every report of any build once it exists (EBQ21 asks whether Word export is required beyond EB) | `ExportFormat.DOCX`, `DocxReportRenderer` (Apache POI XWPF); `DocumentProtector` DOCX protection | EB E0 |
| **P3** | **Attachment access classes** | EB (BRID-025), CSF (CSQ07: agents' lists and downloads), Renewal and Claims (their shared document types), later any module with confidential documents | `att_document_access` (document type, permission), `att_attachment_link.process_tag`; `DocumentService` filters list, download and ZIP; types without a row keep today's behaviour; rows for `RENEWAL_ADVICE` and `CLAIM_REPORT` (XQ04) | EB E0 (V1031), before CSF S1 |
| **P4** | **`ReportCategory` additions** | All seven builds | `RENEWAL` ("Renewal"), `CLAIMS_HANDLING` ("Claims Handling"), `EMPLOYEE_BENEFITS` ("Employee Benefits"), `CUSTOMER_SERVICE` ("Customer Service"), `COMPLIANCE` ("Compliance"), `SUBMITTED_POLICIES` ("Submitted Policies"); UAM uses the existing `CONTROL`. The `ReportMetadata` factories (`claimsHandling`, `compliance`, `submitted`, ...) and Renewal's `ParameterType.CODE_SET` stay with each build | All six values in one commit by the first foundation wave (6.2, step A2); later foundations add only their factory |
| **P5** | **Order of the shared files** (`security/domain/Permission.java` first) | All seven foundation waves, plus the Collections and Accounting build agents | Shared files: `Permission.java`; `navigation/modules.ts`; `features/help/helpContent.ts`; `application.yml` and `docs/operations/CONFIGURATION.md`; `report/core/ReportCategory.java` / `ReportMetadata.java`; `crm/service/ClientService.java` (SANC S0 events, CSF S1 `updateContact`); `attachment/**` (EB E0 access classes, CSF S0 `AllowedFileType`, SANC S0 `DocumentNamingService`); `workflow/**` (SANC S0); `account/**` and `booking/**` (BT0, Renewal R0 fast track and `QueueSource.RENEWAL`, EB E0 billing number); `opsledger` ports (SP S0; CSF S1 with the Operations owner) | Foundation waves run **one at a time**, in the order of 6.2; each is additive and merged before the next starts |

Wave names collide between designs (S0 is used by CSF, Sanction Screening and Submitted Policies). In plans, prefix
them: RN-R0, CL-CL0, EB-E0, CSF-S0, SANC-S0, UAM-U0, SP-S0.

### 6.2 Recommended build order (BRD-6 to BRD-12)

**Phase A: foundations, strictly one after the other (P5).**

| Step | Wave | Why here |
|---|---|---|
| A1 | UAM-U0 + SANC-S0 (one foundation agent, as both designs ask) | No dependency on the other new BRDs; both designs require one agent or back-to-back runs. Adds the P4 `ReportCategory` values for all builds. Gives the `DirectoryAuthenticator` port (D6) |
| A2 | CL-CL0 | Claims depends only on built modules, and Renewal needs its read API (D4) |
| A3 | BT0, then EB-E0 | Under this order EB is the first of the three business-type builds to start, so E0 builds BT0 as its first commit; then P2 (DOCX) and P3 (access classes, with the `RENEWAL_ADVICE` / `CLAIM_REPORT` rows) |
| A4 | CSF-S0 | The CSF design asks for E0 before S0 (shared `attachment` and navigation files) |
| A5 | RN-R0 | Needs BT0 (merged in A3) |
| A6 | SP-S0 | Needs BT0; also needs Collections C1 and Operations O1-A (cashiering) for its ports, which are being built by other agents |

**Phase B: business waves, in parallel once their foundation is merged.**
- UAM U1-A (commits the `ExternalUserProvisioner` port and the EXTERNAL request type first), U1-B.
- SANC S1-A, then S1-B, then S1-C (each starts on the previous stub, as designed).
- CL1-A and CL1-B (CL1-B delivers `ClaimExperienceQueryService`).
- EB E1-B, E1-C; **E1-A after UAM U1-A** has committed the provisioner port (D7).
- CSF S1 (after the Operations owner agrees `paymentsOfClient`).
- RN R1-A to R1-D. The CLAIMS check and the claims part of the Account History tab are complete once CL1-B is
  merged (D4); before that they report "claims not connected".
- SP S1-A to S1-D.

**Phase C: dependent and integration waves.**
- RN R3 right after SP S1-D, so no submitted-policy hand-off waits long as PENDING (D2).
- Integration and hardening: CL2, EB E2, SANC S2, UAM U2, RN R2, SP S2.
- CSF S2 last: its RA and claims-report tabs need Renewal R1-D or EB E1-B (RAs) and Claims CL1-A (documents).

Order of the BRDs in short: **BRD-11 with BRD-10, then BRD-7, BRD-8, BRD-9 (core), BRD-6 and BRD-12, with the
Renewal hand-off (R3) and CSF integration last.**

## 7. Glossary of names used differently (added 26-Sep-2026)

The BDOI drop plan and integration list of 26-Sep-2026 (`docs/source-documents/BDOI_DROP_PLAN.md`) use some names that
already have another meaning in the specs. Read them as follows.

| Name | In the BDOI drop plan and integration list | In the specs and designs | Rule |
|---|---|---|---|
| **CMS** | **Cash Management System** (also "New BOB"): BDO's channel for BDOI's **outward payments** (Drop 0 integration; Disbursement, BRD-5) | **Collection Management System**: the collections BRD and module (BDOI_CLXN_BRD_SPEC.md, `collections`), the Operations feeds, and the Data Migration source value `CMS` (objects C01, F02, F03) | Write "CMS (Cash Management System)" or "CMS / New BOB" for the bank channel, and "Collection Management System" or "Collections" for BRD-4. Register DCR-221 |
| **BOB** | Old BOB = Old Business Online Banking **Collection** System (SOA bills payments by funds transfer, into Cashiering); New BOB = CMS above (outward payments) | BDO Business Online Banking for disbursement funding and the `DISB_BOB_APPROVED` upload (BRD-5) | Name Old BOB or New BOB explicitly |
| **R0** | "R0 early renewal" release of the superseded concept paper | First Renewal build wave (RENEWAL_DESIGN section 14) | R0 means the build wave only; the early release is withdrawn |
| **HLS / LOAS** | One system, HL-LOAS (Home Loan System, Loan Origination and Admin), shown in Drop 0 (HLS) and Drop 2 (LOAS) | HLS insurance report (Submitted Policies), CLPC billing | One integration, two data flows (register DCR-216) |
| **EUA** | Replaced by EIAM (Enterprise Identity Access Management on Microsoft Entra ID) | BRD-11 NFR: EUA with the Windows ID | Target sign-in is EIAM with OpenID Connect (USER_ACCESS_DESIGN section 10.1; DCR-230) |
