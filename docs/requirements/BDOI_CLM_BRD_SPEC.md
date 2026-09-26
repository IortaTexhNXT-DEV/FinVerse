# BDOI Claims (BRD-7, CLM) - Requirements Baseline and Fit/Gap

Client: BDO Insurance and Reinsurance Brokers, Inc. (BDOI), Philippines. Platform: iNXT BrokerVerse (BIBS, BDOI Broker System).

Status: **for BDOI concurrence.** The review workbook is consolidated later with the other BRDs. Build design: [`CLAIMS_BROKING_DESIGN.md`](../architecture/CLAIMS_BROKING_DESIGN.md).

This baseline follows the structure of [`BDOI_OPS_BRD_SPEC.md`](BDOI_OPS_BRD_SPEC.md). Fit classes: **FIT** works today; **CONFIGURE** set-up only; **CHANGE** extends an existing capability; **NEW** new build; **OUT** out of scope per the BRD.

## 1. Source documents

Source: `docs/source-documents/Claims (CLM).PDF` (45 pages, three documents bound in reverse chronological order). Every page was read. The pages without a text layer (p.27 target process, p.45 signed approval sheet) and the tables that did not extract cleanly (p.39-41 capacity and retention tables, p.42-44 report list) were rendered as images and read.

| Pages | Document | Content |
|---|---|---|
| 1-9 | **Workshop addendum** "Claims - Addendum (Workshop)", v1.0 08-Apr-2026, signed 10-16 Apr 2026 (prepared by BPS; input by Claims, Risk Management & Analytics; approved by the Product Owner, the SAVP Unit Head Claims / Risk Mgmt / Technical Underwriting / Reinsurance, and the FVP Head Retail Marketing) | New requirements BRCLM.037-043 from the gap discovery workshop of 19-27 Mar 2026 (p.5-7): multiple locations per claim, claims-prone locations, latest cover version, Marketing access to loss information, insurer-reported claims and updates, insurer location references, several insurer claim numbers per incident. States that it "does not introduce a change in project scope" (p.4) |
| 10-18 | **Renumbering addendum** "Claims - Addendum", v1.0 17-Dec-2025, approved 17-22 Dec 2025 | Replaces FRID-001..036 with BRCLM.001..036 (p.13-16), text unchanged; approval sheets (p.17-18) |
| 19-45 | **Original Claims BRD** "Motor and Non-Motor Claims Logging", template D003, v1 22-Jan-2025 (process diagrams 3-Mar-2025), signed 5-6 Mar 2025 (p.45, scanned) | Purpose and scope (p.21), overview and objectives (p.23), assumptions (p.23), current Motor process (p.24) and Non-Motor process (p.25-26), target process (p.27, image), stakeholder / role matrix (p.28), functional requirements FRID-001..036 with RQID-001..024 (p.29-31), non-functional requirements (p.32-38), capacity and performance (p.39-40), retention (p.41), report list (p.42-44), approval (p.45) |

Requirement IDs: **43** (BRCLM.001-043). The BRCLM.001-036 text is identical to FRID-001-036; the row cites both pages. There are no persona columns for BRCLM.001-036; the persona is taken from the stakeholder matrix (p.28) and the target process lanes (p.27: *System action*, *User action*, *Triggered by a manual request*).

Cross-references read for context (other BRDs, not analysed here):
- `Report List as of APR-27-2026.pdf` p.39-42: the Claims reports (Outstanding, Settled, Outstanding 90 Days Past Due, Claims Aging, Loss Experience, Loss Ratio) with source system **ISYS**, weekly / monthly frequency, XLSX, file name `<Report>_<date of extraction>`, and aging brackets 0-30 / 31-60 / 61-90 / 91-180 / 181+ days.
- `Renewal (RN) BRD.pdf` p.50, 82, 86: the renewal list needs the number of claims and the status of each claim; "Total Loss Claim" is a non-renewal reason; accounts with claims must be flagged.
- `Customer Servicing Facility.PDF` p.10 (BRCSF-009): contact-centre agents retrieve uploaded "claims reports" with the policy documents.

## 2. Business context

BDOI is a **broker**. Insurers accept, evaluate and pay claims; BDOI files and follows the claim **on behalf of the client**. BDOI has no claims liability, keeps no claims reserve, and pays nothing from its own funds. The BRD describes claims *logging and monitoring* (p.21: "Motor and Non-Motor Claims Logging").

Current process (manual filing):
- **Motor** (p.24): the client, account officer or branch sends a notice of accident / loss; Marketing prepares a Preliminary Loss Advice (PLA: assured, policy / reference no., date and location of loss, nature of loss, initial loss reserve if available). The claim handler validates the PLA and sends it to the insurer, then chases the client for documents and forwards them. The insurer evaluates the estimate and either issues a **Letter of Authority (LOA)** to an accredited repair shop (CASA / dealer), after which the handler tags the claim SETTLED / CLOSED and monitors the repair offline, or makes a **cash offer**. The assured signs the offer (or rejects it; the handler notifies the insurer), the handler returns the signed offer, and the insurer issues a cheque **payable to the assured**.
- **Non-Motor** (p.25-26): Marketing completes a Claims Reporting Form (CRF) and gives it to Claims; Claims sends a formal loss advice to the insurer by e-mail (same data plus the assigned adjuster). The insurer assesses; an adjuster may inspect the site and evaluate. Claims chases documents. The insurer / adjuster makes an offer; Claims reviews it and may contest it (the insurer re-evaluates or reiterates its position). The insured accepts and signs, Claims sends the signed offer, the insurer pays, and Claims tags the claim CLOSED.

Target (p.23, p.27): a claims facility (the BRD says "in eBIX"; in BIBS, a BrokerVerse module) with claim authorisation tied to premium payment, cover visibility, date management, configurable claim statuses restricted by role / unit, claimant override, policy number visibility, PHP default, settlement types, adjuster list, follow-up date override, action plan, diary, insurer reserve, ageing and the claims reports. The 2026 workshop adds multi-location and multi-insurer claims, insurer-reported updates, insurer location references, claims-prone location analysis and Marketing access to loss experience.

Organisation: Claims, Risk Management & Analytics, with a Motor Head Office team and a Non-Motor team covering HO, North Luzon and Mindanao regions; users at Head Office (Makati, Ortigas) and the branches Angeles, Cebu, CDO, Davao and GenSan (p.37). Personas (p.28): **Unit Head**, **Team Head**, **Team Lead**, **Claims Officer / Claims Assistant**; the workshop adds **Claims / Risk user** and **Marketing user**.

## 3. Fit/gap summary

| Fit | Meaning | Rows |
|---|---|---|
| FIT | Works today | 2 |
| CONFIGURE | Set-up only | 3 |
| CHANGE | Extend or re-purpose existing capability | 5 |
| NEW | New build | 33 |
| OUT | Out of scope per BRD | 0 |
| **Total** | | **43** |

### Rows per section and fit

| Section | Name | Rows | FIT | CONFIGURE | CHANGE | NEW | Effort S/M/L |
|---|---|---|---|---|---|---|---|
| A | Cover, policy and premium validation | 9 | 2 | 1 | 3 | 3 | 7/2/0 |
| B | Claim status, settlement and closure | 8 | 0 | 0 | 0 | 8 | 6/2/0 |
| C | Claim details, parties and insurer reserve | 6 | 0 | 2 | 0 | 4 | 6/0/0 |
| D | Follow-up, diary and ageing | 7 | 0 | 0 | 0 | 7 | 5/2/0 |
| E | Reports and analytics | 9 | 0 | 0 | 2 | 7 | 5/4/0 |
| F | Multi-location and multi-insurer claims (workshop addendum) | 4 | 0 | 0 | 0 | 4 | 0/4/0 |
| **Total** | | **43** | **2** | **3** | **5** | **33** | **29/14/0** |

The row sizes are small because each row is one field, permission or report. The module as a whole is a medium build (about the size of `adjustment`): see the design, section 14.

### Platform impact

| Area | Treatment | Impact |
|---|---|---|
| Insurer-side `claims` module | **Not extended** | It models an insurer's claim at the company share with reserves, settlements and recoveries posted to the GL and paid through payables. A broker records the insurer's figures as information only. The decision and its reasons are in the design, section 2 |
| Accounts, endorsements, invoices (BRD-1, BRD-2) | Re-use (read) | The cover is the account (ARN) and policy year; its version is the endorsement sequence from `booking`; payment and remittance status come from the `opsledger` invoice ledger |
| Operations (BRD-2) | Re-use a parked port | The new module implements `opsledger.service.port.ClaimsFeed` (feed `CLAIMS_SPECIAL_REMIT`), so the claims condition of a special remittance is confirmed in-app (OQ46) |
| Accounting engine / GL | None | No accounting event: the BRD has no claim money flow through BDOI (CLQ10) |
| LOV, workflow, audit, attachments, messaging, docgen, bulk, reports, alerts, jobs | Re-use | Adjusters and catastrophe codes are LOVs; the claim case runs in the workflow engine for queues and assignment; every change is audited |
| Security | Extend | Claims permissions and four BDOI claims roles plus a risk role; report view / export grants to Marketing |

### Target modules

| Module | Rows |
|---|---|
| `brokerclaims` (new) | 39 |
| `brokerclaims` + `report` (grants, saved variants, extract) | 2 |
| `issuance` / `booking` / `opsledger` (read only) | 2 |

## 4. Claims flow in BIBS (for concurrence)

| Step | Owner | What happens | BRD |
|---|---|---|---|
| Notice of loss | Client / AO / branch, Claims | Claim recorded against a cover (ARN + policy year + version), with reported date, loss date, nature and location(s); claimant defaults to the assured | BRCLM.003/004/006/037/039 |
| Premium check | System | Invoices of the cover read from the invoice ledger; unpaid or partly paid premium disables the claims authorisation code and shows a flag | BRCLM.001 |
| Loss advice to insurer | Claims Officer | Loss advice (PLA / CRF data) sent by e-mail from the claim; insurer claim number(s) captured as they arrive | p.24-25, BRCLM.041/043 |
| Handling | Claims Officer / TL / TH | Status updates restricted by role / unit, adjuster assignment, insurer reserve, next follow-up date, action plan, diary, insurer updates | BRCLM.010-013/017-024/041 |
| Premium remittance hold-up | Claims, Remittance | Status "With BDOI - For Premium Remittance": Claims requests a special remittance (condition CLAIMS); Remittance confirms the claim in-app | BRCLM.010, OQ46, MKTID.009 |
| Settlement | TL / TH | Requested type of settlement (LOA, cash, directly filed, release papers) and settlement amount / date recorded; cheque transmittal tracked by status | BRCLM.010/014/015 |
| Closure | Claims Officer / TL / TH | Temporary closure (non-submission of documents, with offer) or permanent closure (settled or closed type) | BRCLM.005/035 |
| Monitoring | Claims, Risk, Marketing | Ageing overall and per status, pending actions, outstanding / settled / 90-day past due, loss experience, loss ratio, claims-prone locations | BRCLM.025-034/038/040 |

## 5. Requirements and fit/gap

Column **Sec** is the section of section 3. The page is the page of the PDF (addendum page / original BRD page).

### A. Cover, policy and premium validation

| BR ID | Sec | Persona | Requirement | Key acceptance criteria | Fit | Current capability | Proposed solution | Module | Size | Q |
|---|---|---|---|---|---|---|---|---|---|---|
| BRCLM.001 (FRID-001, RQID-001)<br><sub>p.13 / p.29; p.27 #1</sub> | A | System | Identify unpaid invoices / policies; the Claims Authorization Code is disabled for unpaid invoices / policies | Unpaid invoices of the policy identified; authorization code cannot be generated while premium is unpaid (NFR 15.05 "Premium Policy Validation", 55 tpd, 5 s) | **NEW** | Payment status per invoice exists: `opsledger/domain/OpsInvoice.java` (`payment_status` UNPAID / PARTIALLY_PAID / PAID / NOT_APPLICABLE, `remittance_status`, `dp_flag`, `cancelled`), read by `opsledger/service/InvoiceLedgerQueryService.java#forArn`. No claim, no authorization code | Premium check on the claim: the invoices of the cover's ARN and policy year (originals and endorsements, cancelled excluded) from `InvoiceLedgerQueryService.forArn`; result PAID / UNPAID / DP (direct payment); "Generate Authorization Code" (`CAC-<yyyy>`) enabled only when PAID; unpaid invoice list with balance on the claim; re-check on `OpsLedgerEvents.InvoiceMovementPosted`; alert `BCL_UNPAID_PREMIUM_CLAIM` | `brokerclaims` | M | CLQ01 |
| BRCLM.002 (FRID-002, RQID-002)<br><sub>p.13 / p.29; p.27 #3; p.28</sub> | A | Unit Head (grants), Claims Officer / Assistant | Permissions to access the Cover Numbers | Only users with the permission reach the cover data from Claims | **CHANGE** | Accounts are readable with `ACCOUNT_VIEW` (`account/api/AccountController.java`), a Marketing / Processing permission with maintenance screens around it | Read-only **Cover Lookup** in Claims with permission `BCL_COVER_VIEW` (account header, risk items / locations, endorsements, invoices with payment and remittance status, claims of the cover); no account maintenance rights granted to Claims roles | `brokerclaims` | S | CLQ02 |
| BRCLM.003 (FRID-003, RQID-002)<br><sub>p.14 / p.29; p.27 #2, #4</sub> | A | Claims Officer / Assistant | View access to all Cover Numbers to check policy coverages; Cover Number and version are required when recording a claim | All covers visible (no portfolio restriction); a claim cannot be saved without cover number and version | **NEW** | Account (ARN) with policy number per policy year (`account/domain/Account.java`), risk items and sums insured (`account/domain/RiskItem.java`); endorsements per ARN (`booking/service/BookingQueryService.java#endorsements`) | Claim header keeps `arn`, `policy_year`, `policy_no`, `cover_version_no` (count of endorsements of that policy year effective on or before the loss date) and `cover_version_ref` (last endorsement no.), all mandatory; cover lookup lists every account without AO / branch filter | `brokerclaims` | M | CLQ02 |
| BRCLM.004 (FRID-004, RQID-003)<br><sub>p.13 ("1BRCLM.004") / p.29; p.27 #5</sub> | A | Claims Officer / Assistant | Maintain the **Reported Date** only | Reported date captured and kept; drives the claim age | **NEW** | - | Mandatory `reported_date` (not after today, not before the loss date); editable only by `BCL_STATUS_UPDATE` holders while the claim is open, with reason and audit; other operational dates are system time stamps | `brokerclaims` | S | CLQ03 |
| BRCLM.007 (FRID-007, RQID-006)<br><sub>p.13 / p.29; p.27 #9 (user action)</sub> | A | Claims user | Get the Policy Number from the source (EBIX) and upload it to Claims | Policy number available on the claim without re-keying | **FIT** | In BIBS the policy number is recorded on the account by issuance (`issuance/service/EpolicyService.java#confirm` -> `account/service/AccountLifecycleService.java#recordPolicy`, one number per policy year) and carried on every invoice (`booking/service/InvoiceBuilder.java`, `opsledger OpsInvoice.policy_no`) | The claim copies the policy number of its policy year from the account; no upload. Legacy EBIX / ISYS claims and policies need a one-off migration (CLQ14) | `issuance` / `account` (read) | S | CLQ14 |
| BRCLM.008 (FRID-008, RQID-006)<br><sub>p.13 / p.29; p.27 #10</sub> | A | System | Display the Policy Number upon invoicing, as the insurer requires it when a claim is reported | Invoice shows the policy number | **FIT** | `booking/domain/BookedInvoice.java` `policy_no`; `opsledger/domain/OpsInvoice.java` `policy_no`; Invoice 360 (`opsledger/service/Invoice360Service.java`) | No change. Contract note: the client billing statement (Collections SOA, BRCLXN.058) prints the policy number | `booking` / `opsledger` (read) | S | |
| BRCLM.009 (FRID-009, RQID-007)<br><sub>p.13 / p.29; p.27 #11</sub> | A | System | Philippine Peso as the default currency | New claim defaults to PHP | **CONFIGURE** | Accounts default to PHP (`account/domain/Account.java` constructor); currency master exists | Claim currency defaults to the cover currency, else parameter `BCL_DEFAULT_CURRENCY` = PHP | `brokerclaims` | S | |
| BRCLM.016 (FRID-016, RQID-011)<br><sub>p.14 / p.30; p.27 #15</sub> | A | Claims user | View the Marketing Team / Unit of the policy and the Location / BDOI Branch | Marketing unit, AO and branch visible on the claim | **CHANGE** | Sales stamp on the account (`account/domain/SalesStamp.java`: region, department, team, account officer, cost center); invoicing branch on the ledger invoice (`OpsInvoice.branch_id`) | Snapshot of the sales stamp and branch on the claim at recording, refreshed on demand; shown in the summary card and in every report (Marketing Team, Account Officer columns, p.42-43) | `brokerclaims` | S | |
| BRCLM.039<br><sub>p.5-6</sub> | A | Claims user | View the latest policy endorsement or cover version applicable to a claim, so validation uses the most current terms | Claim shows the cover number and version used; indicates when a newer endorsement / version exists; policy data is view-only for Claims users | **CHANGE** | Endorsements per ARN with effective date (`booking/domain/BookingEndorsement.java`, `BookingQueryService.endorsements(arn)`); new endorsements announced by `OpsLedgerEvents.OpsInvoiceBooked`. Risk items are not versioned per endorsement | Claim shows "Cover v<n> (<endorsement no.>)"; flag **Newer version** when the policy year has a later endorsement (computed on read, refreshed on `OpsInvoiceBooked`); cover panel read-only; "use latest version" action with audit. Location-level versioning waits for CLQ02 | `brokerclaims` | S | CLQ02 |

### B. Claim status, settlement and closure

| BR ID | Sec | Persona | Requirement | Key acceptance criteria | Fit | Current capability | Proposed solution | Module | Size | Q |
|---|---|---|---|---|---|---|---|---|---|---|
| BRCLM.005 (FRID-005, RQID-004)<br><sub>p.13 / p.29; p.27 #6-7; p.28</sub> | B | Team Head, Team Lead | Permission to update the status to "Closed" | Only permitted users close a claim | **NEW** | Insurer-side close exists for the insurer model only (`claims/service/ClaimLifecycleService.java`, `CLAIM_AUTHORIZE`) | Permanent closure (settlement type whose `closes_claim` flag is set) requires `BCL_CLOSE`; refused while the settlement amount / date required by the type is missing | `brokerclaims` | S | CLQ06 |
| BRCLM.010 (FRID-010, RQID-008)<br><sub>p.13-14 / p.29; p.27 #13; p.28</sub> | B | Unit Head | Maintain values for Claim Status (18 values listed, section 6.1) | Values maintainable; used on the claim | **NEW** | LOVs exist (`lov/service/LovService.java`) without behaviour attributes (Collections adds them in its own `clx_lov_attribute`, V1000); the insurer claim status is an enum (`claims/domain/ClaimStatus.java`) | LOV `BCL_CLAIM_STATUS` (maker-checker, effective dates) seeded with the 18 BRD values; behaviour in `bcl_lov_attribute` (the Collections `clx_lov_attribute` pattern): phase (NEW / IN_PROGRESS / TEMP_CLOSED), party the claim waits on, default follow-up days, "awaiting premium remittance" flag | `brokerclaims` | M | CLQ04 |
| BRCLM.011 (FRID-011, RQID-008)<br><sub>p.14 / p.29; p.27 #12</sub> | B | Team Head, Team Lead | Permission to update the Claim Status | Only permitted users change the status | **NEW** | - | Endpoint guarded by `BCL_STATUS_UPDATE`; each change writes `bcl_status_history` (from, to, time, user, remark) and resets "age this stage" | `brokerclaims` | S | |
| BRCLM.012 (FRID-012, RQID-009)<br><sub>p.14 / p.29; p.28</sub> | B | Unit Head | Role / unit-based permissions for selecting a Claim Status | Status list filtered by the user's role and unit | **NEW** | Role -> permission model (`security/domain/Role.java`); users have a home branch only (`security/domain/AppUser.java`); no claims unit | Access matrix `bcl_status_access` (status, role, unit or any) maintained by the Unit Head (`BCL_SETUP`); claims handler register `bcl_handler` (user, unit Motor HO / Non-Motor HO / branch, team); status drop-down filtered server-side | `brokerclaims` | M | CLQ04 |
| BRCLM.013 (FRID-013, RQID-009)<br><sub>p.14 / p.29</sub> | B | System | Allow selection of a Claim Status only for specific roles / units | Selection of a status outside the matrix refused | **NEW** | - | Same matrix enforced in the service (`BCL_STATUS_NOT_ALLOWED`, HTTP 422), not only in the UI | `brokerclaims` | S | CLQ04 |
| BRCLM.014 (FRID-014, RQID-010 / RQID-013)<br><sub>p.14 / p.30; p.27 #14</sub> | B | Unit Head | Maintain values for Requested Type of Settlement (10 values, section 6.2) | Values maintainable; used on the claim | **NEW** | - | LOV `BCL_SETTLEMENT_TYPE` seeded with the 10 values; attributes in `bcl_lov_attribute`: outcome (SETTLED / CLOSED_WITHOUT_PAYMENT), `closes_claim`, `requires_settlement_amount` | `brokerclaims` | S | CLQ05 |
| BRCLM.015 (FRID-015, RQID-010)<br><sub>p.14 / p.30</sub> | B | Team Head, Team Lead | Permission to update the Requested Type of Settlement | Only permitted users set or change it | **NEW** | - | `BCL_SETTLEMENT_UPDATE`; change history and audit | `brokerclaims` | S | CLQ05 |
| BRCLM.035 (FRID-035, RQID-024)<br><sub>p.16 / p.31; p.27 #31; p.28</sub> | B | Claims Officer / Assistant | Tag closures as temporary or permanent | Closure kind recorded; temporary closures can be resumed | **NEW** | - | Claim phase TEMP_CLOSED (statuses "Temporary Closed Claim - ...") vs CLOSED (permanent, through a closing settlement type and `BCL_CLOSE`); reopen of a temporary closure by `BCL_STATUS_UPDATE`, of a permanent one by `BCL_REOPEN` with reason | `brokerclaims` | S | CLQ06 |

### C. Claim details, parties and insurer reserve

| BR ID | Sec | Persona | Requirement | Key acceptance criteria | Fit | Current capability | Proposed solution | Module | Size | Q |
|---|---|---|---|---|---|---|---|---|---|---|
| BRCLM.006 (FRID-006, RQID-005)<br><sub>p.13 / p.29; p.27 #8</sub> | C | Team Head, Team Lead | Permission to override or input the Claimant's Name | Claimant defaults to the assured; override only with permission | **NEW** | Assured / client name on the account (`Account.clientName`) | `claimant_name` defaults to the assured; override with `BCL_CLAIMANT_OVERRIDE`, reason and audit; flag `claimant_overridden` | `brokerclaims` | S | CLQ25 |
| BRCLM.017 (FRID-017, RQID-012)<br><sub>p.14-15 / p.30; p.27 #16</sub> | C | Unit Head | Maintain values for Adjuster / Appraiser (25 companies listed, section 6.3) | List maintainable and selectable | **CONFIGURE** | LOV types and values with maker-checker (`lov/domain/LovValue.java`, `LOV_MANAGE` + `MASTER_AUTHORIZE`) | LOV `BCL_ADJUSTER` seeded with the 25 companies; maintained by the Claims Unit Head (owner permission, design section 12) | `brokerclaims` (seed) | S | CLQ11 |
| BRCLM.018 (FRID-018, RQID-012)<br><sub>p.15 / p.30; p.28</sub> | C | Team Head, Team Lead | Permission to update the Adjuster / Appraiser values | Adjuster on the claim set or changed only with permission | **NEW** | - | `adjuster_code` on the claim (and per insurer claim, section F) changed with `BCL_ADJUSTER_ASSIGN`; history | `brokerclaims` | S | CLQ11 |
| BRCLM.023 (FRID-023, RQID-017)<br><sub>p.16 / p.30; p.27 #20</sub> | C | Claims user | Field to amend the Insurer Reserve | Insurer reserve recorded and amendable | **NEW** | The insurer-side reserve (`claims/domain/ReserveChange.java`) posts to the GL and is not usable for a broker | `insurer_reserve` per insurer claim line (information only, no journal); initial loss reserve from the PLA; `bcl_reserve_change` history (previous, new, reason, user, time) | `brokerclaims` | S | CLQ08 |
| BRCLM.024 (FRID-024, RQID-017)<br><sub>p.16 / p.30</sub> | C | Team Head, Team Lead | Permission to amend the Insurer Reserve | Only permitted users amend | **NEW** | - | `BCL_RESERVE_AMEND` | `brokerclaims` | S | CLQ08 |
| BRCLM.036 (FRID-036)<br><sub>p.16 / p.31; p.27 #32</sub> | C | Unit Head | Maintain values for catastrophe codes: Typhoon, Earthquake, Flood, Volcanic Eruption, Landslide, Fire, Others (e.g. Pandemic, El Niño, Terrorism) | Codes maintainable; claim can be tagged | **CONFIGURE** | LOV framework | LOV `BCL_CATASTROPHE` with the 7 values; optional free-text event name on the claim (e.g. the typhoon name) | `brokerclaims` (seed) | S | CLQ12 |

### D. Follow-up, diary and ageing

| BR ID | Sec | Persona | Requirement | Key acceptance criteria | Fit | Current capability | Proposed solution | Module | Size | Q |
|---|---|---|---|---|---|---|---|---|---|---|
| BRCLM.019 (FRID-019, RQID-014)<br><sub>p.15 / p.30; p.27 #17</sub> | D | Team Head, Team Lead | Permission to override the Next Follow-up Date | System date overridable only with permission | **NEW** | - | `next_follow_up_date` computed at each status change (status follow-up days, else `BCL_FOLLOW_UP_DAYS`); override with `BCL_FOLLOW_UP_OVERRIDE` and reason; flag `follow_up_overridden` | `brokerclaims` | S | CLQ07 |
| BRCLM.020 (FRID-020, RQID-015)<br><sub>p.15 / p.30</sub> | D | System | A field for the Next Action Plan Summary | Field on the claim | **NEW** | - | `next_action_plan` (2,000 characters), shown in the worklist and reports ("Follow Ups / Remarks", "Next Action") | `brokerclaims` | S | |
| BRCLM.021 (FRID-021, RQID-015)<br><sub>p.15 / p.30; p.27 #18; p.28</sub> | D | Claims Officer / Assistant | Permission to encode the Next Action Plan Summary | Only permitted users write it | **NEW** | - | `BCL_ACTION_PLAN`; every version kept in the claim history | `brokerclaims` | S | |
| BRCLM.022 (FRID-022, RQID-016)<br><sub>p.15 / p.30; p.27 #19</sub> | D | System (claims handlers) | Diary function for claims handlers | Handlers log and plan activities per claim (NFR 15.08 "Activity Log", 1,206 tpd) | **NEW** | No diary in the platform; Collections efforts are claim-agnostic and not yet built | `bcl_diary_entry` (type call / e-mail / meeting / note / follow-up, date, due date, assignee, text, done); "My Diary" list across claims; due entries notified daily (`BCL_FOLLOW_UP_DUE` job) | `brokerclaims` | M | CLQ07 |
| BRCLM.025 (FRID-025, RQID-018)<br><sub>p.16 / p.30; p.27 #21</sub> | D | System | Calculate the overall age of claims from the date reported | Age overall = days from reported date to today (or to closure) | **NEW** | Ageing helpers exist for money (`subledger/service/AgeingService.java`), not for claims | Computed on read: `age_overall` = (closed_on or as-of date) - reported_date; calendar days | `brokerclaims` | S | CLQ03 |
| BRCLM.027 (FRID-027, RQID-019)<br><sub>p.16 / p.31; p.27 #23</sub> | D | System | Calculate the age of claims per status | Age in the current status; time spent per status | **NEW** | Workflow stage entry time exists for coarse stages (`workflow/domain/WorkCase.java`) | `bcl_status_history` gives days per status; `age_this_stage` = as-of - `status_since` | `brokerclaims` | S | |
| BRCLM.034 (FRID-034, RQID-023)<br><sub>p.16 / p.31; p.27 #30</sub> | D | System | Track and report pending actions for claims | Pending actions visible and reportable | **NEW** | My Work queues (`workflow/service/WorkQueueService.java`) | Pending action = next follow-up date reached or open diary entry due; worklist tab "Follow-ups Due", report `BCL-PENDING-ACTIONS`, alert `BCL_FOLLOW_UP_OVERDUE` | `brokerclaims` | M | CLQ07 |

### E. Reports and analytics

| BR ID | Sec | Persona | Requirement | Key acceptance criteria | Fit | Current capability | Proposed solution | Module | Size | Q |
|---|---|---|---|---|---|---|---|---|---|---|
| BRCLM.026 (FRID-026, RQID-018)<br><sub>p.16 / p.30; p.27 #22; p.43</sub> | E | System | Claims Aging Report with the overall age from the date reported | Report with age overall (fields p.43) | **NEW** | Report framework (`report/core/ReportDefinition.java`, `TabularReportBuilder`); insurer-side PGIBR018 is not a broker report | `BCL-AGEING`: outstanding claims with age overall and age bracket (parameter `BCL_AGEING_BUCKETS`, default 0-30 / 31-60 / 61-90 / 91-180 / 181+ per the Report List) | `brokerclaims` | S | CLQ21 |
| BRCLM.028 (FRID-028, RQID-019)<br><sub>p.16 / p.31; p.27 #24; p.43</sub> | E | System | Claims Aging Report aging per status | Report grouped by status with age this stage | **NEW** | - | `BCL-AGEING-STATUS`: grouped by status, then insurer; age this stage and overall (p.43 field list) | `brokerclaims` | S | |
| BRCLM.029 (FRID-029, RQID-020)<br><sub>p.16 / p.31; p.27 #25; p.42</sub> | E | System | Settled Claims report | List of settled claims (p.42 fields) | **NEW** | - | `BCL-SETTLED`: claims permanently closed with outcome SETTLED in the date range; date settled, settlement amount, age overall | `brokerclaims` | S | CLQ21 |
| BRCLM.030 (FRID-030, RQID-021)<br><sub>p.16 / p.31; p.27 #26; p.43</sub> | E | System | Loss Experience report | Insured, claimant, policy, loss, deductible, amount of loss paid / O/S / total, insurer, status (p.43) | **NEW** | Insurer-side `claims/service/PolicyClaimsService.java` is company-share GL based | `BCL-LOSS-EXPERIENCE` per client / account / policy year: paid = settled amounts, O/S = insurer reserve - paid on open claims; also served by `ClaimExperienceQueryService` to Renewal and Marketing | `brokerclaims` | M | CLQ08, CLQ20 |
| BRCLM.031 (FRID-031, RQID-022)<br><sub>p.16 / p.31; p.27 #27; p.42</sub> | E | System | Outstanding Claims Report | List of all outstanding claims (p.42 fields); 90-days-past-due variant (p.43) | **NEW** | - | `BCL-OUTSTANDING` (every open or temporarily closed claim) and `BCL-OUTSTANDING-PAST-DUE` (age overall above `BCL_PAST_DUE_DAYS`, default 90) | `brokerclaims` | S | |
| BRCLM.032 (FRID-032, RQID-022)<br><sub>p.16 / p.31; p.27 #28; p.44</sub> | E | System | Loss Ratio Report | Loss experience fields plus premiums (p.44) | **NEW** | Premium per invoice in `opsledger` (`OpsInvoice.gross_premium`, endorsements and cancellations as separate invoices) | `BCL-LOSS-RATIO`: losses (paid + O/S) / premium of the same cover and policy year (net of endorsements and cancellations) x 100, grouped per client, product line or insurer | `brokerclaims` | M | CLQ20 |
| BRCLM.033 (FRID-033, RQID-022)<br><sub>p.16 / p.31; p.27 #29; p.28</sub> | E | Unit Head (access), Claims users | Access to the necessary data to generate needed reports or analytics | Users with access extract data and build their own analyses | **CHANGE** | Report Centre with XLSX / CSV / ODS / XML export (`report/**`), saved report variants (`nbreport/service/ReportVariantService.java`, BRNB.057); dynamic builder parked (Q40) | Flat data extract `BCL-DATA-EXTRACT` (one row per claim x insurer x location with every field) behind `BCL_DATA_EXTRACT`; saved variants reused for all BCL reports | `brokerclaims` + `report` | M | CLQ21 |
| BRCLM.038<br><sub>p.5</sub> | E | Claims / Risk user | Identify claims-prone locations or areas so that loss patterns and recurring risks can be analysed | Claims viewable by location; locations with frequent / recurring claims identifiable; builds on existing claims and loss reports | **NEW** | Normalised location key per risk item (`account/domain/RiskIdentifiers.java#locationKey`, `RiskItem.address/city/province`) | `BCL-PRONE-LOCATIONS`: claim count, paid and O/S per location key, city and province over a period, with a "claims-prone" flag at or above `BCL_PRONE_MIN_CLAIMS` in `BCL_PRONE_YEARS`; drill-down to the claims; catastrophe code filter | `brokerclaims` | M | CLQ16 |
| BRCLM.040<br><sub>p.6</sub> | E | Marketing user | View loss information related to accounts and policies for renewal, pricing and client discussions | Marketing views loss experience via existing claims reports; extract / download as per role-based permissions | **CHANGE** | Report view / export split (`report/core/ReportAccess.java`, CSHID.017/018) | Grant `BCL_REPORT_VIEW` to `MKT_AO` / `MKT_TL`; `BCL_REPORT_EXPORT` per the role matrix (CLQ15); Marketing never gets claim maintenance; loss summary on the account page through `ClaimExperienceQueryService` | `brokerclaims` + security grants | S | CLQ15 |

### F. Multi-location and multi-insurer claims (workshop addendum)

| BR ID | Sec | Persona | Requirement | Key acceptance criteria | Fit | Current capability | Proposed solution | Module | Size | Q |
|---|---|---|---|---|---|---|---|---|---|---|
| BRCLM.037<br><sub>p.5</sub> | F | Claims user | Select and associate multiple insured locations with a single claim | More than one location per claim; locations only from the policy / cover location list; each linked location viewable in the claim; still one claim reference | **NEW** | Locations are risk items of kind LOCATION on the account (`account/domain/RiskItem.java`, `Account.getItems()`) | `bcl_claim_location` (claim, item no., address / city / province / location key snapshot, insurer location refs); picker limited to the cover's LOCATION items; claim number unchanged | `brokerclaims` | M | CLQ02 |
| BRCLM.041<br><sub>p.6</sub> | F | Claims user | Capture claims reported or updated by insurers so insurer communications are centrally recorded and auditable | Insurer claim references recorded; each insurer update captures date, source and remarks; updates form part of the claim history with audit trail | **NEW** | Audit trail (`audit/service/AuditTrailService.java`); attachments (`attachment/service/AttachmentService.java`) | Claim source `INSURER_REPORTED` when the insurer notifies first; `bcl_insurer_update` (immutable: date, source LOV `BCL_UPDATE_SOURCE`, reference, remarks, attachments) listed in the claim timeline; bulk upload `BCL_INSURER_UPDATE` for insurer bordereaux | `brokerclaims` | M | CLQ17 |
| BRCLM.042<br><sub>p.6</sub> | F | Claims user | Align internal insured locations with insurer-recognised location references | Insurer-provided location reference per insured location; both references viewable in claims; mappings auditable | **NEW** | Location key per risk item; no insurer reference | `bcl_location_ref` (account, item no., location key, insurer, insurer location reference, effective from / to; a change closes the old row) with audit; maintained from the cover lookup or by upload `BCL_LOCATION_REF`; shown on every claim location | `brokerclaims` | M | CLQ18 |
| BRCLM.043<br><sub>p.7</sub> | F | Claims user | Record and manage several insurer claim numbers under a single claim incident | One incident, one record; each insurer claim number linked to a specific insurer; all visible with the insurer name; searchable and reportable; no duplicate number for the same insurer and claim | **NEW** | Co-insurance shares per invoice (`opsledger/domain/OpsInvoiceShare.java`); insurer panel (`catalog/service/InsurerService.java`) | `bcl_insurer_claim` (claim, insurer, share %, insurer claim no., date reported to insurer, reserve, settled amount, adjuster); unique (claim, insurer, number); indexed search; warning when the same insurer number exists on another claim; `BCL-INSURER-CLAIMS` register | `brokerclaims` | M | CLQ19 |

## 6. Lists and formats from the BRD

### 6.1 Claim status values (BRCLM.010, p.13-14)

| # | Value | Phase proposed | Waiting on |
|---|---|---|---|
| 1 | Newly Filed Claim - with complete documents | NEW | Insurer |
| 2 | Newly Filed Claim - without or incomplete documents | NEW | Claimant |
| 3 | For Adjuster's Review and Evaluation | IN_PROGRESS | Adjuster |
| 4 | For Claimant's Acceptance of Offer | IN_PROGRESS | Claimant |
| 5 | For Claimant's Submission of Documents | IN_PROGRESS | Claimant |
| 6 | For Insurer's Issuance of Check | IN_PROGRESS | Insurer |
| 7 | For Insurer's Returning of Release Papers | IN_PROGRESS | Insurer |
| 8 | For Insurer's Review and Evaluation | IN_PROGRESS | Insurer |
| 9 | With Adjuster - For Issuance of Settlement Offer | IN_PROGRESS | Adjuster |
| 10 | With Assured - For Schedule of Meeting | IN_PROGRESS | Assured |
| 11 | With BDOI - For Premium Remittance | IN_PROGRESS | BDOI (flag "awaiting premium remittance") |
| 12 | With BDOI - For Transmittal of Settlement Check | IN_PROGRESS | BDOI |
| 13 | With BDOI - Under Review / Discussion | IN_PROGRESS | BDOI |
| 14 | With Claimant - For Pull-out of Salvaged Items | IN_PROGRESS | Claimant |
| 15 | With Claimant - For Submission of CNR | IN_PROGRESS | Claimant |
| 16 | With Insurer - For Issuance of LOA | IN_PROGRESS | Insurer |
| 17 | Temporary Closed Claim - Non-submission of Documents | TEMP_CLOSED | Claimant |
| 18 | Temporary Closed Claim - with Offer | TEMP_CLOSED | Claimant |

"CNR" is not defined in the BRD (glossary p.22 is empty); see CLQ12.

### 6.2 Requested type of settlement (BRCLM.014, p.14)

| Value | Outcome proposed | Closes the claim |
|---|---|---|
| Closed - Cancelled | CLOSED_WITHOUT_PAYMENT | Yes |
| Closed - Denied | CLOSED_WITHOUT_PAYMENT | Yes |
| Closed - within Deductible | CLOSED_WITHOUT_PAYMENT | Yes |
| Closed - without Payment | CLOSED_WITHOUT_PAYMENT | Yes |
| Settled | SETTLED | Yes |
| Settled - Directly Filed | SETTLED | Yes |
| Settled - LOA Issued | SETTLED | Yes (repair monitored offline, p.24) |
| Settled - LOA Issued - For Schedule of Repair | SETTLED | To confirm (CLQ05) |
| Settled - LOA Issued - Vehicle Under Repair | SETTLED | To confirm (CLQ05) |
| Settled - Signed Release Papers Returned | SETTLED | Yes |

### 6.3 Adjusters / appraisers (BRCLM.017, p.14-15)

Gemini Adjustment Company; Chartered Adjusters, Inc.; BA International Adjusters & Surveyors Company, Inc.; Total Claims Specialist, Inc.; Unified Adjusters and Surveyors (FAR East), Inc.; Top Brass Insurance Adjusters & Surveyors Co., Inc.; Adjustment Standard Corporation (ASCOR); CARES Adjusters & Surveyors, Inc.; Plaridel Adjusters and Appraisers, Inc.; Crawford & Company Philippines, Inc.; Manila Adjusters & Surveyors Company (MASCO); Tan-Gatue Adjustment Company, Inc.; Technical Inspection Group Adjustment & Surveyors Corp.; Esteban Adjusters and Valuers, Inc.; Interclaim Adjustment Co., Inc.; Pacific International Loss Adjusters Co., Inc.; Senon Insurance Adjusters & Appraisers; Audemus Adjustment Corporation; PALM Property Adjusters; McLarens Philippines; Universal Adjuster Appraisers Co., Inc.; Eagle Prosperity Adjustment and Surveyor Corp.; DFM Adjustment Co., Inc.; Optimum Claims Solutions Insurance Adjustment, Inc.; TreborAsia Insurance Adjustment Services (25).

### 6.4 Catastrophe codes (BRCLM.036, p.16)

Typhoon; Earthquake; Flood; Volcanic Eruption; Landslide; Fire; Others (i.e. Pandemic, El Niño, Terrorism).

### 6.5 Loss notice content (process, p.24-25)

Preliminary Loss Advice (Motor) and formal loss advice e-mail (Non-Motor): Assured; Policy / Reference No.; Date of Accident / Loss; Location of Accident / Loss; Nature of Loss / Loss Description; Initial loss reserve (if available); Assigned Adjuster (Non-Motor, depending on the nature of loss). These are the minimum fields of the claim record.

## 7. Reports

### Report list of the BRD (p.42-44, all "Ad-Hoc", "As needed", Excel)

| Report | Fields | BR | Code proposed |
|---|---|---|---|
| List of all Outstanding Claims | Claim Number, Name of Claimant, Assured's Name, Nature / Type of Loss / Accident, Date of Loss, Date Reported, Claim Amount, Deductible, Claim Status, Insurance Company, Claim Type, Follow Ups / Remarks, Age this Stage, Age Overall, Next Follow Up, Claim Handler, Marketing Team, Account Officer | 031 | `BCL-OUTSTANDING` |
| List of all Settled Claims | Claim Number, Name of Claimant, Assured's Name, Nature / Type of Loss / Accident, Date of Loss, Date Reported, Claim Amount, Deductible, Date Settled, Settlement Amount, Age Overall, Claim Status, Claim Handler, Marketing Team, Account Officer | 029 | `BCL-SETTLED` |
| List of Outstanding Claims 90 Days Past Due | Same as Outstanding | 031 | `BCL-OUTSTANDING-PAST-DUE` |
| Claims Aging Report aging per status | Same as Outstanding | 026 / 028 | `BCL-AGEING`, `BCL-AGEING-STATUS` |
| Loss Experience report | Insured, Claimant, Policy No., Date of Loss, Nature of Loss, Type of Loss, Deductible, Amount of Loss (paid, O/S, Total), Insurer Name, Status | 030 | `BCL-LOSS-EXPERIENCE` |
| Loss Ratio Report | Loss Experience fields + Premiums | 032 | `BCL-LOSS-RATIO` |

### Reports named in requirement rows (no layout in the BRD)

| Report | BR | Code proposed |
|---|---|---|
| Pending actions | 034 | `BCL-PENDING-ACTIONS` |
| Claims by location / claims-prone locations | 038 | `BCL-PRONE-LOCATIONS` |
| Insurer claim numbers | 043 AC5 | `BCL-INSURER-CLAIMS` |
| Data extract for analytics | 033 | `BCL-DATA-EXTRACT` |
| Claims activity log (NFR 15.08; audit of 041 / 042) | 041, 042 | `BCL-ACTIVITY-LOG` |

The Report List of 27-Apr-2026 (p.40-42) sets weekly / monthly frequency, file name `<Report>_<date of extraction>` and the aging brackets used by default.

## 8. Non-functional requirements

| Topic | BRD (p.32-41) | Approach | Fit |
|---|---|---|---|
| Authentication | Windows credentials (single sign-on) for the login page; masked password; user-friendly log-on errors; lockout after 3 invalid attempts; password change every 90 days (LDAP); log all access attempts, visible to the System Administrator only. Remark: "EBIX is already an existing system and NFR will follow the existing requirement setup" | Existing JWT login, lockout, audit of logins; SSO / Active Directory stays parked (BRD-1 Q42) | CHANGE (SSO parked) |
| Passwords | SSO and BDO password standard; hashed; minimum 8 (admin 12); out-of-band reset; user ID / password field checks (10 characters, permitted characters); configurable parameters; complexity; history of 8; minimum age 1 day | Platform password policy (BRD-1 / nbadmin) | FIT |
| Access control | Custom roles; protection against direct object reference manipulation; user administration (create, update, delete, lock / unlock); RBAC | Roles and permissions, `@PreAuthorize` on every endpoint; claims endpoints check the claim's company | FIT |
| Audit logging | Access attempts, privileged use, account changes, admin maintenance; no sensitive data in server logs; exportable logs; application start / stop, failures, configuration changes, audit-log access; customer record access and updates; minimum fields timestamp, user, source IP, resource | `AuditTrailService` on every claim change; platform audit and login logs | FIT |
| Input validation, DB, configuration, encryption | Sanitised input; authenticated DB access with least privilege; no hard-coded credentials; no stack traces; source control and versioning; no OS-level DB functions; HTTPS / SFTP / SSH | Platform standards | FIT |
| UI | Follow the existing BDO Insurance UI; no deprecated client libraries; maintained third-party inventory | `docs/design/BDO_UX_GUIDELINES.md` | FIT |
| Sessions | Idle timeout; **15 minutes** for an active session, set in System Administration; random session IDs; concurrent sessions allowed for internal systems; copy / paste allowed; renewed on login, invalidated on logout; HttpOnly / Secure cookies | Session timeout is a system parameter (BRNB.040, `system/service/SystemParameterService.java`); the 15-minute value differs from BRD-1 (CLQ24) | CONFIGURE |
| APIs and batch | OAuth 2.0 or similar, method-level authorisation, minimal responses, failed / denied calls logged, mTLS for public APIs; batch jobs authenticated, least privilege, restricted execution, traceable runs | No public claims API; jobs are `ManagedJob` with run history | FIT |
| Users | Head Office - Claims 28 (15 concurrent); BDOI branches - Claims 9 (5); Head Office - Marketing 10 (5, settlement status weekly); support, BDOI IT, DCO 10 each (2) | Well inside BRD-1 sizing | FIT |
| Volumes (p.39-40) | Claims booking 55 tpd; premium policy validation 55 tpd; claims status 1,096 tpd; settlement status 1,096 tpd; activity log 1,206 tpd; e-mail notifications to client, AO or insurer 50 per week; reports weekly (aging, outstanding: 5 tpw), monthly (settled: 1 tpm), daily (loss experience, loss ratio: 3 tpd); 10% growth; 2% acceptable error rate | Indexed claim tables; reports as SQL aggregates | FIT |
| Response times | Login, screens, dashboard, claims booking, premium validation: 5 s; status and settlement status updates: 1 minute; activity log and report generation: 5 minutes; report files on a shared drive, 5 MB average, 10 MB maximum | p95 < 3 s online; reports synchronous under 5 minutes; shared drive parked (Report repository) | CONFIGURE |
| Scalability | No downtime when scaling the application; database scaling manual, at most 120 minutes downtime | Container scaling; DBA procedure | FIT |
| Locations | Head Office Makati and Ortigas; branches Angeles, Cebu, CDO, Davao, GenSan | BDO network, web | FIT |
| Availability | Monday-Friday 06:00-20:00; standard maintenance window | Same deployment as BRD-1 (07:00-22:00 Mon-Sat is wider in the evening but starts later) (CLQ24) | CONFIGURE |
| Recovery | RTO 4 hours; RPO 4 hours; DR server required; recovery process documentation required | Platform HA / DR; backups every 4 hours | CONFIGURE |
| Retention (p.41, ref. PPC DOCMNGT-GENPOL-002) | Application, database, infrastructure logs and historical data: 10 years online, 15 years archive, purge after 15 years, backup every 4 hours, backup retention 5 years; audit logs archive **16** years; configuration snapshots weekly; digital documents monthly backup; hard-copy documents 10 years, purge after 10 years | Claims record type in the `nbadmin` retention rules (BRNB.106) with 10 years online; archive and purge stay parked (Q39) (CLQ24) | CONFIGURE |
| Regulatory | None | - | FIT |

## 9. Acronyms and terms used in the BRD

| Term | Meaning in the BRD |
|---|---|
| CASA | Accredited repair shop / dealer for motor repairs (p.24) |
| CNR | Not defined (status "With Claimant - For Submission of CNR"); CLQ12 |
| Cover Number / version | EBIX policy record and its endorsement version; in BIBS the account (ARN) and policy year, and the endorsement sequence (CLQ02) |
| CRF | Claims Reporting Form, completed by Marketing (Non-Motor, p.25) |
| EBIX / ISYS | Legacy core system / legacy claims reporting source (Report List) |
| LOA | Letter of Authority issued by the insurer to the repair shop (p.24) |
| PLA | Preliminary Loss Advice (Motor, p.24) |

## 10. Questions answered by this BRD

| Q# | Source | Question (short) | Answer from this BRD | Design impact |
|---|---|---|---|---|
| **OQ46** | Operations | What does Operations need from the Claims system (e.g. special remittance for claims)? | **Partially.** Claims is a BIBS module, not an external system. The claim is blocked by unpaid premium (the authorization code is disabled, BRCLM.001) and has a status "With BDOI - For Premium Remittance" (BRCLM.010): the claims special remittance is the premium remittance needed before the insurer settles. The BRD defines no data sent to Operations, and no claim money flowing through BDOI: the insurer pays the assured (cheque payable to the assured, LOA to the repair shop, p.24-26); BDOI only transmits the settlement cheque (status "With BDOI - For Transmittal of Settlement Check") | `brokerclaims` implements `opsledger.service.port.ClaimsFeed` for feed `CLAIMS_SPECIAL_REMIT`: one item per invoice of a claim in a status flagged "awaiting premium remittance", key = invoice no. `SpecialRemittanceService` then confirms claims requests in-app (no change to remittance). No `DisbursementGateway` or cashiering use. Claim proceeds through BDOI stay parked (CLQ10) |
| **OQ01** | Operations | Which BRQID.004 systems are external (… Claims)? | **Answered for Claims.** Claims logging is a module of the target system (the BRD enhances "the existing claims module", p.23; the workshop addendum is part of the BIBS gap discovery). Legacy claims reports come from ISYS (Report List p.40-42) | No external Claims interface; the `ClaimsFeed` adapter is in-app (`transport` not applicable). Legacy EBIX / ISYS claims become a migration topic (CLQ14) |
| **OQ25** | Operations | Special remittance approvers, SLA and exact eligibility (claims / renewal / installment / immediate OR) | **Partially, for the claims condition.** Eligible when the invoice belongs to the cover of an open claim whose status is "With BDOI - For Premium Remittance". Approvers and SLA are not given | Eligibility check through the feed (above). Approvers / SLA remain open under OQ25 |
| **Q44** | BRD-1 | Confirm insurer-only functions (reinsurance, technical reserves, IBNR, claims reserving) are not required for BDOI | **Answered for claims.** BDOI records the **insurer's** reserve as information (BRCLM.023/024) and the insurer's settlement; it does no reserving and posts nothing | The insurer-side `claims` module stays hidden for BDOI roles; the broker module posts no journal |
| **Q37** | BRD-1 | Authoritative system per data element | **Partially.** The policy number is sourced from the policy system and flows to Claims, which must not re-key it (BRCLM.007); policy data is view-only for Claims users (BRCLM.039 AC3) | The account (issuance) is the policy-number master; claims keep a snapshot |
| **OQ48**, **PQ17** | Operations / BRD-3 | Access matrices | **Partially, for Claims.** Stakeholder matrix p.28: Unit Head maintains values and permissions; Team Head and Team Lead close, override claimant, update status / settlement type / adjuster, override follow-up date, amend reserve; Claims Officer / Assistant records claims, maintains the reported date, encodes the action plan and tags closures. Marketing gets controlled report access (BRCLM.040) | Roles `CLM_UH`, `CLM_TH`, `CLM_TL`, `CLM_OFFICER`, `CLM_RISK` and Marketing grants (design section 7) |
| **Q39** | BRD-1 | Retention periods per record type | **Partially.** Claims data: 10 years online, 15 years archive, purge after 15 years (p.41) | Retention rule for record type `BrokerClaim` |
| **Q42** | BRD-1 | BDO SSO / Active Directory | **Not resolved; restated.** The Claims NFR asks for Windows credentials (NFR 1.01) | Stays parked |
| **OQ44** | Operations | NFR alignment across BRDs | **Not resolved; a new variant.** Mon-Fri 06:00-20:00, session timeout 15 minutes, RTO / RPO 4 hours, retention 10 + 15 years | Recorded in CLQ24 |

Not answered: OQ02-OQ24, OQ26-OQ43, OQ45, OQ47, OQ49, OQ50; Q01-Q36, Q38, Q40, Q41, Q43; PQ01-PQ16, PQ18-PQ21; the Collections (CQ) and Accounting (AQ) questions.

## 11. Open questions for BDOI (Claims)

| Q# | Topic | Question | Related |
|---|---|---|---|
| CLQ01 | Authorization code | What is the "Claims Authorization Code" (an EBIX function, a code sent to the insurer, an internal approval)? When and by whom is it generated? Does "unpaid" mean any unpaid invoice of the policy year, only the invoice covering the loss date, or also unpaid endorsements? Partly paid and installment accounts? Direct-payment accounts (premium paid to the insurer)? May a TL / TH override the block with a reason? | BRCLM.001, NFR 15.05 |
| CLQ02 | Cover number and version | Confirm that an EBIX "Cover Number" is the BIBS account (ARN) and policy year, and the "version" the endorsement sequence. Must location changes made by endorsement (location added / removed) be versioned so the claim sees the location list as at the loss date? Can a claim be recorded on an expired or cancelled cover, or on an unbooked account? | BRCLM.002/003/037/039 |
| CLQ03 | Reported date | Meaning of "maintain the Reported Date **only**": is it the date the loss was reported to BDOI or to the insurer; may it be corrected, by whom; is it the start of every ageing? | BRCLM.004/025 |
| CLQ04 | Status matrix | The role / unit matrix for each of the 18 statuses; the list of claims units (Motor HO, Non-Motor HO, North Luzon, Mindanao, branches?); default follow-up days per status; allowed status sequences, if any | BRCLM.010-013 |
| CLQ05 | Settlement types and statuses | The values under "Requested Type of Settlement" are outcomes (Closed - Denied, Settled - LOA Issued …). The overview (p.23) also lists "Settlement Types" and "Settlement Status", and p.27 / p.28 ask to "maintain Settlement Type Status values", without an ID or values. Are these one list or two? Which types close the claim ("LOA Issued - Vehicle Under Repair")? | BRCLM.014/015, p.23, 27, 28 |
| CLQ06 | Closure rights | Claims Officers "tag closures as temporary or permanent" (p.28) while closing is a TL / TH permission (BRCLM.005). Proposed: officers set temporary closure; TL / TH close permanently. May a permanently closed claim be reopened, and by whom? | BRCLM.005/035 |
| CLQ07 | Follow-up and diary | How is the Next Follow-up Date computed (per status, fixed days)? Diary features expected: reminders, assignment to another handler, e-mail notification, private entries? | BRCLM.019-022/034 |
| CLQ08 | Insurer reserve | One reserve per claim or per insurer for co-insured claims? Is the O/S of the loss experience "reserve - paid" or "claim amount - paid" when no reserve is given? Do amendments need approval? | BRCLM.023/024/030 |
| CLQ09 | Amounts | Definitions of Claim Amount, Deductible, Settlement Amount and Date Settled; partial or several payments per claim; settlement per insurer | BRCLM.029-032, p.42 |
| CLQ10 | Money through BDOI | Does BDOI ever receive claim proceeds (cheque payable to BDOI, fund transfer) or pay a claimant? Does an insurer deduct unpaid premium from a settlement? What must be recorded for "Transmittal of Settlement Check" (cheque no., bank, amount, received / released dates)? If money passes through BDOI, Cashiering (receipt) and Disbursement (payout) flows are needed | BRCLM.010, OQ46 |
| CLQ11 | Adjusters | Is a plain list enough, or are contacts and e-mail needed (notifications, formal loss advice copy)? Accreditation per insurer? Appraisers (motor) in the same list? | BRCLM.017/018 |
| CLQ12 | Codes | Values for Claim Type and Nature / Type of Loss (report fields); meaning of "CNR"; whether "Others" catastrophe needs a free-text event and whether events are named (e.g. typhoon name) | BRCLM.010/036, p.42 |
| CLQ13 | Claim number | Format of the BDOI claim number (proposal `BCL-<yyyy>-nnnnnn`); keep EBIX / ISYS numbers of migrated claims as legacy references? | p.42 |
| CLQ14 | Migration | Open and historical claims from EBIX / ISYS (history needed for claims-prone and loss-ratio analysis): scope, years, file format ("data migration will be seamless", p.23) | BRCLM.007/038, p.23 |
| CLQ15 | Marketing access | Which Marketing roles see loss information; all accounts or only their own portfolio / unit; which may export | BRCLM.040 |
| CLQ16 | Claims-prone areas | Granularity (exact location, barangay, city / municipality, province, region); period and threshold for "frequent"; mapping or hazard data (flood zones) expected? | BRCLM.038 |
| CLQ17 | Insurer updates | Channels of insurer reports (e-mail, letter, portal, bordereau file); list of "sources"; is a bulk upload of insurer claim updates wanted? | BRCLM.041 |
| CLQ18 | Insurer location references | Where do insurer location references come from (policy schedule, placement, the insurer at claim time)? One per insurer for co-insured risks? Should placement / issuance capture them rather than Claims? | BRCLM.042 |
| CLQ19 | Multi-insurer incidents | Is the case co-insurance on one policy, several policies of one client (e.g. property and business interruption with different insurers), or both? Can one incident span several ARNs? | BRCLM.043 |
| CLQ20 | Loss ratio | Premium basis (gross with taxes, basic premium, net of cancellations; written vs earned), period basis (policy year, loss date, booking date) and grouping (client, account, product line, insurer, Marketing unit) | BRCLM.032 |
| CLQ21 | Reports | Does BIBS replace ISYS for claims reporting (Report List p.40-42)? Confirm aging brackets and the wording "Settled Claims report availability" (BRCLM.029); scheduled delivery to the shared drive? | BRCLM.026-033 |
| CLQ22 | Notifications | NFR 15.14 counts e-mail notifications to the client, AO or insurer (50 per week). Which events notify whom, and the templates (PLA, formal loss advice, offer to the assured)? | NFR 15.14, p.24-26 |
| CLQ23 | Notice of loss intake | In the current process Marketing / branches prepare the PLA / CRF. Should the AO record the notice in BIBS (with Claims validating), or only Claims users? | p.24-25 |
| CLQ24 | NFR alignment | Mon-Fri 06:00-20:00 vs BRD-1 07:00-22:00 Mon-Sat; session timeout 15 minutes; audit-log archive 16 years vs purge after 15 years; RPO 4 hours (Operations 24 hours) | NFR, OQ44 |
| CLQ25 | Claimants | Third-party claimants (motor TP) and claimant contact details: is a name override enough, or a claimant record with contacts and payee details? | BRCLM.006 |
| CLQ26 | Documents | Is a document checklist per claim type needed to decide "with complete documents" vs "without or incomplete documents"? | BRCLM.010 statuses 1-2 |
| CLQ27 | Branch scope | Do branch claims users see only their branch's claims, or all claims? | NFR p.37, BRCLM.003 |
| CLQ28 | Total loss | The Renewal BRD uses "Total Loss Claim" as a non-renewal reason (RN p.82) and needs the number and status of claims per account (RN p.50, 86). Should Claims record a total-loss indicator (a settlement type, a loss nature or a flag)? | Renewal BRD, BRCLM.014 |

**Answered in part by later BRDs** (single record with the design impact: [`BDOI_CROSS_BRD_DECISIONS.md`](BDOI_CROSS_BRD_DECISIONS.md) section 3):
- **CLQ13 partial** (BRD-13 Data Migration, BRID 11.1): claims history stays in legacy or the archive; open claims are
  not mentioned, so `legacy_ref` is needed only if open claims are migrated (DMQ30).
- **CLQ14 partial** (BRD-13 p.3, BRID 11.1): historical claims are not migrated into BIBS (read-only legacy or archive,
  record type CLAIM); open claims are not addressed (DMQ30). V1025 stays held.
- **CLQ10** is asked again by the Core Replacement umbrella BRD (BR-146 to BR-148, unclaimed cheque safekeeping and
  hand-over to / retrieval from Cashiering): **CRQ03**, cross-BRD conflict **XQ13**.

## 12. Observations on the BRD pack

- The PDF binds three documents newest first: the workshop addendum (Apr-2026), the renumbering addendum (Dec-2025) and the original BRD (Jan / Mar-2025). The renumbering addendum only replaces FRID-001..036 with BRCLM.001..036; its page header "2 of 9 … 7 of 9" repeats the addendum template.
- p.13 prints the ID "1BRCLM.004" (a footnote marker merged with the ID). It is read as BRCLM.004.
- BRCLM.027, 029 and 030 have no process component ("Claims" is blank) in both versions.
- BRCLM.014 maps to two original RQIDs (RQID-010, RQID-013). The "Requested Type of Settlement" values are claim outcomes, not requested types. The overview (p.23) lists "Settlement Types" and "Settlement Status" separately, and the target process (p.27) and stakeholder matrix (p.28) ask to "maintain Settlement Type Status values" with no BR ID and no values (CLQ05).
- The overview (p.23) says the facility is to be built "in eBIX", and assumption 1 says "the existing claims module is functional and can be enhanced". In BIBS the claims module is a new build; the insurer-side claims module of the platform does not fit a broker (design section 2).
- The glossary and references (p.22) are empty; CNR, CASA, LOA, PLA and CRF are not defined.
- The stakeholder matrix gives both Claims Officers ("tag closures as temporary or permanent") and TL / TH ("update claim status to Closed") closure rights (CLQ06).
- The NFR numbering is inconsistent: section titles "15a" and "23b", IDs 15.01-15.17 and 25.18-25.25. The audit-log archive of 16 years exceeds the purge period of 15 years (p.41). The retention table cites the reference PPC DOCMNGT-GENPOL-002.
- The capacity table (p.39-40) is headed "Values on the table are sample requirements you can use", yet it carries project-specific users and volumes; it is taken as the requirement.
- The workshop addendum says it "does not introduce a change in project scope" (p.4), yet it adds seven must-have requirements (multi-location, multi-insurer, insurer updates, location mapping, claims-prone analysis). They are baselined here as in scope.
- The workshop addendum page footers read "x of 9" while the approval pages (p.8-9) are unnumbered; one reviewer name is printed "Dan Ace Cəuton" (p.9).
- The original approval sheet (p.45, scanned) is signed 5-6 Mar 2025 by the preparers, the Claims reviewers, the SAVP Unit Head and the Process Owner (then AVP / Unit Head Northern Luzon); the BRQ and BRD numbers are blank.
- The Report List of 27-Apr-2026 (a separate document) repeats the Claims reports with ISYS as the source and adds "typical fields" (adjuster appointed, loss reserve, paid to date, last action date) that the BRD does not list. They fit the proposed extract but are not baselined here.
